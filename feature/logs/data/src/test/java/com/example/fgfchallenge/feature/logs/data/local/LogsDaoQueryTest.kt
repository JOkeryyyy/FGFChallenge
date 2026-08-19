package com.example.fgfchallenge.feature.logs.data.local

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture.SNAPSHOT
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.data.model.Severity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

/**
 * Verifies the query engine against real SQLite: what each condition matches, how conditions
 * combine, how results are ordered, and that the aggregate select counts exactly the rows the
 * paged select returns.
 *
 * Assertions are written as ordered ID lists, so a test that passes proves both the predicate and
 * the ordering rather than only set membership.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LogsDaoQueryTest {
    private var database: LogsDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    @Test
    fun `default query returns every row newest first, breaking timestamp ties by id`() =
        withSnapshot { dao ->
            // log-1 and log-2 share a timestamp: descending ID is what keeps their relative order
            // stable between two loads of the same page.
            assertThat(dao.matchingIds(LogQuery()))
                .isEqualTo(listOf("log-6", "log-5", "log-4", "log-3", "log-2", "log-1"))
        }

    @Test
    fun `oldest first reverses both the timestamp and the tie-break`() =
        withSnapshot { dao ->
            val query = LogQuery(sortDirection = LogSortDirection.OldestFirst)

            assertThat(dao.matchingIds(query))
                .isEqualTo(listOf("log-1", "log-2", "log-3", "log-4", "log-5", "log-6"))
        }

    @Test
    fun `search matches message regardless of case`() =
        withSnapshot { dao ->
            val query = LogQuery(literalSearch = "timeout")

            // "Connection timeout", "Timeout escalated", and "TIMEOUT while flushing".
            assertThat(dao.matchingIds(query)).isEqualTo(listOf("log-6", "log-5", "log-1"))
        }

    @Test
    fun `search matches id as well as message`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(literalSearch = "log-3")))
                .isEqualTo(listOf("log-3"))
        }

    @Test
    fun `search matches a substring rather than a whole field`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(literalSearch = "escalat")))
                .isEqualTo(listOf("log-5"))
        }

    @Test
    fun `search treats a percent sign as literal text, not a wildcard`() =
        withSnapshot { dao ->
            // Unescaped, this pattern would match every row in the table.
            assertThat(dao.matchingIds(LogQuery(literalSearch = "%")))
                .isEqualTo(listOf("log-3"))
        }

    @Test
    fun `search treats an underscore as literal text, not a single-character wildcard`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(literalSearch = "_")))
                .isEqualTo(listOf("log-4"))
        }

    @Test
    fun `search treats the escape character itself as literal text`() =
        withSnapshot { dao ->
            dao.insertAll(
                listOf(
                    LogsTestFixture.entity(
                        id = "log-7",
                        timestamp = "2026-08-16T17:14:00Z",
                        severity = "INFO",
                        tag = "ui",
                        message = """Path C:\logs written""",
                        latencyMs = 7,
                        isAiGenerated = false,
                    ),
                ),
            )

            assertThat(dao.matchingIds(LogQuery(literalSearch = """C:\logs""")))
                .isEqualTo(listOf("log-7"))
        }

    @Test
    fun `blank search adds no condition`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(literalSearch = "   ")))
                .isEqualTo(dao.matchingIds(LogQuery()))
        }

    @Test
    fun `selected tags combine with OR inside their category`() =
        withSnapshot { dao ->
            val query = LogQuery(selectedTags = setOf("network", "db"))

            assertThat(dao.matchingIds(query))
                .isEqualTo(listOf("log-6", "log-5", "log-2", "log-1"))
        }

    @Test
    fun `selected severities combine with OR inside their category`() =
        withSnapshot { dao ->
            val query = LogQuery(selectedSeverities = setOf(Severity.ERROR, Severity.FATAL))

            assertThat(dao.matchingIds(query))
                .isEqualTo(listOf("log-6", "log-5", "log-1"))
        }

    @Test
    fun `active categories combine with AND`() =
        withSnapshot { dao ->
            val query =
                LogQuery(
                    selectedTags = setOf("network"),
                    selectedSeverities = setOf(Severity.ERROR, Severity.FATAL),
                )

            // log-6 is an ERROR but tagged db, so the tag category removes it.
            assertThat(dao.matchingIds(query)).isEqualTo(listOf("log-5", "log-1"))
        }

    @Test
    fun `search and structured filters narrow each other`() =
        withSnapshot { dao ->
            val query =
                LogQuery(
                    literalSearch = "timeout",
                    selectedSeverities = setOf(Severity.ERROR),
                )

            assertThat(dao.matchingIds(query)).isEqualTo(listOf("log-6", "log-1"))
        }

    @Test
    fun `ai generated constraint filters to the requested value only`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(aiGeneratedConstraint = true)))
                .isEqualTo(listOf("log-5", "log-3", "log-1"))
            assertThat(dao.matchingIds(LogQuery(aiGeneratedConstraint = false)))
                .isEqualTo(listOf("log-6", "log-4", "log-2"))
        }

    @Test
    fun `an inactive ai constraint keeps both values`() =
        withSnapshot { dao ->
            assertThat(dao.matchingIds(LogQuery(aiGeneratedConstraint = null)))
                .isEqualTo(dao.matchingIds(LogQuery()))
        }

    @Test
    fun `the time range includes its start and excludes its end`() =
        withSnapshot { dao ->
            val query =
                LogQuery(
                    startInclusiveUtc = Instant.parse("2026-08-16T17:11:00Z"),
                    endExclusiveUtc = Instant.parse("2026-08-16T17:12:00Z"),
                )

            // log-4 sits exactly on the inclusive start; log-5 sits exactly on the exclusive end.
            assertThat(dao.matchingIds(query)).isEqualTo(listOf("log-4"))
        }

    @Test
    fun `each time bound can constrain on its own`() =
        withSnapshot { dao ->
            val fromLog4 = LogQuery(startInclusiveUtc = Instant.parse("2026-08-16T17:11:00Z"))
            val beforeLog4 = LogQuery(endExclusiveUtc = Instant.parse("2026-08-16T17:11:00Z"))

            assertThat(dao.matchingIds(fromLog4)).isEqualTo(listOf("log-6", "log-5", "log-4"))
            assertThat(dao.matchingIds(beforeLog4)).isEqualTo(listOf("log-3", "log-2", "log-1"))
        }

    @Test
    fun `both latency bounds are inclusive`() =
        withSnapshot { dao ->
            val query =
                LogQuery(
                    minimumLatencyInclusive = 120,
                    maximumLatencyInclusive = 300,
                )

            // log-2 is exactly 120ms and log-4 is exactly 300ms; both must be kept.
            assertThat(dao.matchingIds(query)).isEqualTo(listOf("log-4", "log-2"))
        }

    @Test
    fun `a query matching nothing returns no rows and a zero summary`() =
        withSnapshot { dao ->
            val query = LogQuery(literalSearch = "no such message")

            assertThat(dao.matchingIds(query)).isEmpty()
            assertThat(dao.summaryOf(query).totalCount).isEqualTo(0)
        }

    @Test
    fun `the summary counts every severity in the complete result`() =
        withSnapshot { dao ->
            val summary = dao.summaryOf(LogQuery())

            assertThat(summary.totalCount).isEqualTo(6)
            assertThat(summary.countBySeverity).isEqualTo(
                mapOf(
                    Severity.DEBUG to 1,
                    Severity.INFO to 1,
                    Severity.WARN to 1,
                    Severity.ERROR to 2,
                    Severity.FATAL to 1,
                ),
            )
        }

    @Test
    fun `the summary describes the whole result even when only part of it is paged in`() =
        withSnapshot { dao ->
            val query = LogQuery()

            // The list holds two rows; the count must still describe all six matches, which is
            // what stops the UI from reporting `LazyPagingItems.itemCount` as the result count.
            assertThat(dao.matchingIds(query, loadSize = 2)).isEqualTo(listOf("log-6", "log-5"))
            assertThat(dao.summaryOf(query).totalCount).isEqualTo(6)
        }

    @Test
    fun `the summary and the list agree for every representative query`() =
        withSnapshot { dao ->
            val queries =
                listOf(
                    LogQuery(),
                    LogQuery(literalSearch = "timeout"),
                    LogQuery(literalSearch = "%"),
                    LogQuery(selectedTags = setOf("network", "db")),
                    LogQuery(selectedSeverities = setOf(Severity.ERROR, Severity.FATAL)),
                    LogQuery(aiGeneratedConstraint = true),
                    LogQuery(
                        startInclusiveUtc = Instant.parse("2026-08-16T17:10:30Z"),
                        endExclusiveUtc = Instant.parse("2026-08-16T17:13:00Z"),
                    ),
                    LogQuery(minimumLatencyInclusive = 120, maximumLatencyInclusive = 300),
                    LogQuery(
                        literalSearch = "timeout",
                        selectedTags = setOf("network"),
                        selectedSeverities = setOf(Severity.ERROR, Severity.FATAL),
                        aiGeneratedConstraint = true,
                    ),
                    LogQuery(literalSearch = "no such message"),
                )

            for (query in queries) {
                assertThat(dao.summaryOf(query).totalCount, "total for $query")
                    .isEqualTo(dao.matchingIds(query).size)
            }
        }

    @Test
    fun `an unrecognized stored severity counts toward the total without becoming an error`() =
        withSnapshot { dao ->
            dao.insertAll(
                listOf(
                    LogsTestFixture.entity(
                        id = "log-7",
                        timestamp = "2026-08-16T17:15:00Z",
                        severity = "TRACE",
                        tag = "ui",
                        message = "Span opened",
                        latencyMs = 3,
                        isAiGenerated = false,
                    ),
                ),
            )

            val summary = dao.summaryOf(LogQuery())

            assertThat(summary.totalCount).isEqualTo(7)
            assertThat(summary.countBySeverity[Severity.UNKNOWN]).isEqualTo(1)
            assertThat(summary.countBySeverity[Severity.ERROR]).isEqualTo(2)
        }

    /**
     * Opens a database holding [SNAPSHOT] and runs [block] against its DAO.
     *
     * Room's work is bound to the test's own dispatcher, so queries complete inside `runTest`'s
     * scheduler rather than on a background thread the test would have to wait for.
     */
    private fun withSnapshot(block: suspend (LogsDao) -> Unit) =
        runTest {
            val database =
                createInMemoryLogsDatabase(StandardTestDispatcher(testScheduler))
                    .also { this@LogsDaoQueryTest.database = it }
            val dao = database.logsDao()
            dao.replaceSnapshot(SNAPSHOT)

            block(dao)
        }
}
