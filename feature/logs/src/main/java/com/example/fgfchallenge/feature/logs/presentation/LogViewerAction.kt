package com.example.fgfchallenge.feature.logs.presentation

import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter

/**
 * Every user input the log viewer accepts, dispatched through the screen's single `onAction`
 * callback.
 *
 * Each entry names what the user did rather than the state change it causes, so the ViewModel stays
 * the only place that decides what an interaction means. Selection travels as a log ID because the
 * ViewModel owns the row data the details are resolved from.
 *
 * The filter entries are the clearest case of that split: a chip tap reports the chip, not the
 * resulting selection, and none of them says whether the change reaches the query — only
 * [FiltersApplied] does that.
 */
internal sealed interface LogViewerAction {
    /** Search text changed, including the search field's clear button emitting an empty query. */
    data class QueryChanged(
        val query: String,
    ) : LogViewerAction

    /**
     * The app bar's search action was tapped while search was collapsed, which reveals the field.
     *
     * Visibility only. The text the field is opened onto is whatever [QueryChanged] last reported,
     * so this neither starts nor widens a search — it is why a collapsed search can still be active.
     */
    data object SearchOpened : LogViewerAction

    /** The same action tapped while search was expanded, which hides the field but keeps its text. */
    data object SearchDismissed : LogViewerAction

    /** The sort control was tapped; the ViewModel decides which order follows the current one. */
    data object SortOrderToggled : LogViewerAction

    /** The Filter control was tapped, which starts a draft from the filters currently applied. */
    data object FilterSheetOpened : LogViewerAction

    /** The filter sheet was dismissed by swipe, Back, or an outside tap, discarding the draft. */
    data object FilterSheetDismissed : LogViewerAction

    /** A tag chip was tapped; the ViewModel decides whether that selects or deselects it. */
    data class FilterTagToggled(
        val tag: String,
    ) : LogViewerAction

    data class FilterSeverityToggled(
        val severity: Severity,
    ) : LogViewerAction

    data class FilterAiGeneratedChanged(
        val choice: AiGeneratedFilter,
    ) : LogViewerAction

    /**
     * The date-range picker was confirmed. The bounds stay UTC epoch milliseconds this far because
     * the conversion into calendar dates is a UTC rule, and those live with the query policy rather
     * than in a composable. Either bound may be `null`, which leaves that side of the range open.
     */
    data class FilterDateRangeChanged(
        val startUtcMillis: Long?,
        val endUtcMillis: Long?,
    ) : LogViewerAction

    data class FilterStartTimeChanged(
        val hourOfDayUtc: Int,
        val minuteOfHourUtc: Int,
    ) : LogViewerAction

    data class FilterEndTimeChanged(
        val hourOfDayUtc: Int,
        val minuteOfHourUtc: Int,
    ) : LogViewerAction

    /** Both latency bounds are inclusive, and both arrive together from the one range control. */
    data class FilterLatencyRangeChanged(
        val minimumMs: Long,
        val maximumMs: Long,
    ) : LogViewerAction

    /** Apply: the draft becomes the applied filters, and only now does the query change. */
    data object FiltersApplied : LogViewerAction

    /** Clear All: the draft returns to no filters, still uncommitted until Apply. */
    data object FiltersCleared : LogViewerAction

    /** Retry on the error dialog. */
    data object RetryClicked : LogViewerAction

    /** The error dialog was dismissed without retrying (Dismiss, Back, or an outside tap). */
    data object ErrorDismissed : LogViewerAction

    /** A log row was tapped, which opens the details sheet for that row. */
    data class LogSelected(
        val logId: String,
    ) : LogViewerAction

    /** The details sheet was dismissed by its close button, a swipe down, or Back. */
    data object DetailsDismissed : LogViewerAction
}
