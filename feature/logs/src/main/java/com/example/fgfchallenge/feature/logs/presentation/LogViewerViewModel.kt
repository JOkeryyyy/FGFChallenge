package com.example.fgfchallenge.feature.logs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.repository.LogsRepository
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.toSeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.toggleSeverity
import com.example.fgfchallenge.feature.logs.presentation.model.toggleTag
import com.example.fgfchallenge.feature.logs.presentation.model.utcDateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * The log viewer's screen-level state producer: it owns the single immutable
 * [StateFlow]`<`[LogViewerUiState]`>` the screen renders, the separate paged-row stream Paging
 * drives, and is the only place a [LogViewerAction] turns into a state change.
 *
 * It is also the whole of the app's query coordination. The screen's inputs become one canonical
 * [LogQuery] through [toLogQuery], and that single value is handed to both repository reads, so the
 * rows and the counts describe the same criteria by construction rather than by agreement between
 * two call sites. There is no domain layer, use case, or repository wrapper between them: this
 * one-screen prototype has nothing to reuse them for.
 *
 * Two streams leave this class rather than one, and that is deliberate. [state] holds only bounded
 * screen state; [pagedLogs] carries the rows, because Paging owns and evicts its own working set
 * and putting `PagingData` in an immutable state value would defeat that.
 *
 * It also owns the filter sheet's *draft*, not just the applied filters. The sheet is a stateless
 * design-system component that reports chip taps and picker results and re-renders whatever it is
 * handed back, so a half-composed filter set lives in one place, survives configuration changes with
 * the rest of the state, and reaches the query only when Apply says so.
 *
 * The result *list* is still the Roadmap #3 fixture: [pagedLogs] and [LogViewerUiState.summary] are
 * live repository reads, but nothing renders them until Roadmap #10 replaces the fixture content
 * and maps entries into display-ready list items. Roadmap #10 also owns startup refresh, so no
 * launch refresh is triggered here yet.
 */
@HiltViewModel
internal class LogViewerViewModel
    @Inject
    constructor(
        private val repository: LogsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(defaultState())

        val state: StateFlow<LogViewerUiState> = _state.asStateFlow()

        /**
         * The active criteria, derived from state and de-duplicated.
         *
         * [distinctUntilChanged] is what keeps selecting a row, editing the filter draft, or
         * dismissing a sheet from restarting the database work: those change [state] but not the
         * query. It also means a search that normalizes to the same value — trailing whitespace,
         * say — re-uses the running queries instead of discarding their results.
         */
        private val activeQuery: Flow<LogQuery> =
            _state
                .map { it.activeLogQuery() }
                .distinctUntilChanged()

        /**
         * The rows for the active query.
         *
         * `flatMapLatest` is the latest-generation mechanism: a new query cancels the previous
         * Pager's collection, so rows from an obsolete query can never arrive after the criteria
         * change. `cachedIn` keeps the loaded pages across configuration changes and makes the
         * stream shareable, which a `PagingData` flow otherwise is not.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedLogs: Flow<PagingData<LogEntry>> =
            activeQuery
                .flatMapLatest { query -> repository.pagedLogs(query) }
                .cachedIn(viewModelScope)

        init {
            observeSummary()
            observeFilterOptions()
        }

        fun onAction(action: LogViewerAction) {
            when (action) {
                // Search text is reflected immediately, per ARCHITECTURE.md. Replacing the queries
                // on every keystroke is what `flatMapLatest` above is for: the obsolete work is
                // cancelled rather than allowed to finish and overwrite the newer result.
                is LogViewerAction.QueryChanged -> {
                    updateQueryInputs { it.copy(query = action.query) }
                }

                LogViewerAction.SortOrderToggled -> {
                    updateQueryInputs { it.copy(sortOrder = it.sortOrder.toggled()) }
                }

                is LogViewerAction.LogSelected -> {
                    selectLog(action.logId)
                }

                LogViewerAction.DetailsDismissed -> {
                    _state.update { it.copy(selectedLog = null) }
                }

                // Opening starts the draft from what is currently applied, so the sheet always
                // shows the filters the visible rows were produced with.
                LogViewerAction.FilterSheetOpened -> {
                    _state.update { it.copy(filterDraft = it.filters) }
                }

                // Dismissing without applying discards the edit; the applied filters are untouched,
                // so nothing the user was reading changes underneath them.
                LogViewerAction.FilterSheetDismissed -> {
                    _state.update { it.copy(filterDraft = null) }
                }

                is LogViewerAction.FilterTagToggled -> {
                    updateFilterDraft { it.toggleTag(action.tag) }
                }

                is LogViewerAction.FilterSeverityToggled -> {
                    updateFilterDraft { it.toggleSeverity(action.severity) }
                }

                is LogViewerAction.FilterAiGeneratedChanged -> {
                    updateFilterDraft { it.copy(aiGenerated = action.choice) }
                }

                // The pickers report UTC epoch milliseconds; turning them into calendar dates is a
                // UTC rule, so it happens here rather than in the sheet.
                is LogViewerAction.FilterDateRangeChanged -> {
                    updateFilterDraft {
                        it.copy(
                            startDateUtc = action.startUtcMillis?.let(::utcDateOf),
                            endDateUtc = action.endUtcMillis?.let(::utcDateOf),
                        )
                    }
                }

                is LogViewerAction.FilterStartTimeChanged -> {
                    updateFilterDraft {
                        it.copy(startTimeUtc = LocalTime.of(action.hourOfDayUtc, action.minuteOfHourUtc))
                    }
                }

                is LogViewerAction.FilterEndTimeChanged -> {
                    updateFilterDraft {
                        it.copy(endTimeUtc = LocalTime.of(action.hourOfDayUtc, action.minuteOfHourUtc))
                    }
                }

                is LogViewerAction.FilterLatencyRangeChanged -> {
                    updateFilterDraft {
                        it.copy(minimumLatencyMs = action.minimumMs, maximumLatencyMs = action.maximumMs)
                    }
                }

                // The one filter action that reaches the query. It goes through `updateQueryInputs`
                // like every other criteria change, so applying a filter drops a summary counted for
                // the previous criteria in the same state value.
                LogViewerAction.FiltersApplied -> {
                    updateQueryInputs { current ->
                        val draft = current.filterDraft ?: return@updateQueryInputs current
                        current.copy(filters = draft, filterDraft = null)
                    }
                }

                // Clear All resets the draft and leaves the sheet open: it is an edit like any
                // other, and the roadmap's rule is that nothing queries the database until Apply.
                LogViewerAction.FiltersCleared -> {
                    updateFilterDraft { LogFilterSelection() }
                }

                // Both paths leave the error behind, and the fixture is the only result set this
                // milestone can return to. Roadmap #10 replaces this with a startup refresh, where
                // Retry and Dismiss stop being equivalent. The summary is carried in from the
                // current state because it belongs to the active query, not to the fixture content
                // — and is then dropped by `updateQueryInputs` if resetting changed the query. The
                // filter options come across for a different reason: they describe the stored
                // snapshot rather than anything the user typed, so recovering the screen must not
                // empty the sheet's controls until Room next emits.
                LogViewerAction.RetryClicked, LogViewerAction.ErrorDismissed -> {
                    updateQueryInputs {
                        defaultState().copy(summary = it.summary, filterOptions = it.filterOptions)
                    }
                }
            }
        }

        /**
         * Edits the open draft, and does nothing when no sheet is open.
         *
         * The null check is not defensive padding: a control can report a change as the sheet is
         * being dismissed, and reopening a sheet the user just closed — or resurrecting a draft they
         * discarded — is worse than dropping that last edit. Nothing here touches
         * [LogViewerUiState.filters], which is what keeps a half-composed filter set away from the
         * database.
         */
        private fun updateFilterDraft(transform: (LogFilterSelection) -> LogFilterSelection) {
            _state.update { current ->
                val draft = current.filterDraft ?: return@update current
                current.copy(filterDraft = transform(draft))
            }
        }

        /**
         * Applies a change to the query inputs and, if it produced different criteria, drops the
         * summary in the *same* state value.
         *
         * Doing it here rather than when the new aggregate is subscribed to is what makes the
         * guarantee structural: no published [LogViewerUiState] ever pairs new criteria with a
         * total counted for the old ones, not even for the frame between the two writes. Routing
         * every query input through this function is also what keeps the rule from having to be
         * remembered again for each filter control Roadmap #11 adds.
         */
        private fun updateQueryInputs(transform: (LogViewerUiState) -> LogViewerUiState) {
            _state.update { current ->
                val next = transform(current)
                if (next.activeLogQuery() == current.activeLogQuery()) {
                    next
                } else {
                    next.copy(summary = LogViewerSummaryState.Pending)
                }
            }
        }

        /**
         * Keeps [LogViewerUiState.summary] describing the query that is currently active.
         *
         * `collectLatest` cancels the superseded aggregate collection, the same generation rule
         * [pagedLogs] follows. The equality check is the belt to that braces: an emission that
         * arrives from a collection which has not been cancelled *yet* is discarded rather than
         * published, so the summary can only ever describe the criteria the state currently holds.
         */
        private fun observeSummary() {
            viewModelScope.launch {
                activeQuery.collectLatest { query ->
                    repository.summary(query).collect { summary ->
                        _state.update { current ->
                            if (current.activeLogQuery() != query) {
                                current
                            } else {
                                current.copy(summary = LogViewerSummaryState.Ready(summary.toSeveritySummaryUi()))
                            }
                        }
                    }
                }
            }
        }

        /**
         * Keeps the filter sheet's controls describing the stored snapshot.
         *
         * Options are snapshot metadata rather than query results — the tags that exist and the
         * latency extent, both unfiltered — so this collection is independent of the active query
         * and simply follows Room. A refresh that replaces the snapshot re-emits here, and a latency
         * bound the new extent no longer restricts is collapsed by `activeLogQuery`, not by the
         * slider that produced it.
         */
        private fun observeFilterOptions() {
            viewModelScope.launch {
                repository.filterOptions().collect { options ->
                    updateQueryInputs { it.copy(filterOptions = options) }
                }
            }
        }

        /**
         * A selection for an ID that is not in the current result set is ignored rather than
         * clearing the sheet, so a stale tap arriving after the list changed cannot dismiss a sheet
         * the user is reading.
         *
         * Roadmap #10 replaces this scan of the fixture with `LogsRepository.logById`, so a row on
         * a page Paging has since evicted stays selectable.
         */
        private fun selectLog(logId: String) {
            val details = _state.value.detailsFor(logId) ?: return
            _state.update { it.copy(selectedLog = details) }
        }

        private companion object {
            /** Blank query, no filters, newest first, nothing selected, and the fixture result set. */
            fun defaultState(): LogViewerUiState = LogViewerFixtures.allLogsState()

            fun LogViewerUiState.detailsFor(logId: String): LogDetailsUi? =
                (loadState as? LogViewerLoadState.Content)
                    ?.items
                    ?.filterIsInstance<LogViewerListItem.LogRow>()
                    ?.firstOrNull { it.row.id == logId }
                    ?.details
        }
    }
