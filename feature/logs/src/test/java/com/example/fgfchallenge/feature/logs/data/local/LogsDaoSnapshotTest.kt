package com.example.fgfchallenge.feature.logs.data.local

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture.SNAPSHOT
import com.example.fgfchallenge.feature.logs.data.mapper.toLogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the reads and writes that are not driven by a `LogQuery`: replacing the stored snapshot,
 * the unfiltered filter options, the stable-ID details lookup, and the invalidation that makes
 * observers see a replacement at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LogsDaoSnapshotTest {
    private var database: LogsDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun `replacing a snapshot leaves only the new rows`() =
        withDatabase { dao ->
            dao.replaceSnapshot(SNAPSHOT)
            val replacement =
                listOf(
                    LogsTestFixture.entity(
                        id = "log-99",
                        timestamp = "2026-08-17T09:00:00Z",
                        severity = "INFO",
                        tag = "sync",
                        message = "Second launch",
                        latencyMs = 10,
                        isAiGenerated = false,
                        sessionId = "session-667",
                    ),
                )

            dao.replaceSnapshot(replacement)

            // A second launch replaces rather than accumulates, so IDs from the first are gone.
            assertThat(dao.count()).isEqualTo(1)
            assertThat(dao.matchingIds(LogQuery())).isEqualTo(listOf("log-99"))
        }

    @Test
    fun `a failed replacement rolls back to the previous snapshot`() =
        withDatabase { dao ->
            dao.replaceSnapshot(SNAPSHOT)
            // Two rows with the same primary key: the insert fails part-way, after the delete.
            val duplicated = listOf(LogsTestFixture.ERROR_DB_TIMEOUT, LogsTestFixture.ERROR_DB_TIMEOUT)

            runCatching { dao.replaceSnapshot(duplicated) }

            // Without the transaction the table would now be empty or half written.
            assertThat(dao.count()).isEqualTo(SNAPSHOT.size)
            assertThat(dao.matchingIds(LogQuery()))
                .isEqualTo(listOf("log-6", "log-5", "log-4", "log-3", "log-2", "log-1"))
        }

    @Test
    fun `filter options expose the distinct tags and the stored latency extent`() =
        withDatabase { dao ->
            dao.replaceSnapshot(SNAPSHOT)

            val options = dao.latencyBounds().first().toLogFilterOptions(dao.availableTags().first())

            assertThat(options.availableTags).isEqualTo(listOf("cache", "db", "network", "ui"))
            assertThat(options.minimumLatencyMs).isEqualTo(12)
            assertThat(options.maximumLatencyMs).isEqualTo(5000)
        }

    @Test
    fun `filter options are empty rather than zeroed while no snapshot is stored`() =
        withDatabase { dao ->
            val options = dao.latencyBounds().first().toLogFilterOptions(dao.availableTags().first())

            assertThat(options.availableTags).isEmpty()
            // Null, not 0: an empty table has no latency extent, and 0 would seed a real slider.
            assertThat(options.minimumLatencyMs).isNull()
            assertThat(options.maximumLatencyMs).isNull()
        }

    @Test
    fun `details resolve by stable id`() =
        withDatabase { dao ->
            dao.replaceSnapshot(SNAPSHOT)

            val found = dao.logById("log-3")

            assertThat(found).isNotNull().isEqualTo(LogsTestFixture.INFO_UI_FRAMES)
            assertThat(dao.logById("log-absent")).isNull()
        }

    @Test
    fun `replacing the snapshot re-emits the observed summary`() =
        withDatabase { dao ->
            dao.replaceSnapshot(SNAPSHOT)

            dao.severityCounts(LogQuerySql.severityCountSelect(LogQuery())).test {
                assertThat(awaitItem().sumOf { it.count }).isEqualTo(SNAPSHOT.size)

                dao.replaceSnapshot(SNAPSHOT.take(2))

                // The raw query declares its observed entity, so Room invalidates it on write.
                assertThat(awaitItem().sumOf { it.count }).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun withDatabase(block: suspend (LogsDao) -> Unit) =
        runTest {
            val database =
                createInMemoryLogsDatabase(StandardTestDispatcher(testScheduler))
                    .also { this@LogsDaoSnapshotTest.database = it }

            block(database.logsDao())
        }
}
