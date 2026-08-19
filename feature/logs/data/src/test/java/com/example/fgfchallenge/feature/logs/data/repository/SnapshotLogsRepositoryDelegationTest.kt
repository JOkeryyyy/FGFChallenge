package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.local.createInMemoryLogsDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies that the repository's write path is supplied by the selected variant strategy.
 *
 * The read path is unaffected and stays covered by the query and failure tests; what matters here
 * is only that `refreshSnapshot` is one delegation to the injected [SnapshotRefresher] and returns
 * that strategy's result untouched. That is what lets the benchmark variant install a fixed Room
 * fixture without any other part of the data layer differing between build types.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SnapshotLogsRepositoryDelegationTest {
    @Test
    fun `refreshSnapshot delegates once and returns the strategy result`() =
        runTest {
            val database = createInMemoryLogsDatabase(StandardTestDispatcher(testScheduler))
            val refresher = RecordingSnapshotRefresher(Result.Success(Unit))
            try {
                val repository = SnapshotLogsRepository(refresher, database.logsDao())

                assertThat(repository.refreshSnapshot()).isEqualTo(Result.Success(Unit))
                assertThat(refresher.calls).isEqualTo(1)
            } finally {
                database.close()
            }
        }

    @Test
    fun `refreshSnapshot returns the strategy failure untouched`() =
        runTest {
            val database = createInMemoryLogsDatabase(StandardTestDispatcher(testScheduler))
            val refresher = RecordingSnapshotRefresher(Result.Error(LogsDataError))
            try {
                val repository = SnapshotLogsRepository(refresher, database.logsDao())

                assertThat(repository.refreshSnapshot()).isEqualTo(Result.Error(LogsDataError))
                assertThat(refresher.calls).isEqualTo(1)
            } finally {
                database.close()
            }
        }
}

/** A [SnapshotRefresher] that returns a fixed result and counts how often it was asked. */
private class RecordingSnapshotRefresher(
    private val result: EmptyResult<LogsDataError>,
) : SnapshotRefresher {
    var calls: Int = 0
        private set

    override suspend fun refresh(): EmptyResult<LogsDataError> {
        calls++
        return result
    }
}
