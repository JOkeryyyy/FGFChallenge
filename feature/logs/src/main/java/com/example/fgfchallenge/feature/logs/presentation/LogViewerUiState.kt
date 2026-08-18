package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.latencyExtent

/**
 * The log viewer's complete *bounded* screen state: one immutable value produced by
 * `LogViewerViewModel` and rendered by `LogViewerScreen`.
 *
 * Bounded is the defining property. The result rows are not here and never will be — they travel
 * separately as `Flow<PagingData<LogViewerListItem>>`, because Paging owns and evicts its own
 * working set and a state value cannot. What is left is small and fixed in size regardless of how
 * many logs the snapshot holds: the query inputs, the filter sheet's draft, the snapshot metadata
 * its controls are built from, the active query's aggregate, the selected log, and how the launch
 * refresh went.
 *
 * The screen-wide inputs ([query], [filters], [sortOrder], [selectedLog]) sit beside — not inside —
 * [refresh], so a query survives a retry and the details sheet is simply "[selectedLog] is not null"
 * rather than a separate visibility flag.
 *
 * [query], [filters], and [sortOrder] are also the query inputs: together they are what `toLogQuery`
 * turns into the one `LogQuery` the paged rows and [summary] are both read with.
 */
@Immutable
internal data class LogViewerUiState(
    val query: String = "",
    /**
     * Whether the app bar's search field is showing, which is presentation only.
     *
     * It sits beside [query] rather than replacing it: the text is the query input and outlives the
     * field it was typed into, so collapsing search hides the control without withdrawing the
     * search — the app bar's search action keeps a small indicator to say so. Opening or closing it
     * therefore changes nothing `toLogQuery` reads.
     */
    val isSearchExpanded: Boolean = false,
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
    val refresh: LogViewerRefreshState = LogViewerRefreshState.InProgress,
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
 * How the once-per-launch snapshot refresh went, which is what decides whether the stored snapshot
 * may be presented as current.
 *
 * It is deliberately *not* the list's load state. Paging reports its own refresh, append, and retry
 * states for the rows it is loading out of Room; this reports whether Room's contents are the
 * latest remote snapshot at all. The two are independent: a completed refresh can be followed by a
 * Paging append failure, and a failed refresh still leaves a queryable previous snapshot behind.
 */
@Immutable
internal sealed interface LogViewerRefreshState {
    /** The launch refresh is running. The screen shows skeletons rather than possibly stale rows. */
    data object InProgress : LogViewerRefreshState

    /** The refresh replaced the snapshot: what the list queries is the latest remote content. */
    data object Complete : LogViewerRefreshState

    /**
     * The refresh failed, so the previous snapshot — if any — was kept but is not current.
     *
     * [dismissed] records that the user closed the dialog without retrying. The failure itself is
     * not forgotten, because nothing about it stopped being true; only the modal goes away, leaving
     * the retained snapshot readable underneath.
     */
    data class Failed(
        val dismissed: Boolean = false,
    ) : LogViewerRefreshState
}

/** The retryable failure dialog is up exactly while an unacknowledged refresh failure stands. */
internal val LogViewerUiState.showsRefreshFailure: Boolean
    get() = refresh is LogViewerRefreshState.Failed && !refresh.dismissed

/**
 * A dismissed failure still has to be visible somewhere, which is what the stale-snapshot notice is
 * for.
 *
 * Without it, dismissing the dialog would be a dead end: retry lives only on that dialog, and the
 * rows left underneath would silently read as the current snapshot — the one thing a failed refresh
 * must never be allowed to look like.
 */
internal val LogViewerUiState.showsStaleSnapshotNotice: Boolean
    get() = refresh is LogViewerRefreshState.Failed && refresh.dismissed

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

/** The complete filtered result is empty, and counted — not merely uncounted so far. */
internal val LogViewerUiState.hasNoMatches: Boolean
    get() = summary is LogViewerSummaryState.Ready && summary.summary.totalLogCount == 0
