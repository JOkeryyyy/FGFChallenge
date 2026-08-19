package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.di.DefaultDispatcher
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.error.guardLogsDataFailures
import com.example.fgfchallenge.feature.logs.data.error.map
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.mapper.toEntity
import com.example.fgfchallenge.feature.logs.data.mapper.toLogBatch
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The shipping launch refresh: one complete remote snapshot fetched, decoded, mapped, and then
 * atomically replacing the stored one.
 *
 * Debug and release both bind this strategy, so it is the production write path in full — the
 * extraction from `SnapshotLogsRepository` moved this code without changing what it does.
 */
internal class RemoteSnapshotRefresher
    @Inject
    constructor(
        private val logsApi: LogsApi,
        private val logsDao: LogsDao,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : SnapshotRefresher {
        /**
         * Fetch, decode, map, then replace — in that order, and never interleaved.
         *
         * The whole payload becomes storable rows before the transaction opens, so an invalid entry
         * is discovered while the previous snapshot is still intact. The replacement itself is one
         * transaction inside the DAO, which is what makes a mid-import failure roll back to that
         * previous snapshot rather than leave the table empty or half-written.
         */
        override suspend fun refresh(): EmptyResult<LogsDataError> =
            guardLogsDataFailures {
                val response = logsApi.getLogs()
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    // A non-2xx status, or a 2xx with no body: well-formed HTTP, unusable payload.
                    return@guardLogsDataFailures Result.Error(LogsDataError)
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
    }
