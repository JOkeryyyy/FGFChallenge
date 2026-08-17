package com.example.fgfchallenge.feature.logs.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Behavior tests for `LogViewerViewModel`: what each [LogViewerAction] does to the single state
 * value, and — just as importantly — what it leaves alone.
 *
 * The result set is still the fixture, so several assertions deliberately pin *non*-effects: a
 * query does not filter and a sort toggle does not reorder, because both belong to the processing
 * pipeline in Roadmap #7. Every action is handled synchronously, so no test dispatcher is needed.
 */
class LogViewerViewModelTest {
    private val viewModel = LogViewerViewModel()

    @Test
    fun `starts on the default all-logs content with no query, newest first, and nothing selected`() {
        val state = viewModel.state.value

        assertThat(state.query).isEqualTo("")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.NewestFirst)
        assertThat(state.selectedLog).isNull()
        assertThat(state.loadState).isEqualTo(LogViewerFixtures.allLogsState().loadState)
    }

    @Test
    fun `query change is retained without touching the result set`() {
        val before = viewModel.state.value.loadState

        viewModel.onAction(LogViewerAction.QueryChanged("net"))

        assertThat(viewModel.state.value.query).isEqualTo("net")
        // Roadmap #7 owns filtering; until then the query is recorded and nothing is filtered.
        assertThat(viewModel.state.value.loadState).isSameInstanceAs(before)
    }

    @Test
    fun `clearing the query returns it to blank`() {
        viewModel.onAction(LogViewerAction.QueryChanged("network"))
        viewModel.onAction(LogViewerAction.QueryChanged(""))

        assertThat(viewModel.state.value.query).isEqualTo("")
    }

    @Test
    fun `sort toggle alternates the order without reordering the rows`() {
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
        runTest {
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
}
