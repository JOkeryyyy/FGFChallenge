package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.fixture.BenchmarkLogsFixture
import com.example.fgfchallenge.feature.logs.data.local.createInMemoryLogsDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the property the Macrobenchmark suite depends on most: the benchmark launch refresh installs
 * the fixture once and then leaves it alone.
 *
 * Every iteration kills the process and relaunches, which runs the launch refresh again. If that
 * second refresh rewrote 100,000 rows, each iteration would measure a snapshot replacement instead
 * of the interaction under test — and `CompilationMode.Ignore()` exists precisely so the seeded
 * database survives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BenchmarkSnapshotRefresherTest {
    @Test
    fun `first refresh installs the fixture and the second is a no-op`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val database = createInMemoryLogsDatabase(dispatcher)
            try {
                val dao = database.logsDao()
                val refresher = BenchmarkSnapshotRefresher(dao, dispatcher)

                assertThat(refresher.refresh()).isEqualTo(Result.Success(Unit))
                assertThat(dao.count()).isEqualTo(BenchmarkLogsFixture.SIZE)

                assertThat(refresher.refresh()).isEqualTo(Result.Success(Unit))
                assertThat(refresher.installCount).isEqualTo(1)
                assertThat(dao.count()).isEqualTo(BenchmarkLogsFixture.SIZE)
                assertThat(dao.logById(BenchmarkLogsFixture.FIRST_ID)).isNotNull()
                assertThat(dao.logById(BenchmarkLogsFixture.LAST_ID)).isNotNull()
            } finally {
                database.close()
            }
        }

    @Test
    fun `an incomplete snapshot is reinstalled`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val database = createInMemoryLogsDatabase(dispatcher)
            try {
                val dao = database.logsDao()
                val refresher = BenchmarkSnapshotRefresher(dao, dispatcher)
                // A partial table: the right shape, the wrong contents. Only the row count and the
                // two sentinels distinguish it, which is what the guard checks.
                dao.replaceSnapshot(BenchmarkLogsFixture.create().take(10))

                assertThat(refresher.refresh()).isEqualTo(Result.Success(Unit))

                assertThat(refresher.installCount).isEqualTo(1)
                assertThat(dao.count()).isEqualTo(BenchmarkLogsFixture.SIZE)
                assertThat(dao.logById(BenchmarkLogsFixture.LAST_ID)).isNotNull()
            } finally {
                database.close()
            }
        }
}
