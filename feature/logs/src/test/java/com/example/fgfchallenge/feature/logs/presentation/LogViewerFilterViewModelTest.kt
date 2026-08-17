package com.example.fgfchallenge.feature.logs.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
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
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Behavior tests for the filter state the ViewModel owns: the draft the sheet edits, and the line
 * between that draft and the criteria the database is actually asked for.
 *
 * The subject is deliberately the *separation*. A design-system sheet reports taps and picker
 * results; everything about what those mean — which chip toggles off, when a draft becomes the
 * applied filters, and which of those reach `LogQuery` — is asserted here, because that is where it
 * is decided. Several tests therefore pin non-effects: an edit that must not re-query, a Clear All
 * that must not clear the result.
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
        assertThat(viewModel.state.value.filterDraft).isNull()
    }

    @Test
    fun `opening the sheet starts the draft from the filters already applied`() {
        applyFilters(LogFilterSelection(tags = setOf("network")))

        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        // The sheet must open showing what the visible rows were produced with, not a blank slate.
        assertThat(viewModel.state.value.filterDraft).isEqualTo(LogFilterSelection(tags = setOf("network")))
    }

    @Test
    fun `editing the draft changes neither the applied filters nor the query`() {
        val queriesBefore = repository.summaryQueries.size
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(LogViewerAction.FilterTagToggled("network"))
        viewModel.onAction(LogViewerAction.FilterSeverityToggled(Severity.ERROR))
        viewModel.onAction(LogViewerAction.FilterAiGeneratedChanged(AiGeneratedFilter.Yes))

        val state = viewModel.state.value
        assertThat(state.filterDraft?.tags).isEqualTo(setOf("network"))
        assertThat(state.filters).isEqualTo(LogFilterSelection())
        // The point of a draft: a half-composed filter set never reaches the database.
        assertThat(repository.summaryQueries).hasSize(queriesBefore)
    }

    @Test
    fun `chips toggle off when tapped again`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(LogViewerAction.FilterTagToggled("network"))
        viewModel.onAction(LogViewerAction.FilterSeverityToggled(Severity.ERROR))
        viewModel.onAction(LogViewerAction.FilterTagToggled("network"))
        viewModel.onAction(LogViewerAction.FilterSeverityToggled(Severity.ERROR))

        val draft = viewModel.state.value.filterDraft
        assertThat(draft?.tags).isNotNull().isEmpty()
        assertThat(draft?.severities).isNotNull().isEmpty()
    }

    @Test
    fun `applying commits the draft, closes the sheet, and re-queries with it`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(LogViewerAction.FilterTagToggled("network"))
        viewModel.onAction(LogViewerAction.FilterSeverityToggled(Severity.FATAL))

        viewModel.onAction(LogViewerAction.FiltersApplied)

        val state = viewModel.state.value
        assertThat(state.filterDraft).isNull()
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
        viewModel.onAction(LogViewerAction.FilterSeverityToggled(Severity.ERROR))

        viewModel.onAction(LogViewerAction.FiltersApplied)

        assertThat(viewModel.state.value.summary).isEqualTo(LogViewerSummaryState.Pending)
    }

    @Test
    fun `clear all empties the draft and leaves the applied filters until it is applied`() {
        applyFilters(LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.ERROR)))
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(LogViewerAction.FiltersCleared)

        val cleared = viewModel.state.value
        // Still an edit, so the sheet stays open and the result set does not move yet.
        assertThat(cleared.filterDraft).isEqualTo(LogFilterSelection())
        assertThat(cleared.filters).isEqualTo(
            LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.ERROR)),
        )

        viewModel.onAction(LogViewerAction.FiltersApplied)

        assertThat(viewModel.state.value.filters).isEqualTo(LogFilterSelection())
        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
    }

    @Test
    fun `dismissing discards the draft and keeps the applied filters`() {
        applyFilters(LogFilterSelection(tags = setOf("network")))
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(LogViewerAction.FilterTagToggled("auth"))

        viewModel.onAction(LogViewerAction.FilterSheetDismissed)

        val state = viewModel.state.value
        assertThat(state.filterDraft).isNull()
        assertThat(state.filters).isEqualTo(LogFilterSelection(tags = setOf("network")))
    }

    @Test
    fun `an edit arriving after dismissal is dropped rather than reopening the sheet`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(LogViewerAction.FilterSheetDismissed)

        viewModel.onAction(LogViewerAction.FilterTagToggled("network"))
        viewModel.onAction(LogViewerAction.FiltersCleared)

        assertThat(viewModel.state.value.filterDraft).isNull()
    }

    @Test
    fun `applying with no draft open leaves everything as it is`() {
        applyFilters(LogFilterSelection(tags = setOf("network")))
        val before = viewModel.state.value

        viewModel.onAction(LogViewerAction.FiltersApplied)

        assertThat(viewModel.state.value).isEqualTo(before)
    }

    @Test
    fun `the picker's UTC milliseconds become the UTC days they fall in`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)

        viewModel.onAction(
            LogViewerAction.FilterDateRangeChanged(
                startUtcMillis = MAY_20_UTC_MIDNIGHT,
                endUtcMillis = MAY_22_UTC_MIDNIGHT,
            ),
        )
        viewModel.onAction(LogViewerAction.FilterStartTimeChanged(hourOfDayUtc = 17, minuteOfHourUtc = 9))
        viewModel.onAction(LogViewerAction.FilterEndTimeChanged(hourOfDayUtc = 17, minuteOfHourUtc = 11))

        assertThat(viewModel.state.value.filterDraft).isEqualTo(
            LogFilterSelection(
                startDateUtc = LocalDate.of(2025, 5, 20),
                endDateUtc = LocalDate.of(2025, 5, 22),
                startTimeUtc = LocalTime.of(17, 9),
                endTimeUtc = LocalTime.of(17, 11),
            ),
        )
    }

    @Test
    fun `a cleared date range returns both bounds to unset`() {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(
            LogViewerAction.FilterDateRangeChanged(MAY_20_UTC_MIDNIGHT, MAY_22_UTC_MIDNIGHT),
        )

        viewModel.onAction(LogViewerAction.FilterDateRangeChanged(startUtcMillis = null, endUtcMillis = null))

        val draft = viewModel.state.value.filterDraft
        assertThat(draft?.startDateUtc).isNull()
        assertThat(draft?.endDateUtc).isNull()
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

        applyLatencyRange(minimumMs = 0, maximumMs = 10_000)

        // The slider cannot express "no latency filter" other than by spanning its own extent, so
        // the query policy is what turns that back into no predicate — and the badge follows it.
        assertThat(repository.summaryQueries.last()).isEqualTo(LogQuery())
        assertThat(viewModel.state.value.activeFilterCount).isEqualTo(0)
    }

    @Test
    fun `a latency selection inside the snapshot is sent and counted`() {
        repository.emitFilterOptions(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))

        applyLatencyRange(minimumMs = 250, maximumMs = 7_500)

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

    /** Drives the whole sheet cycle, since that is the only way filters become applied. */
    private fun applyFilters(selection: LogFilterSelection) {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        selection.tags.forEach { viewModel.onAction(LogViewerAction.FilterTagToggled(it)) }
        selection.severities.forEach { viewModel.onAction(LogViewerAction.FilterSeverityToggled(it)) }
        viewModel.onAction(LogViewerAction.FilterAiGeneratedChanged(selection.aiGenerated))
        viewModel.onAction(
            LogViewerAction.FilterDateRangeChanged(
                startUtcMillis = selection.startDateUtc?.utcMidnightMillis(),
                endUtcMillis = selection.endDateUtc?.utcMidnightMillis(),
            ),
        )
        viewModel.onAction(LogViewerAction.FiltersApplied)
    }

    private fun applyLatencyRange(
        minimumMs: Long,
        maximumMs: Long,
    ) {
        viewModel.onAction(LogViewerAction.FilterSheetOpened)
        viewModel.onAction(LogViewerAction.FilterLatencyRangeChanged(minimumMs, maximumMs))
        viewModel.onAction(LogViewerAction.FiltersApplied)
    }

    private fun summaryOf(total: Int) = LogSummary(totalCount = total)

    private fun LocalDate.utcMidnightMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val MAY_20_UTC_MIDNIGHT = 1_747_699_200_000L
        const val MAY_22_UTC_MIDNIGHT = 1_747_872_000_000L
    }
}
