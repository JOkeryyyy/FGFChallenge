package com.example.fgfchallenge.feature.logs.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Behavior tests for the filter state the ViewModel owns: the *applied* filters, the sheet's
 * visibility, and the line between an edit and the criteria the database is actually asked for.
 *
 * The subject is deliberately the boundary rather than the editing. The uncommitted edit belongs to
 * `LogFilterSheetHost` and never reaches this class — chip toggling, picker conversion, and the
 * Apply-time round-trip are pinned in `LogFilterSheetEditTest` as plain functions. What is asserted
 * here is what crosses: that opening a sheet queries nothing, that Apply is the only thing that
 * does, and that a selection which restricts nothing is not sent as a predicate.
 */
class LogViewerFilterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeLogsRepository()

    private lateinit var viewModel: LogViewerViewModel

    @Before
    fun setUp() {
        viewModel = LogViewerViewModel(repository)
    }

    @Test
    fun `no sheet is open until the filter control is used`() {
        assertThat(viewModel.state.value.isFilterSheetOpen).isFalse()
    }

    @Test
    fun `opening and dismissing the sheet changes visibility and queries nothing`() {
        val queriesBefore = repository.summaryQueries.size

        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        assertThat(viewModel.state.value.isFilterSheetOpen).isTrue()

        viewModel.onAction(LogViewerAction.FilterSheetDismissed)
        assertThat(viewModel.state.value.isFilterSheetOpen).isFalse()

        // A filter session that is opened, edited, and abandoned costs the database nothing. The
        // edits are not even represented here — they lived and died in the sheet's own state.
        assertThat(repository.summaryQueries).hasSize(queriesBefore)
    }

    @Test
    fun `applying commits the selection, closes the sheet, and re-queries with it`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(
            LogViewerAction.FiltersApplied(
                LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.FATAL)),
            ),
        )

        val state = viewModel.state.value
        assertThat(state.isFilterSheetOpen).isFalse()
        assertThat(state.filters).isEqualTo(
            LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.FATAL)),
        )
        assertThat(repository.summaryQueries.last()).isEqualTo(
            LogQuery(selectedTags = setOf("network"), selectedSeverities = setOf(Severity.FATAL)),
        )
    }

    @Test
    fun `applying returns the summary to pending so the previous total is not shown as current`() {
        repository.emitSummary(LogQuery(), summaryOf(total = 5_000))
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(LogViewerAction.FiltersApplied(LogFilterSelection(severities = setOf(Severity.ERROR))))

        assertThat(viewModel.state.value.summary).isEqualTo(LogViewerSummaryState.Pending)
    }

    @Test
    fun `dismissing keeps the applied filters exactly as they were`() {
        applyFilters(LogFilterSelection(tags = setOf("network")))
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(LogViewerAction.FilterSheetDismissed)

        val state = viewModel.state.value
        assertThat(state.isFilterSheetOpen).isFalse()
        // Dismissal is not an implicit Apply: whatever the user was editing is discarded with the
        // sheet, and the rows they were reading do not move.
        assertThat(state.filters).isEqualTo(LogFilterSelection(tags = setOf("network")))
    }

    @Test
    fun `re-applying an unchanged selection starts no new query`() {
        applyFilters(LogFilterSelection(tags = setOf("network")))
        val queriesAfterFirst = repository.summaryQueries.size

        applyFilters(LogFilterSelection(tags = setOf("network")))

        // Apply is cheap to press twice because the derived query is identical and `activeQuery`
        // drops it as a duplicate — no Pager and no aggregate restart.
        assertThat(repository.summaryQueries).hasSize(queriesAfterFirst)
    }

    @Test
    fun `clearing the selection and applying returns the unfiltered result`() {
        applyFilters(LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.ERROR)))

        applyFilters(LogFilterSelection())

        assertThat(viewModel.state.value.filters).isEqualTo(LogFilterSelection())
        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
    }

    @Test
    fun `filter options from the repository reach the state the sheet is built from`() {
        repository.emitFilterOptions(
            LogFilterOptions(
                availableTags = listOf("auth", "cache", "network"),
                minimumLatencyMs = 5,
                maximumLatencyMs = 9_000,
            ),
        )

        val options = viewModel.state.value.filterOptions
        assertThat(options.availableTags).containsExactly("auth", "cache", "network")
        assertThat(options.maximumLatencyMs).isEqualTo(9_000L)
    }

    @Test
    fun `a latency selection spanning the whole snapshot is not sent as a predicate`() {
        repository.emitFilterOptions(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))

        applyFilters(LogFilterSelection(minimumLatencyMs = 0, maximumLatencyMs = 10_000))

        // The sheet normally reports full-width bounds as absent, but a selection carried over from
        // a previous snapshot can still arrive spelled out. The query policy is what turns that back
        // into no predicate — and the badge follows it.
        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(0)
    }

    @Test
    fun `a latency selection inside the snapshot is sent and counted`() {
        repository.emitFilterOptions(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))

        applyFilters(LogFilterSelection(minimumLatencyMs = 250, maximumLatencyMs = 7_500))

        assertThat(repository.summaryQueries.last()).isEqualTo(
            LogQuery(minimumLatencyInclusive = 250, maximumLatencyInclusive = 7_500),
        )
        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(1)
    }

    @Test
    fun `the active filter count reports categories, not selected values`() {
        applyFilters(
            LogFilterSelection(
                tags = setOf("network", "auth", "cache"),
                severities = setOf(Severity.ERROR, Severity.FATAL),
                aiGenerated = AiGeneratedFilter.No,
                startDateUtc = LocalDate.of(2025, 5, 22),
            ),
        )

        // Three categories are active — tags, severities, AI — plus the open-ended date range.
        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(4)
    }

    @Test
    fun `the search text is not one of the filter categories`() {
        viewModel.onAction(LogViewerAction.QueryChanged("timeout"))

        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(0)
    }

    @Test
    fun `a reversed date range restricts nothing, so it is not counted as an active filter`() {
        applyFilters(
            LogFilterSelection(
                startDateUtc = LocalDate.of(2025, 5, 23),
                endDateUtc = LocalDate.of(2025, 5, 22),
            ),
        )

        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(0)
    }

    /**
     * Applies a finished selection the way the sheet host does — one action, carrying the whole
     * thing, which is the only way filters become applied.
     */
    private fun applyFilters(selection: LogFilterSelection) {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(LogViewerAction.FiltersApplied(selection))
    }

    private fun summaryOf(total: Int) = LogSummary(totalCount = total)
}
