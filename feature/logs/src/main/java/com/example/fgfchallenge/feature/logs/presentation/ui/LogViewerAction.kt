package com.example.fgfchallenge.feature.logs.presentation.ui

import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection

/**
 * Every user input the log viewer accepts, dispatched through the screen's single `onAction`
 * callback.
 *
 * Each entry names what the user did rather than the state change it causes, so the ViewModel stays
 * the only place that decides what an interaction means. Selection travels as a log ID because the
 * ViewModel owns the row data the details are resolved from.
 *
 * The filter entries are the clearest case of that split, and of its limit. Opening and dismissing
 * the sheet are reported here because they are screen-level visibility; the edits inside it are not,
 * because nothing outside the sheet renders a half-composed filter set. Only [FiltersApplied]
 * reaches the query.
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

    /** The Filter control was tapped, which shows the sheet over the current rows. */
    data object FilterSheetOpened : LogViewerAction

    /** The filter sheet was dismissed by swipe, Back, or an outside tap, discarding the edit. */
    data object FilterSheetDismissed : LogViewerAction

    /**
     * Apply: the sheet's finished selection becomes the applied filters, and only now does the query
     * change.
     *
     * It carries the whole selection rather than reporting each chip and picker, because the edit
     * itself belongs to `LogFilterSheetHost` — the individual taps never reach the ViewModel, which
     * is what keeps a filter session from recomposing the screen once per interaction. Clear All is
     * likewise absent: it resets the sheet's own edit and commits nothing.
     */
    data class FiltersApplied(
        val selection: LogFilterSelection,
    ) : LogViewerAction

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
