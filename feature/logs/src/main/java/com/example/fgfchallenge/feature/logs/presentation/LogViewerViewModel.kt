package com.example.fgfchallenge.feature.logs.presentation

import androidx.lifecycle.ViewModel
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The log viewer's screen-level state producer: it owns the single immutable
 * [StateFlow]`<`[LogViewerUiState]`>` the screen renders and is the only place a [LogViewerAction]
 * turns into a state change.
 *
 * The result set is still the Roadmap #3 fixture, so this milestone deliberately implements only
 * the interaction half of the loop. Query text and sort order are recorded but change no rows;
 * filtering, grouping, reordering, and recomputed severity counts arrive with the processing
 * pipeline in Roadmap #7, and repository-backed loading and retry with Roadmap #5. Every action is
 * handled synchronously, so no dispatcher is injected yet.
 */
@HiltViewModel
internal class LogViewerViewModel
    @Inject
    constructor() : ViewModel() {
        private val _state = MutableStateFlow(LogViewerUiState())

        val state: StateFlow<LogViewerUiState> = _state.asStateFlow()

        fun onAction(action: LogViewerAction) {
            when (action) {
                // Search text is reflected immediately, per ARCHITECTURE.md; only the (not yet
                // implemented) result processing is debounced.
                is LogViewerAction.QueryChanged -> _state.update { it.copy(query = action.query) }

                LogViewerAction.SortOrderToggled -> _state.update { it.copy(sortOrder = it.sortOrder.toggled()) }

                is LogViewerAction.LogSelected -> selectLog(action.logId)

                LogViewerAction.DetailsDismissed -> _state.update { it.copy(selectedLog = null) }

                // Both paths leave the error behind, and the fixture is the only result set this
                // milestone can return to. Roadmap #5 replaces this with a repository re-fetch,
                // where Retry and Dismiss stop being equivalent.
                LogViewerAction.RetryClicked, LogViewerAction.ErrorDismissed -> _state.value = defaultState()
            }
        }

        /**
         * A selection for an ID that is not in the current result set is ignored rather than
         * clearing the sheet, so a stale tap arriving after the list changed cannot dismiss a sheet
         * the user is reading.
         */
        private fun selectLog(logId: String) {
            val details = _state.value.detailsFor(logId) ?: return
            _state.update { it.copy(selectedLog = details) }
        }

        private companion object {
            /** Blank query, newest first, nothing selected, and the full fixture result set. */
            fun defaultState(): LogViewerUiState = LogViewerFixtures.allLogsState()

            fun LogViewerUiState.detailsFor(logId: String): LogDetailsUi? =
                (loadState as? LogViewerLoadState.Content)
                    ?.items
                    ?.filterIsInstance<LogViewerListItem.LogRow>()
                    ?.firstOrNull { it.row.id == logId }
                    ?.details
        }
    }
