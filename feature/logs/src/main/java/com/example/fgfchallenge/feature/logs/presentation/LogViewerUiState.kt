package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi

/**
 * The log viewer's complete screen state: one immutable value produced by `LogViewerViewModel` and
 * rendered by `LogViewerScreen`.
 *
 * The screen-wide inputs ([query], [sortOrder], [selectedLog]) sit beside — not inside — the
 * mutually exclusive [loadState], so a query survives a reload and the details sheet is simply
 * "[selectedLog] is not null" rather than a separate visibility flag.
 */
@Immutable
internal data class LogViewerUiState(
    val query: String = "",
    val sortOrder: LogSortOrder = LogSortOrder.NewestFirst,
    val selectedLog: LogDetailsUi? = null,
    val loadState: LogViewerLoadState = LogViewerLoadState.Loading,
)

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
