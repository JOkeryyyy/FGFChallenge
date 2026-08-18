package com.example.fgfchallenge.feature.logs.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.error.guardLogsDataFailures
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import com.example.fgfchallenge.feature.logs.data.local.LogQuerySql
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.mapper.toLogEntry
import com.example.fgfchallenge.feature.logs.data.mapper.toLogFilterOptions
import com.example.fgfchallenge.feature.logs.data.mapper.toLogSummary
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The [LogsRepository] that coordinates the two sources: a [SnapshotRefresher] that writes one
 * complete snapshot at launch, and the feature-owned Room database that answers every read
 * afterwards.
 *
 * The name describes that strategy rather than either source, because neither one alone is the
 * source of truth: the refresher only ever writes, and Room only ever reads.
 *
 * Room is used directly instead of through a `LogsLocalDataSource` wrapper. `LogsDao` is already
 * the narrow, swappable interface such a wrapper would introduce, so a pass-through layer would add
 * indirection without adding a boundary (`documentation/conventions/data-layer.md` §6). The write
 * side does have a real second implementation — the benchmark variant's fixed fixture — which is
 * why [SnapshotRefresher] exists and `LogsDao` still does not have an equivalent.
 *
 * This class is where infrastructure failures stop for reads, using the same
 * `guardLogsDataFailures` boundary the refreshers use: everything SQLite can throw becomes
 * [LogsDataError], except coroutine cancellation, which is always rethrown untouched so structured
 * concurrency keeps working.
 */
internal class SnapshotLogsRepository
    @Inject
    constructor(
        private val snapshotRefresher: SnapshotRefresher,
        private val logsDao: LogsDao,
    ) : LogsRepository {
        /**
         * One delegation, by design: which snapshot the launch refresh installs is the variant's
         * decision, and everything below this line reads whatever it left in Room.
         */
        override suspend fun refreshSnapshot(): EmptyResult<LogsDataError> = snapshotRefresher.refresh()

        override fun pagedLogs(query: LogQuery): Flow<PagingData<LogEntry>> =
            Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        // Matching the page size keeps the first screenful exactly the newest 100
                        // matches; Paging would otherwise load three pages up front.
                        initialLoadSize = PAGE_SIZE,
                        prefetchDistance = PREFETCH_DISTANCE,
                        // Keep five pages while scrolling. This is well above Paging's required
                        // pageSize + (2 * prefetchDistance) minimum, so old pages can be dropped
                        // without immediately being reloaded.
                        maxSize = MAX_CACHED_ROWS,
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
            guardLogsDataFailures {
                Result.Success(logsDao.logById(id)?.toLogEntry())
            }

        private companion object {
            /** `ARCHITECTURE.md` fixes these: 100 rows per page, next page fetched 25 rows out. */
            const val PAGE_SIZE = 100
            const val PREFETCH_DISTANCE = 30
            const val MAX_CACHED_ROWS = PagingConfig.MAX_SIZE_UNBOUNDED
        }
    }
