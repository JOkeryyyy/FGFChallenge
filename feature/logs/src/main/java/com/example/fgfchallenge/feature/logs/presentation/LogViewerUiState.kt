package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi

/**
 * The log viewer's mutually exclusive screen state.
 *
 * This milestone renders the state from fixtures; Roadmap #4 adds the ViewModel that produces it
 * from user actions and repository results. Every string here is already display-ready, so the
 * screen formats nothing during composition.
 *
 * A filtered result with no matches is [Content] with an empty item list — not [Error] — because
 * the search still succeeded.
 */
internal sealed interface LogViewerUiState {
    /** Initial load in flight: skeleton placeholders only, no fabricated log values. */
    data object Loading : LogViewerUiState

    /** Retryable load failure, presented as a modal dialog over the shell. */
    data class Error(
        val title: String,
        val message: String,
    ) : LogViewerUiState

    /**
     * A rendered result set. [items] is the flattened header/row list; it is empty when the active
     * [query] matches nothing, in which case [severitySummary] reports zero counts.
     */
    @Immutable
    data class Content(
        val query: String,
        val resultCountLabel: String,
        val sortLabel: String,
        val severitySummary: SeveritySummaryUi,
        val items: List<LogViewerListItem>,
    ) : LogViewerUiState
}
