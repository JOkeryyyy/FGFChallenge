package com.example.fgfchallenge.feature.logs.data.repository

import androidx.paging.testing.asSnapshot
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture.SNAPSHOT
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import com.example.fgfchallenge.feature.logs.data.remote.LogsPayloadDto
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

/**
 * The read half of the repository: what the paged stream, the aggregate summary, the filter
 * options, and the details lookup return for a given query.
 *
 * The predicates themselves are covered by the DAO tests. What is verified here is that the
 * repository hands the same query to both query-driven paths — a list and a summary that describe
 * the same rows — and that nothing above the boundary receives an entity.
 */
@RunWith(RobolectricTestRunner::class)
class SnapshotLogsRepositoryQueryTest {
    @Test
    fun `paged logs return application entries newest first by default`() =
        withStoredSnapshot { repository, _ ->
            val rows = repository.pagedLogs(LogQuery()).asSnapshot()

            assertThat(rows.map { it.id }).isEqualTo(listOf("log-6", "log-5", "log-4", "log-3", "log-2", "log-1"))
            // Mapped out of the database representation: a UTC instant, not epoch millis.
            assertThat(rows.first().timestamp.toString()).isEqualTo("2026-08-16T17:13:00Z")
        }

    @Test
    fun `paged logs honour the requested direction`() =
        withStoredSnapshot { repository, _ ->
            val query = LogQuery(sortDirection = LogSortDirection.OldestFirst)

            val rows = repository.pagedLogs(query).asSnapshot()

            assertThat(rows.map { it.id }).isEqualTo(listOf("log-1", "log-2", "log-3", "log-4", "log-5", "log-6"))
        }

    @Test
    fun `paged logs evict offscreen pages after a long scroll`() =
        runLogsRepositoryTest(UnusedLogsApi) { repository, dao ->
            dao.replaceSnapshot(largeSnapshot())

            val presentedRows =
                repository.pagedLogs(LogQuery()).asSnapshot {
                    appendScrollWhile { entry -> entry.id != OLDEST_LARGE_SNAPSHOT_ID }
                }

            assertThat(presentedRows).hasSize(MAX_PRESENTED_ROWS)
        }

    @Test
    fun `the summary counts the same rows the list shows`() =
        withStoredSnapshot { repository, _ ->
            val query = LogQuery(literalSearch = "timeout")

            val rows = repository.pagedLogs(query).asSnapshot()
            val summary = repository.summary(query).first()

            assertThat(summary.totalCount).isEqualTo(rows.size)
            assertThat(summary.countBySeverity[Severity.ERROR]).isEqualTo(2)
            assertThat(summary.countBySeverity[Severity.FATAL]).isEqualTo(1)
            // Absent severities are still present at zero, so the legend keeps its shape.
            assertThat(summary.countBySeverity[Severity.DEBUG]).isEqualTo(0)
        }

    @Test
    fun `an empty result is an empty page and a zeroed summary`() =
        withStoredSnapshot { repository, _ ->
            val query = LogQuery(literalSearch = "no such message")

            assertThat(repository.pagedLogs(query).asSnapshot()).isEqualTo(emptyList())
            assertThat(repository.summary(query).first().totalCount).isEqualTo(0)
        }

    @Test
    fun `filter options describe the unfiltered snapshot`() =
        withStoredSnapshot { repository, _ ->
            val options = repository.filterOptions().first()

            assertThat(options.availableTags).isEqualTo(listOf("cache", "db", "network", "ui"))
            assertThat(options.minimumLatencyMs).isEqualTo(12)
            assertThat(options.maximumLatencyMs).isEqualTo(5000)
        }

    @Test
    fun `details resolve by stable id regardless of the active query`() =
        withStoredSnapshot { repository, _ ->
            val found = repository.logById("log-3")

            val entry = (found as Result.Success).data
            assertThat(entry).isNotNull()
            assertThat(entry?.message).isEqualTo("Rendered 100% of frames")
            assertThat(entry?.severity).isEqualTo(Severity.INFO)
        }

    /**
     * Writes the fixture with the DAO rather than through a refresh: these tests are about reading
     * a stored snapshot, and staging it directly keeps the endpoint out of the arrangement.
     */
    private fun withStoredSnapshot(block: suspend (LogsRepository, LogsDao) -> Unit) =
        runLogsRepositoryTest(UnusedLogsApi) { repository, dao ->
            dao.replaceSnapshot(SNAPSHOT)
            block(repository, dao)
        }

    private fun largeSnapshot(): List<LogEntity> =
        (0 until LARGE_SNAPSHOT_SIZE).map { index ->
            LogEntity(
                id = "large-log-$index",
                timestampEpochMillis = index.toLong(),
                severity = "INFO",
                tag = "paging",
                message = "Large paging fixture row $index",
                latencyMs = 1,
                isAiGenerated = false,
                sessionId = "paging-test-session",
            )
        }

    private companion object {
        const val LARGE_SNAPSHOT_SIZE = 700
        const val OLDEST_LARGE_SNAPSHOT_ID = "large-log-0"
        const val MAX_PRESENTED_ROWS = 500
    }
}

private object UnusedLogsApi : LogsApi {
    override suspend fun getLogs(): Response<LogsPayloadDto> = error("The read paths must not call the endpoint")
}
