package com.example.fgfchallenge.feature.logs.presentation

/**
 * Every user input the log viewer accepts, dispatched through the screen's single `onAction`
 * callback.
 *
 * Each entry names what the user did rather than the state change it causes, so the ViewModel stays
 * the only place that decides what an interaction means. Selection travels as a log ID because the
 * ViewModel owns the row data the details are resolved from.
 */
internal sealed interface LogViewerAction {
    /** Search text changed, including the search field's clear button emitting an empty query. */
    data class QueryChanged(
        val query: String,
    ) : LogViewerAction

    /** The sort control was tapped; the ViewModel decides which order follows the current one. */
    data object SortOrderToggled : LogViewerAction

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
