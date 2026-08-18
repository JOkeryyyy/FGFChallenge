package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.di.DefaultDispatcher
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.error.guardLogsDataFailures
import com.example.fgfchallenge.feature.logs.data.fixture.BenchmarkLogsFixture
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Ensures one fixed benchmark snapshot while preserving it across process restarts.
 *
 * The benchmark variant never reaches the network: measurements must not depend on a live endpoint,
 * and the scenario counts have to be identical on every run. Instead this strategy installs
 * [BenchmarkLogsFixture] once and then recognizes it, so the nine relaunches after the first cost a
 * count and two ID lookups rather than a 100,000-row replacement.
 *
 * That idempotence is what makes `CompilationMode.Ignore()` viable on Android 10: the suite never
 * reinstalls the app or clears its data, so the seeded database is still there each iteration.
 */
internal class BenchmarkSnapshotRefresher
    @Inject
    constructor(
        private val logsDao: LogsDao,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : SnapshotRefresher {
        /** How often the fixture was actually written. Asserted by the benchmark-variant unit test. */
        internal var installCount: Int = 0
            private set

        override suspend fun refresh(): EmptyResult<LogsDataError> =
            guardLogsDataFailures {
                if (!fixtureIsInstalled()) {
                    val rows = withContext(defaultDispatcher) { BenchmarkLogsFixture.create() }
                    logsDao.replaceSnapshot(rows)
                    installCount++
                }
                Result.Success(Unit)
            }

        /**
         * Row count plus both sentinel IDs, rather than a count alone: a partially written or
         * previously different snapshot could still hold exactly [BenchmarkLogsFixture.SIZE] rows,
         * and measuring against it would silently change every scenario's result count.
         */
        private suspend fun fixtureIsInstalled(): Boolean =
            logsDao.count() == BenchmarkLogsFixture.SIZE &&
                logsDao.logById(BenchmarkLogsFixture.FIRST_ID) != null &&
                logsDao.logById(BenchmarkLogsFixture.LAST_ID) != null
    }
