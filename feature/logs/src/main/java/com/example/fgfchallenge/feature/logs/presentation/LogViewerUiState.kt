package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.latencyExtent

/**
 * The log viewer's complete screen state: one immutable value produced by `LogViewerViewModel` and
 * rendered by `LogViewerScreen`.
 *
 * The screen-wide inputs ([query], [filters], [sortOrder], [selectedLog]) sit beside — not inside —
 * the mutually exclusive [loadState], so a query survives a reload and the details sheet is simply
 * "[selectedLog] is not null" rather than a separate visibility flag.
 *
 * [query], [filters], and [sortOrder] are also the query inputs: together they are what `toLogQuery`
 * turns into the one `LogQuery` the paged rows and [summary] are both read with. Everything here
 * stays bounded — no page of rows, no match list — which is what lets Paging own the working set.
 */
@Immutable
internal data class LogViewerUiState(
    val query: String = "",
    /** The filters currently *applied* — the only ones the query is derived from. */
    val filters: LogFilterSelection = LogFilterSelection(),
    /**
     * The filter sheet's uncommitted draft, and its visibility: the sheet is open exactly while
     * this is not null, the same way the details sheet is "[selectedLog] is not null".
     *
     * Draft edits deliberately do not reach [filters], so a half-composed filter set never issues a
     * database query. Apply is what moves this value across; dismissing discards it.
     */
    val filterDraft: LogFilterSelection? = null,
    /** Unfiltered snapshot metadata the sheet's controls are built from: tags and latency extent. */
    val filterOptions: LogFilterOptions = LogFilterOptions(),
    val sortOrder: LogSortOrder = LogSortOrder.NewestFirst,
    /** Aggregates over the complete result of the active query, never over the loaded rows. */
    val summary: LogViewerSummaryState = LogViewerSummaryState.Pending,
    val selectedLog: LogDetailsUi? = null,
    val loadState: LogViewerLoadState = LogViewerLoadState.Loading,
)

/**
 * How many filter *categories* the active query restricts on, which is what the Filter control's
 * badge reports.
 *
 * Derived from the query rather than counted off [LogViewerUiState.filters], so the badge and the
 * database agree by construction: a latency range spanning the whole snapshot, or a reversed date
 * range, restricts nothing and is normalized away by `toLogQuery` — counting the raw selection
 * would claim a filter the rows were never narrowed by. The search text is excluded because it is
 * its own control, not one of the structured filters the sheet edits.
 */
internal val LogViewerUiState.activeFilterCount: Int
    get() =
        with(toLogQuery(filterOptions.latencyExtent())) {
            listOf(
                selectedTags.isNotEmpty(),
                selectedSeverities.isNotEmpty(),
                aiGeneratedConstraint != null,
                startInclusiveUtc != null || endExclusiveUtc != null,
                minimumLatencyInclusive != null || maximumLatencyInclusive != null,
            ).count { it }
        }

/**
 * Whether the active query's full-result aggregate has arrived yet.
 *
 * The distinction matters because the alternative is worse than a missing number: reusing the
 * previous query's total while the new one is still being counted would label an old result as the
 * current one. A query change therefore returns this to [Pending] rather than leaving a stale
 * [Ready] in place — which is also why a zero result count is only trustworthy as
 * `Ready(totalLogCount = 0)`, the condition the no-results state waits for.
 */
@Immutable
internal sealed interface LogViewerSummaryState {
    /** No aggregate for the active query yet: nothing is counted, not "counted zero". */
    data object Pending : LogViewerSummaryState

    data class Ready(
        val summary: SeveritySummaryUi,
    ) : LogViewerSummaryState
}

/**
 * Which of the three mutually exclusive results the screen body shows.
 *
 * A filtered result with no matches is [Content] with an empty item list — not [Error] — because
 * the search still succeeded.
 */
@Immutable
internal sealed interface LogViewerLoadState {
    /** Initial load in flight: skeleton placeholders only, no fabricated log values. */
    data object Loading : LogViewerLoadState

    /**
     * Retryable load failure, presented as a modal dialog over the shell.
     *
     * The copy is carried as resolved strings because this milestone only ever produces the state
     * from fixtures; Roadmap #5 replaces it with a typed failure mapped at the screen boundary.
     */
    data class Error(
        val title: String,
        val message: String,
    ) : LogViewerLoadState

    /**
     * A rendered result set. [items] is the flattened header/row list; it is empty when the active
     * query matches nothing, in which case [severitySummary] reports zero counts.
     *
     * [resultCount] is the raw total the query matched, not `items.size` — the screen formats it,
     * and it describes the whole dataset rather than the rows currently materialized.
     */
    @Immutable
    data class Content(
        val resultCount: Int,
        val severitySummary: SeveritySummaryUi,
        val items: List<LogViewerListItem>,
    ) : LogViewerLoadState
}
