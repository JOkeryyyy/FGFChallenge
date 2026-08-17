package com.example.fgfchallenge.feature.logs.presentation

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Behavior tests for `LogViewerViewModel`: what each [LogViewerAction] does to the single state
 * value, which query value reaches the repository, and — just as importantly — what each leaves
 * alone.
 *
 * The result *list* is still the fixture, so a few assertions deliberately pin *non*-effects: a
 * query does not change the fixture rows, because replacing them with the paged stream belongs to
 * Roadmap #10. The query coordination underneath is real, and the tests below assert it through the
 * repository the ViewModel actually calls.
 */
class LogViewerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeLogsRepository()

    private lateinit var viewModel: LogViewerViewModel

    // Constructed here rather than in a field initializer: JUnit builds the test instance before it
    // evaluates the rules, and `viewModelScope` needs the main dispatcher the rule installs.
    @Before
    fun setUp() {
        viewModel = LogViewerViewModel(repository)
    }

    @Test
    fun `starts on the default all-logs content with no query, newest first, and nothing selected`() {
        val state = viewModel.state.value

        assertThat(state.query).isEqualTo("")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.NewestFirst)
        assertThat(state.selectedLog).isNull()
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Pending)
        assertThat(state.loadState).isEqualTo(LogViewerFixtures.allLogsState().loadState)
    }

    @Test
    fun `query change is retained without touching the fixture result set`() {
        val before = viewModel.state.value.loadState

        viewModel.onAction(LogViewerAction.QueryChanged("net"))

        assertThat(viewModel.state.value.query).isEqualTo("net")
        // Roadmap #10 replaces the fixture with the paged stream; until then rows do not move.
        assertThat(viewModel.state.value.loadState).isSameInstanceAs(before)
    }

    @Test
    fun `clearing the query returns it to blank`() {
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.QueryChanged(""))

        assertThat(viewModel.state.value.query).isEqualTo("")
    }

    @Test
    fun `sort toggle alternates the order without reordering the fixture rows`() {
        val before = viewModel.state.value.loadState

        viewModel.onAction(LogViewerAction.SortOrderToggled)
        assertThat(viewModel.state.value.sortOrder).isEqualTo(LogSortOrder.OldestFirst)

        viewModel.onAction(LogViewerAction.SortOrderToggled)
        assertThat(viewModel.state.value.sortOrder).isEqualTo(LogSortOrder.NewestFirst)

        assertThat(viewModel.state.value.loadState).isSameInstanceAs(before)
    }

    @Test
    fun `selecting a row stores that row's details`() {
        val row = LogViewerFixtures.firstAllLogsRow()

        viewModel.onAction(LogViewerAction.LogSelected(row.row.id))

        assertThat(viewModel.state.value.selectedLog).isEqualTo(row.details)
    }

    @Test
    fun `selecting a row resolves the details belonging to that row and no other`() {
        val rows = viewModel.state.value.logRows()

        rows.forEach { item ->
            viewModel.onAction(LogViewerAction.LogSelected(item.row.id))

            val selected = viewModel.state.value.selectedLog
            assertThat(selected).isNotNull()
            assertThat(selected?.logId).isEqualTo(item.row.id)
            assertThat(selected?.message).isEqualTo(item.row.message)
        }
    }

    @Test
    fun `selecting an unknown log leaves the current selection untouched`() {
        val row = LogViewerFixtures.firstAllLogsRow()
        viewModel.onAction(LogViewerAction.LogSelected(row.row.id))

        viewModel.onAction(LogViewerAction.LogSelected("not-a-log-in-this-result-set"))

        assertThat(viewModel.state.value.selectedLog).isEqualTo(row.details)
    }

    @Test
    fun `dismissing the details clears the selection and keeps everything else`() {
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.SortOrderToggled)
        viewModel.onAction(LogViewerAction.LogSelected(LogViewerFixtures.firstAllLogsRow().row.id))

        viewModel.onAction(LogViewerAction.DetailsDismissed)

        val state = viewModel.state.value
        assertThat(state.selectedLog).isNull()
        assertThat(state.query).isEqualTo("network")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.OldestFirst)
    }

    @Test
    fun `dismissing with nothing selected is a no-op`() {
        viewModel.onAction(LogViewerAction.DetailsDismissed)

        assertThat(viewModel.state.value).isEqualTo(LogViewerFixtures.allLogsState())
    }

    @Test
    fun `retry restores the default all-logs state`() {
        assertRestoresDefaults(LogViewerAction.RetryClicked)
    }

    @Test
    fun `error dismissal restores the default all-logs state`() {
        assertRestoresDefaults(LogViewerAction.ErrorDismissed)
    }

    @Test
    fun `state emits once per action that changes it`() =
        runViewModelTest {
            viewModel.state.test {
                assertThat(awaitItem().query).isEqualTo("")

                viewModel.onAction(LogViewerAction.QueryChanged("net"))
                assertThat(awaitItem().query).isEqualTo("net")

                viewModel.onAction(LogViewerAction.SortOrderToggled)
                assertThat(awaitItem().sortOrder).isEqualTo(LogSortOrder.OldestFirst)

                // StateFlow conflates equal values, so a repeated query is not a new emission.
                viewModel.onAction(LogViewerAction.QueryChanged("net"))
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the default screen state asks for the default query`() {
        assertThat(repository.summaryQueries).containsExactly(LogQuery())
    }

    @Test
    fun `a search change replaces the query both reads use`() =
        runViewModelTest {
            viewModel.onAction(LogViewerAction.QueryChanged("timeout"))
            viewModel.pagedLogs.asSnapshot()

            // The same value, not two equivalent ones assembled separately: this is what stops the
            // list and its counts from describing different criteria.
            assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery(literalSearch = "timeout"))
            assertThat(repository.pagedQueries.last()).isEqualTo(repository.summaryQueries.last())
        }

    @Test
    fun `a sort toggle re-queries with the new direction`() {
        viewModel.onAction(LogViewerAction.SortOrderToggled)

        assertThat(repository.summaryQueries.last())
            .isEqualTo(LogQuery(sortDirection = LogSortDirection.OldestFirst))
    }

    @Test
    fun `edits that normalize to the same query do not restart it`() {
        viewModel.onAction(LogViewerAction.QueryChanged("net"))
        viewModel.onAction(LogViewerAction.QueryChanged("net "))
        viewModel.onAction(LogViewerAction.QueryChanged("  net"))

        // The default query plus one for "net": the whitespace variants trim to the running query.
        assertThat(repository.summaryQueries).hasSize(2)
    }

    @Test
    fun `selection and dismissal do not restart the query`() {
        val row = LogViewerFixtures.firstAllLogsRow()

        viewModel.onAction(LogViewerAction.LogSelected(row.row.id))
        viewModel.onAction(LogViewerAction.DetailsDismissed)

        assertThat(repository.summaryQueries).hasSize(1)
    }

    @Test
    fun `the summary reports the active query's full-result aggregate`() {
        repository.emitSummary(
            query = LogQuery(),
            summary =
                LogSummary(
                    totalCount = 5_000,
                    countBySeverity =
                        mapOf(
                            Severity.ERROR to 1_039,
                            Severity.FATAL to 1_011,
                            Severity.WARN to 1_006,
                            Severity.INFO to 1_005,
                            Severity.DEBUG to 939,
                        ),
                ),
        )

        val summary = viewModel.state.value.summary
        assertThat(summary).isInstanceOf(LogViewerSummaryState.Ready::class)
        assertThat((summary as LogViewerSummaryState.Ready).summary)
            .isEqualTo(LogViewerFixtures.allLogsSummary)
    }

    @Test
    fun `a new query returns the summary to pending so a stale total is never shown as current`() {
        repository.emitSummary(LogQuery(), LogSummary(totalCount = 5_000))
        assertThat(viewModel.state.value.summary).isInstanceOf(LogViewerSummaryState.Ready::class)

        viewModel.onAction(LogViewerAction.QueryChanged("timeout"))

        // Nothing has been counted for "timeout" yet, and the previous 5,000 must not stand in for
        // it — a pending summary is the only honest answer until the new aggregate arrives.
        assertThat(viewModel.state.value.summary).isEqualTo(LogViewerSummaryState.Pending)

        repository.emitSummary(LogQuery(literalSearch = "timeout"), LogSummary(totalCount = 718))

        val counted = viewModel.state.value.summary
        assertThat(counted.readyTotalCount()).isEqualTo(718)
    }

    @Test
    fun `no published state pairs new criteria with the previous query's total`() =
        runViewModelTest {
            repository.emitSummary(LogQuery(), LogSummary(totalCount = 5_000))

            viewModel.state.test {
                assertThat(awaitItem().summary.readyTotalCount()).isEqualTo(5_000)

                viewModel.onAction(LogViewerAction.QueryChanged("timeout"))

                // The *first* value carrying the new search has already dropped the old total —
                // not the one after it, which would still be a frame the screen could render.
                val next = awaitItem()
                assertThat(next.query).isEqualTo("timeout")
                assertThat(next.summary).isEqualTo(LogViewerSummaryState.Pending)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the paged rows come from the query the screen state describes`() =
        runViewModelTest {
            repository.rowsFor = { query ->
                if (query.literalSearch == "timeout") listOf(testLogEntry("match")) else emptyList()
            }

            viewModel.onAction(LogViewerAction.QueryChanged("timeout"))

            assertThat(viewModel.pagedLogs.asSnapshot().map(LogEntry::id)).containsExactly("match")
        }

    /**
     * Runs a test body on the same scheduler the rule installs as `Dispatchers.Main`.
     *
     * Paging's own machinery dispatches through the main dispatcher, so a test that collects
     * [LogViewerViewModel.pagedLogs] under a second, independent scheduler would advance one clock
     * while waiting on the other and simply hang.
     */
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) = runTest(mainDispatcherRule.testDispatcher.scheduler, testBody = body)

    /** Both recovery actions return to the same known-good screen this milestone can produce. */
    private fun assertRestoresDefaults(action: LogViewerAction) {
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.SortOrderToggled)
        viewModel.onAction(LogViewerAction.LogSelected(LogViewerFixtures.firstAllLogsRow().row.id))

        viewModel.onAction(action)

        assertThat(viewModel.state.value).isEqualTo(LogViewerFixtures.allLogsState())
    }

    private fun LogViewerUiState.logRows(): List<LogViewerListItem.LogRow> =
        (loadState as LogViewerLoadState.Content).items.filterIsInstance<LogViewerListItem.LogRow>()

    private fun LogViewerSummaryState.readyTotalCount(): Int? = (this as? LogViewerSummaryState.Ready)?.summary?.totalLogCount
}
