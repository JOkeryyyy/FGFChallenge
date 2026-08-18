package com.example.fgfchallenge.feature.logs.presentation

import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Behavior tests for `LogViewerViewModel`: what each [LogViewerAction] does to the single state
 * value, which query value reaches the repository, what the paged stream produces from it, and —
 * just as importantly — what each leaves alone.
 *
 * Three properties get the most attention because they are the ones that fail quietly rather than
 * loudly: that no published state ever pairs new criteria with the previous query's total, that
 * details resolve by ID rather than out of whatever pages happen to be loaded, and that a launch
 * refresh failure is reported as a failure instead of as content.
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
    fun `starts blank, newest first, nothing selected, and nothing counted yet`() {
        val state = viewModel.state.value

        assertThat(state.query).isEqualTo("")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.NewestFirst)
        assertThat(state.selectedLog).isNull()
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Pending)
    }

    // ---- Launch refresh -------------------------------------------------------------------

    @Test
    fun `a launch runs exactly one complete refresh`() {
        assertThat(repository.refreshCount).isEqualTo(1)
        assertThat(viewModel.state.value.refresh).isEqualTo(LogViewerRefreshState.Complete)
    }

    @Test
    fun `a refresh still running holds the screen in its loading state`() {
        val gate = CompletableDeferred<Unit>()
        repository.refreshGate = gate

        val pending = LogViewerViewModel(repository)

        assertThat(pending.state.value.refresh).isEqualTo(LogViewerRefreshState.InProgress)
        assertThat(pending.state.value.showsRefreshFailure).isFalse()
        gate.complete(Unit)
        assertThat(pending.state.value.refresh).isEqualTo(LogViewerRefreshState.Complete)
    }

    @Test
    fun `a failed refresh is reported as a retryable failure rather than as content`() {
        repository.refreshResult = Result.Error(LogsDataError)

        val failed = LogViewerViewModel(repository)

        assertThat(failed.state.value.refresh).isEqualTo(LogViewerRefreshState.Failed(dismissed = false))
        assertThat(failed.state.value.showsRefreshFailure).isTrue()
    }

    @Test
    fun `retry runs the refresh again and clears the failure when it succeeds`() {
        repository.refreshResult = Result.Error(LogsDataError)
        val failed = LogViewerViewModel(repository)
        repository.refreshResult = Result.Success(Unit)

        failed.onAction(LogViewerAction.RetryClicked)

        assertThat(repository.refreshCount).isEqualTo(3) // this test's two, plus setUp's
        assertThat(failed.state.value.refresh).isEqualTo(LogViewerRefreshState.Complete)
    }

    @Test
    fun `retry re-runs the import and keeps the criteria the user composed`() {
        repository.refreshResult = Result.Error(LogsDataError)
        val failed = LogViewerViewModel(repository)
        failed.onAction(LogViewerAction.QueryChanged("timeout"))
        failed.onAction(LogViewerAction.SortOrderToggled)

        failed.onAction(LogViewerAction.RetryClicked)

        // A failed import says nothing about what the user asked for, so resetting their inputs
        // would discard work the failure never invalidated.
        assertThat(failed.state.value.query).isEqualTo("timeout")
        assertThat(failed.state.value.sortOrder).isEqualTo(LogSortOrder.OldestFirst)
    }

    @Test
    fun `dismissing the failure closes the dialog without claiming the snapshot is current`() {
        repository.refreshResult = Result.Error(LogsDataError)
        val failed = LogViewerViewModel(repository)

        failed.onAction(LogViewerAction.ErrorDismissed)

        // The failure itself stands — nothing about it stopped being true — and no second refresh
        // was started, which is what separates Dismiss from Retry.
        assertThat(failed.state.value.refresh).isEqualTo(LogViewerRefreshState.Failed(dismissed = true))
        assertThat(failed.state.value.showsRefreshFailure).isFalse()
        assertThat(repository.refreshCount).isEqualTo(2)
    }

    @Test
    fun `retry survives dismissal, so a dismissed failure is not a dead end`() {
        repository.refreshResult = Result.Error(LogsDataError)
        val failed = LogViewerViewModel(repository)
        failed.onAction(LogViewerAction.ErrorDismissed)

        // Retry lives only on the dialog the user just closed, so the notice replacing it is the
        // one remaining way back to a current snapshot.
        assertThat(failed.state.value.showsStaleSnapshotNotice).isTrue()

        repository.refreshResult = Result.Success(Unit)
        failed.onAction(LogViewerAction.RetryClicked)

        assertThat(failed.state.value.refresh).isEqualTo(LogViewerRefreshState.Complete)
        assertThat(failed.state.value.showsStaleSnapshotNotice).isFalse()
    }

    @Test
    fun `dismissing does nothing when the refresh succeeded`() {
        viewModel.onAction(LogViewerAction.ErrorDismissed)

        assertThat(viewModel.state.value.refresh).isEqualTo(LogViewerRefreshState.Complete)
    }

    // ---- Query coordination ---------------------------------------------------------------

    @Test
    fun `the default screen state asks for the default query`() {
        assertThat(repository.summaryQueries).containsExactly(LogQuery())
    }

    @Test
    fun `a search change replaces the query both reads use`() =
        runViewModelTest {
            viewModel.onAction(LogViewerAction.QueryChanged("timeout"))
            settleTypedText()
            viewModel.pagedLogs.asSnapshot()

            // The same value, not two equivalent ones assembled separately: this is what stops the
            // list and its counts from describing different criteria.
            assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery(literalSearch = "timeout"))
            assertThat(repository.pagedQueries.last()).isEqualTo(repository.summaryQueries.last())
        }

    @Test
    fun `clearing the query returns it to blank`() {
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.QueryChanged(""))

        assertThat(viewModel.state.value.query).isEqualTo("")
    }

    @Test
    fun `a sort toggle re-queries with the new direction`() {
        viewModel.onAction(LogViewerAction.SortOrderToggled)
        assertThat(viewModel.state.value.sortOrder).isEqualTo(LogSortOrder.OldestFirst)
        assertThat(repository.summaryQueries.last())
            .isEqualTo(LogQuery(sortDirection = LogSortDirection.OldestFirst))

        viewModel.onAction(LogViewerAction.SortOrderToggled)
        assertThat(viewModel.state.value.sortOrder).isEqualTo(LogSortOrder.NewestFirst)
    }

    @Test
    fun `edits that normalize to the same query do not restart it`() {
        viewModel.onAction(LogViewerAction.QueryChanged("net"))
        settleTypedText()
        viewModel.onAction(LogViewerAction.QueryChanged("net "))
        settleTypedText()
        viewModel.onAction(LogViewerAction.QueryChanged("  net"))
        settleTypedText()

        // The default query plus one for "net": the whitespace variants trim to the running query.
        // Each one is settled separately, so this is the normalization rule doing the work rather
        // than the debounce swallowing the edits.
        assertThat(repository.summaryQueries).hasSize(2)
    }

    @Test
    fun `typing a word asks the database once, when the typing stops`() {
        val typed = "timeout"
        typed.indices.forEach { end -> viewModel.onAction(LogViewerAction.QueryChanged(typed.take(end + 1))) }

        // Seven keystrokes, and so far nothing beyond the query the screen opened with: a partial
        // word is text on its way somewhere, not criteria worth a Pager and a full-result count.
        assertThat(repository.summaryQueries).containsExactly(LogQuery())
        // The text itself is not held back — only what the database is asked for.
        assertThat(viewModel.state.value.query).isEqualTo(typed)

        settleTypedText()

        assertThat(repository.summaryQueries).containsExactly(LogQuery(), LogQuery(literalSearch = typed))
    }

    @Test
    fun `a deliberate commit made mid-typing is not held back by it`() {
        viewModel.onAction(LogViewerAction.QueryChanged("time"))
        viewModel.onAction(LogViewerAction.SortOrderToggled)

        // A sort toggle is a decision rather than a character on the way to one, so it re-queries
        // at once — with the text the database has been given so far, which is still none.
        assertThat(repository.summaryQueries.last())
            .isEqualTo(LogQuery(sortDirection = LogSortDirection.OldestFirst))

        settleTypedText()

        // And the text that was still being typed then joins it, rather than being lost to it.
        assertThat(repository.summaryQueries.last())
            .isEqualTo(LogQuery(literalSearch = "time", sortDirection = LogSortDirection.OldestFirst))
    }

    @Test
    fun `clearing the field returns to the unfiltered list without a wait`() {
        viewModel.onAction(LogViewerAction.QueryChanged("timeout"))
        settleTypedText()

        viewModel.onAction(LogViewerAction.QueryChanged(""))

        // Clearing ends a search instead of composing one, so it is not made to sit out the pause
        // showing results the user has just dismissed.
        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
    }

    @Test
    fun `showing and hiding the search field keeps the text the user typed`() {
        viewModel.onAction(LogViewerAction.SearchOpened)
        assertThat(viewModel.state.value.isSearchExpanded).isTrue()

        viewModel.onAction(LogViewerAction.QueryChanged("timeout"))
        viewModel.onAction(LogViewerAction.SearchDismissed)

        // The text is the user's search, not the control's state. Collapsing withdraws the field
        // only, so a search they never cleared goes on narrowing the result — which is exactly why
        // the app bar's search action keeps an indicator once the field is out of sight.
        assertThat(viewModel.state.value.isSearchExpanded).isFalse()
        assertThat(viewModel.state.value.query).isEqualTo("timeout")
    }

    @Test
    fun `search visibility alone neither re-queries nor drops the counted aggregate`() {
        repository.emitSummary(LogQuery(), LogSummary(totalCount = 5_000))
        val counted = viewModel.state.value.summary
        assertThat(counted.readyTotalCount()).isEqualTo(5_000)

        viewModel.onAction(LogViewerAction.SearchOpened)
        viewModel.onAction(LogViewerAction.SearchDismissed)
        settleTypedText()

        // Nothing `toLogQuery` reads changed, so the running Pager and aggregate keep their results
        // instead of being replaced by identical ones — and the total stays counted rather than
        // returning to Pending, which is what `updateQueryInputs` would have done to it.
        assertThat(repository.summaryQueries).containsExactly(LogQuery())
        assertThat(viewModel.state.value.summary).isEqualTo(counted)
    }

    @Test
    fun `selection and dismissal do not restart the query`() {
        repository.store(testLogEntry("1711-58123"))

        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))
        viewModel.onAction(LogViewerAction.DetailsDismissed)

        assertThat(repository.summaryQueries).hasSize(1)
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

    // ---- Full-result summary ---------------------------------------------------------------

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
        settleTypedText()

        assertThat(
            viewModel.state.value.summary
                .readyTotalCount(),
        ).isEqualTo(718)
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
    fun `an empty result is only claimed once the aggregate has counted zero`() {
        assertThat(viewModel.state.value.hasNoMatches).isFalse()

        repository.emitSummary(LogQuery(), LogSummary(totalCount = 0))

        assertThat(viewModel.state.value.hasNoMatches).isTrue()
    }

    // ---- Details by stable ID ---------------------------------------------------------------

    @Test
    fun `selecting a row resolves that row's details from the repository`() {
        repository.store(testLogEntry("1711-58123", message = "Connection timed out"))

        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))

        val selected = viewModel.state.value.selectedLog
        assertThat(selected).isNotNull()
        assertThat(selected?.logId).isEqualTo("1711-58123")
        assertThat(selected?.message).isEqualTo("Connection timed out")
        assertThat(repository.logByIdRequests).containsExactly("1711-58123")
    }

    @Test
    fun `a row on a page the list is not holding is still selectable`() {
        // Nothing is served through the paged stream, so a lookup that scanned loaded rows would
        // find nothing here — which is exactly the case the ID lookup exists for.
        repository.store(testLogEntry("evicted-page-row"))
        assertThat(repository.rowsFor(LogQuery())).isEmpty()

        viewModel.onAction(LogViewerAction.LogSelected("evicted-page-row"))

        assertThat(
            viewModel.state.value.selectedLog
                ?.logId,
        ).isEqualTo("evicted-page-row")
    }

    @Test
    fun `selecting an unknown log leaves the current selection untouched`() {
        repository.store(testLogEntry("1711-58123"))
        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))

        viewModel.onAction(LogViewerAction.LogSelected("not-in-this-snapshot"))

        // A stale tap arriving after the list changed must not dismiss a sheet the user is reading.
        assertThat(
            viewModel.state.value.selectedLog
                ?.logId,
        ).isEqualTo("1711-58123")
    }

    @Test
    fun `a failed lookup leaves the current selection untouched`() {
        repository.store(testLogEntry("1711-58123"))
        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))
        repository.logByIdFails = true

        viewModel.onAction(LogViewerAction.LogSelected("1710-59384"))

        assertThat(
            viewModel.state.value.selectedLog
                ?.logId,
        ).isEqualTo("1711-58123")
    }

    @Test
    fun `a lookup still in flight when the sheet is dismissed cannot reopen it`() {
        val gate = CompletableDeferred<Unit>()
        repository.store(testLogEntry("1711-58123"))
        repository.logByIdGate = gate

        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))
        viewModel.onAction(LogViewerAction.DetailsDismissed)
        gate.complete(Unit)

        assertThat(viewModel.state.value.selectedLog).isNull()
    }

    @Test
    fun `dismissing the details clears the selection and keeps everything else`() {
        repository.store(testLogEntry("1711-58123"))
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.SortOrderToggled)
        viewModel.onAction(LogViewerAction.LogSelected("1711-58123"))

        viewModel.onAction(LogViewerAction.DetailsDismissed)

        val state = viewModel.state.value
        assertThat(state.selectedLog).isNull()
        assertThat(state.query).isEqualTo("network")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.OldestFirst)
    }

    @Test
    fun `dismissing with nothing selected is a no-op`() {
        val before = viewModel.state.value

        viewModel.onAction(LogViewerAction.DetailsDismissed)

        assertThat(viewModel.state.value).isEqualTo(before)
    }

    // ---- Paged rows -------------------------------------------------------------------------

    @Test
    fun `the paged rows come from the query the screen state describes`() =
        runViewModelTest {
            repository.rowsFor = { query ->
                if (query.literalSearch == "timeout") listOf(testLogEntry("match")) else emptyList()
            }

            viewModel.onAction(LogViewerAction.QueryChanged("timeout"))
            settleTypedText()

            assertThat(viewModel.pagedLogs.asSnapshot().logRowIds()).containsExactly("match")
        }

    @Test
    fun `the paged list groups its rows under one header per UTC minute`() =
        runViewModelTest {
            repository.rowsFor = {
                listOf(
                    testLogEntry("a", timestamp = "2025-05-22T17:11:58.123Z"),
                    testLogEntry("b", timestamp = "2025-05-22T17:11:11.098Z"),
                    testLogEntry("c", timestamp = "2025-05-22T17:10:59.384Z"),
                )
            }

            val items = viewModel.pagedLogs.asSnapshot()

            // Headers are inserted by the transformation, not carried by the rows, so this is the
            // grouping the list actually renders — including that the first row gets one too.
            assertThat(items.map { it.stableKey }).containsExactly(
                "minute:2025-05-22T17:11Z",
                "log:a",
                "log:b",
                "minute:2025-05-22T17:10Z",
                "log:c",
            )
        }

    @Test
    fun `the paged list carries display-ready rows rather than stored values`() =
        runViewModelTest {
            repository.rowsFor = { listOf(testLogEntry("a", severity = Severity.FATAL)) }

            val row =
                viewModel.pagedLogs
                    .asSnapshot()
                    .filterIsInstance<LogViewerListItem.LogRow>()
                    .single()

            assertThat(row.row.severityLabel).isEqualTo("FATAL")
            assertThat(row.row.time).isEqualTo("58.123")
        }

    /**
     * Runs a test body on the same scheduler the rule installs as `Dispatchers.Main`.
     *
     * Paging's own machinery dispatches through the main dispatcher, so a test that collects
     * [LogViewerViewModel.pagedLogs] under a second, independent scheduler would advance one clock
     * while waiting on the other and simply hang.
     */
    private fun runViewModelTest(body: suspend TestScope.() -> Unit) = runTest(mainDispatcherRule.testDispatcher.scheduler, testBody = body)

    /**
     * Advances past the pause the ViewModel waits out before typed text reaches the database.
     *
     * The wait is virtual, so this costs nothing in wall-clock time; what it costs is that a test
     * about search has to say when the user stopped typing, which is the point — a test that
     * asserted a query without this would be asserting the keystroke behavior that was removed.
     */
    private fun settleTypedText() {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun List<LogViewerListItem>.logRowIds(): List<String> = filterIsInstance<LogViewerListItem.LogRow>().map { it.row.id }

    private fun LogViewerSummaryState.readyTotalCount(): Int? = (this as? LogViewerSummaryState.Ready)?.summary?.totalLogCount
}
