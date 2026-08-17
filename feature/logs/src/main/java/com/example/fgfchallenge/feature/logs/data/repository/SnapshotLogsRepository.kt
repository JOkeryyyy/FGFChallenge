package com.example.fgfchallenge.feature.logs.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.fgfchallenge.feature.logs.data.di.DefaultDispatcher
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.error.map
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import com.example.fgfchallenge.feature.logs.data.local.LogQuerySql
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.mapper.toEntity
import com.example.fgfchallenge.feature.logs.data.mapper.toLogBatch
import com.example.fgfchallenge.feature.logs.data.mapper.toLogEntry
import com.example.fgfchallenge.feature.logs.data.mapper.toLogFilterOptions
import com.example.fgfchallenge.feature.logs.data.mapper.toLogSummary
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * The [LogsRepository] that coordinates the two sources: the one-shot remote endpoint that supplies
 * a complete snapshot, and the feature-owned Room database that answers every read afterwards.
 *
 * The name describes that strategy rather than either source, because neither one alone is the
 * source of truth: the network only ever writes, and Room only ever reads.
 *
 * Both sources are used directly instead of through `LogsRemoteDataSource`/`LogsLocalDataSource`
 * wrappers. `LogsApi` and `LogsDao` are already the narrow, swappable interfaces such wrappers
 * would introduce, so a pass-through layer would add indirection without adding a boundary
 * (`documentation/conventions/data-layer.md` §6).
 *
 * This class is where infrastructure failures stop. Everything the endpoint or SQLite can throw
 * becomes [LogsDataError], except coroutine cancellation, which is always rethrown untouched so
 * structured concurrency keeps working.
 */
internal class SnapshotLogsRepository
    @Inject
    constructor(
        private val logsApi: LogsApi,
        private val logsDao: LogsDao,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : LogsRepository {
        /**
         * Fetch, decode, map, then replace — in that order, and never interleaved.
         *
         * The whole payload becomes storable rows before the transaction opens, so an invalid entry
         * is discovered while the previous snapshot is still intact. The replacement itself is one
         * transaction inside the DAO, which is what makes a mid-import failure roll back to that
         * previous snapshot rather than leave the table empty or half-written.
         */
        override suspend fun refreshSnapshot(): EmptyResult<LogsDataError> =
            guardDataFailures {
                val response = logsApi.getLogs()
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    // A non-2xx status, or a 2xx with no body: well-formed HTTP, unusable payload.
                    return@guardDataFailures Result.Error(LogsDataError)
                }

                // Validating and mapping the snapshot is CPU work, so it moves off the caller's
                // thread. The Retrofit call already suspends, and the DAO write is main-safe.
                val entities =
                    withContext(defaultDispatcher) {
                        body.toLogBatch().map { batch -> batch.entries.map(LogEntry::toEntity) }
                    }

                when (entities) {
                    is Result.Error -> {
                        entities
                    }

                    is Result.Success -> {
                        logsDao.replaceSnapshot(entities.data)
                        Result.Success(Unit)
                    }
                }
            }

        override fun pagedLogs(query: LogQuery): Flow<PagingData<LogEntry>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        // Matching the page size keeps the first screenful exactly the newest 100
                        // matches; Paging would otherwise load three pages up front.
                        initialLoadSize = PAGE_SIZE,
                        prefetchDistance = PREFETCH_DISTANCE,
                        // Placeholders would require a full COUNT per generation and would put
                        // unloaded rows in the list; the summary reports the real total instead.
                        enablePlaceholders = false,
                    ),
                // A factory, not a value: Paging calls it again after Room invalidates the previous
                // source, which is how a snapshot replacement reaches an already-collected stream.
                pagingSourceFactory = { logsDao.pagedLogs(LogQuerySql.pagedSelect(query)) },
            ).flow
                .map { pagingData -> pagingData.map(LogEntity::toLogEntry) }

        override fun summary(query: LogQuery): Flow<LogSummary> =
            logsDao
                .severityCounts(LogQuerySql.severityCountSelect(query))
                .map { counts -> counts.toLogSummary() }

        /**
         * The two option queries are separate selects — a `DISTINCT` scan and a `MIN`/`MAX`
         * aggregate — combined here so the UI observes one value. Neither reads whole rows.
         */
        override fun filterOptions(): Flow<LogFilterOptions> =
            combine(
                logsDao.availableTags(),
                logsDao.latencyBounds(),
            ) { tags, bounds -> bounds.toLogFilterOptions(tags) }

        override suspend fun logById(id: String): Result<LogEntry?, LogsDataError> =
            guardDataFailures {
                Result.Success(logsDao.logById(id)?.toLogEntry())
            }

        private companion object {
            /** `ARCHITECTURE.md` fixes these: 100 rows per page, next page fetched 25 rows out. */
            const val PAGE_SIZE = 100
            const val PREFETCH_DISTANCE = 25
        }
    }

/**
 * Runs a data-access [block] and collapses every expected infrastructure failure into
 * [LogsDataError], so no exception type from either source escapes the repository.
 *
 * It is `inline`, so [block] may suspend even though this function does not.
 */
private inline fun <T> guardDataFailures(block: () -> Result<T, LogsDataError>): Result<T, LogsDataError> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        // Must come first and stay `kotlinx.coroutines.CancellationException`: swallowing it here
        // would make a cancelled refresh look handled instead of cancelled. It is also a
        // `RuntimeException`, so the catch below would otherwise absorb it.
        throw cancellation
    } catch (_: IOException) {
        // Connectivity loss and timeouts: `UnknownHostException`, `ConnectException`,
        // `NoRouteToHostException`, `SocketTimeoutException`.
        Result.Error(LogsDataError)
    } catch (_: RuntimeException) {
        // Decoding failures (`SerializationException`), SQLite failures (`SQLiteException` is a
        // `RuntimeException`), and anything else unexpected.
        Result.Error(LogsDataError)
    }
