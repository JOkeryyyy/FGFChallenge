package com.example.fgfchallenge.feature.logs.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.aggregateCriteria
import com.example.fgfchallenge.feature.logs.data.repository.LogsRepository
import com.example.fgfchallenge.feature.logs.presentation.activeLogQuery
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.minuteHeaderBetween
import com.example.fgfchallenge.feature.logs.presentation.model.toListItem
import com.example.fgfchallenge.feature.logs.presentation.model.toLogDetailsUi
import com.example.fgfchallenge.feature.logs.presentation.model.toSeveritySummaryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The log viewer's screen-level state producer: it owns the single immutable
 * [kotlinx.coroutines.flow.StateFlow]`<`[LogViewerUiState]`>` the screen renders, the separate paged-row stream Paging
 * drives, and is the only place a [LogViewerAction] turns into a state change.
 *
 * It is also the whole of the app's query coordination. The screen's inputs become one canonical
 * [com.example.fgfchallenge.feature.logs.data.model.LogQuery] through [com.example.fgfchallenge.feature.logs.presentation.toLogQuery], and that single value is handed to both repository reads, so the
 * rows and the counts describe the same criteria by construction rather than by agreement between
 * two call sites. There is no domain layer, use case, or repository wrapper between them: this
 * one-screen prototype has nothing to reuse them for.
 *
 * Two streams leave this class rather than one, and that is deliberate. [state] holds only bounded
 * screen state; [pagedLogs] carries the rows, because Paging owns and evicts its own working set
 * and putting `PagingData` in an immutable state value would defeat that. Nothing here is sized by
 * the snapshot or by the number of matches — not the state, not the details lookup, not the
 * transformation that groups rows into minutes.
 *
 * It owns the *applied* filters and not the filter sheet's draft. The draft belongs to
 * `LogFilterSheetHost`, because holding it here published a new [LogViewerUiState] for every chip tap
 * and every frame of a latency drag — recomposing the screen and its row list for a value neither
 * renders. What arrives here instead is one [LogViewerAction.FiltersApplied] carrying the finished
 * selection, which is also the only point a filter set is allowed to reach the query.
 */
@HiltViewModel
internal class LogViewerViewModel
    @Inject
    constructor(
        private val repository: LogsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LogViewerUiState())

        val state: StateFlow<LogViewerUiState> = _state.asStateFlow()

        /**
         * The search text the database is allowed to see: the typed text, settled.
         *
         * The field itself stays search-as-you-type — [LogViewerUiState.query] is updated on the
         * keystroke and the text renders immediately — but each keystroke would otherwise be a new
         * [com.example.fgfchallenge.feature.logs.data.model.LogQuery], and every new query starts *two* pieces of database work: a fresh Pager and a
         * fresh full-result aggregate over the whole filtered set. Typing a seven-character word
         * that way costs seven of each, six of them cancelled before they are read. Waiting for the
         * typing to stop collapses that to one.
         *
         * Blank text is committed immediately instead of waited on, because returning to the
         * unfiltered list is the end of a search rather than a step in composing one — clearing the
         * field should not sit for [SEARCH_DEBOUNCE_MILLIS] showing results the user just dismissed.
         */
        @OptIn(FlowPreview::class)
        private val settledSearchText: Flow<String> =
            _state
                .map { it.query }
                .distinctUntilChanged()
                .debounce { text -> if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MILLIS }

        /**
         * The active criteria, derived from state and de-duplicated.
         *
         * [distinctUntilChanged] is what keeps selecting a row, opening the filter sheet, or
         * dismissing one from restarting the database work: those change [state] but not the query.
         * It is also what makes re-applying an unchanged filter selection free, and means a search
         * that normalizes to the same value — trailing whitespace, say — re-uses the running queries
         * instead of discarding their results.
         *
         * The settled search text is substituted back into the state *before* the query is derived,
         * rather than copied over the derived query, so [activeLogQuery] stays the single place that
         * normalizes any of these inputs — a rule added there applies to the debounced text too.
         *
         * Only the text is delayed. A filter Apply, a Clear All that follows it, and a sort toggle
         * are deliberate commits rather than characters on their way to one, so they pass through
         * this flow at once; combining them with the settled text is also what keeps a keystroke
         * from emitting at all until it settles, since the query it produces still holds the
         * previous text and is dropped as a duplicate.
         */
        private val activeQuery: Flow<LogQuery> =
            combine(_state, settledSearchText) { state, searchText ->
                state.copy(query = searchText).activeLogQuery()
            }.distinctUntilChanged()

        /**
         * The criteria the full-result aggregate is counted for, which is [activeQuery] without its
         * direction.
         *
         * A sort toggle changes which rows come first; it cannot change how many of each severity
         * match. Keying the aggregate on the whole query made every toggle restart a count whose
         * answer was already known — two full scans of the snapshot on the search path — and drop
         * the displayed total to "counting" while they ran.
         */
        private val activeAggregateCriteria: Flow<LogQuery> =
            activeQuery.map(LogQuery::aggregateCriteria).distinctUntilChanged()

        /**
         * The list, as display-ready items grouped under UTC minute headers.
         *
         * `flatMapLatest` is the latest-generation mechanism: a new query cancels the previous
         * Pager's collection, so rows from an obsolete query can never arrive after the criteria
         * change.
         *
         * Both transformations run *before* `cachedIn`, which is what keeps them off the recomposing
         * thread and stops them from re-running for every collector: a row is formatted once, when
         * its page loads. `cachedIn` also keeps the loaded pages across configuration changes and
         * makes the stream shareable, which a `PagingData` flow otherwise is not.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedLogs: Flow<PagingData<LogViewerListItem>> =
            activeQuery
                .flatMapLatest { query -> repository.pagedLogs(query) }
                .map { pagingData ->
                    pagingData
                        .map(LogEntry::toListItem)
                        .insertSeparators(generator = ::minuteHeaderBetween)
                }.cachedIn(viewModelScope)

        /** Cancelled and replaced by a retry, so two refreshes can never resolve out of order. */
        private var refreshJob: Job? = null

        /**
         * Cancelled by the next selection and by dismissal, so a lookup that is still in flight
         * cannot open a sheet for a row the user has moved on from — or reopen one they just closed.
         */
        private var selectionJob: Job? = null

        init {
            observeSummary()
            observeFilterOptions()
            // One complete refresh per launch. The ViewModel outlives configuration changes, so
            // this runs once for the screen rather than once per composition.
            refreshSnapshot()
        }

        fun onAction(action: LogViewerAction) {
            when (action) {
                // Search text is reflected immediately, per ARCHITECTURE.md. Replacing the queries
                // on every keystroke is what `flatMapLatest` above is for: the obsolete work is
                // cancelled rather than allowed to finish and overwrite the newer result.
                is LogViewerAction.QueryChanged -> {
                    updateQueryInputs { it.copy(query = action.query) }
                }

                // Visibility only, and deliberately not routed through `updateQueryInputs`: showing
                // or hiding the field changes nothing `toLogQuery` reads, so the running Pager and
                // aggregate keep their results and the summary is not dropped to Pending. The text
                // survives a dismissal because it is the user's search, not the control's state.
                LogViewerAction.SearchOpened -> {
                    _state.update { it.copy(isSearchExpanded = true) }
                }

                LogViewerAction.SearchDismissed -> {
                    _state.update { it.copy(isSearchExpanded = false) }
                }

                LogViewerAction.SortOrderToggled -> {
                    updateQueryInputs { it.copy(sortOrder = it.sortOrder.toggled()) }
                }

                is LogViewerAction.LogSelected -> {
                    selectLog(action.logId)
                }

                LogViewerAction.DetailsDismissed -> {
                    selectionJob?.cancel()
                    _state.update { it.copy(selectedLog = null) }
                }

                // Visibility only. The sheet seeds its own edit from the applied filters when it
                // opens, so the filters the visible rows were produced with are still what it shows.
                LogViewerAction.FilterSheetOpened -> {
                    _state.update { it.copy(isFilterSheetOpen = true) }
                }

                // Dismissing without applying discards the edit; the applied filters are untouched,
                // so nothing the user was reading changes underneath them.
                LogViewerAction.FilterSheetDismissed -> {
                    _state.update { it.copy(isFilterSheetOpen = false) }
                }

                // The one filter action that reaches the query. It goes through `updateQueryInputs`
                // like every other criteria change, so applying a filter drops a summary counted for
                // the previous criteria in the same state value. Re-applying an unchanged selection
                // is not special-cased here: it derives the same `LogQuery`, which `activeQuery`
                // drops as a duplicate, so nothing restarts.
                is LogViewerAction.FiltersApplied -> {
                    updateQueryInputs { it.copy(filters = action.selection, isFilterSheetOpen = false) }
                }

                // Retry re-runs the launch refresh, and only that. The query, filters, and sort are
                // the user's and survive it — a failed import says nothing about what they asked
                // for, and resetting their inputs would lose work the failure never invalidated.
                LogViewerAction.RetryClicked -> {
                    refreshSnapshot()
                }

                // Dismissing closes the modal without claiming the snapshot is current: the failure
                // stands, and what stays readable underneath is the previous snapshot Room kept.
                LogViewerAction.ErrorDismissed -> {
                    _state.update { current ->
                        if (current.refresh is LogViewerRefreshState.Failed) {
                            current.copy(refresh = LogViewerRefreshState.Failed(dismissed = true))
                        } else {
                            current
                        }
                    }
                }
            }
        }

        /**
         * Fetches the complete remote snapshot and replaces the stored one.
         *
         * The three outcomes are the three states, with nothing in between: while it runs the screen
         * shows skeletons instead of a snapshot that may be about to be replaced, and a failure is
         * reported as exactly that rather than as content. The repository has already guaranteed the
         * previous snapshot survives a failure, so nothing here has to undo anything.
         */
        private fun refreshSnapshot() {
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    _state.update { it.copy(refresh = LogViewerRefreshState.InProgress) }
                    val refreshState =
                        when (repository.refreshSnapshot()) {
                            is Result.Success -> LogViewerRefreshState.Complete
                            is Result.Error -> LogViewerRefreshState.Failed()
                        }
                    _state.update { it.copy(refresh = refreshState) }
                }
        }

        /**
         * Resolves the tapped row's details from the repository by ID.
         *
         * By ID rather than by scanning what the list currently holds: Paging evicts pages as the
         * user scrolls, so a row that is on screen is not necessarily a row whose data is still in
         * memory, and a lookup that depended on that would fail exactly where the paged list is
         * doing its job. An ID the snapshot no longer holds, or a failed read, leaves the current
         * selection untouched — a stale tap arriving after the list changed must not dismiss a
         * sheet the user is reading.
         */
        private fun selectLog(logId: String) {
            selectionJob?.cancel()
            selectionJob =
                viewModelScope.launch {
                    val entry = (repository.logById(logId) as? Result.Success)?.data ?: return@launch
                    _state.update { it.copy(selectedLog = entry.toLogDetailsUi()) }
                }
        }

        /**
         * Applies a change to the query inputs and, if it produced different *counting* criteria,
         * drops the summary in the *same* state value.
         *
         * Doing it here rather than when the new aggregate is subscribed to is what makes the
         * guarantee structural: no published [LogViewerUiState] ever pairs new criteria with a
         * total counted for the old ones, not even for the frame between the two writes. That
         * matters more than it sounds, because the paged rows change generation at the same moment
         * — a summary left standing would be describing a list that no longer exists.
         *
         * The comparison is [aggregateCriteria] rather than the whole query, and has to be: a sort
         * toggle produces a different query but the same counts, and [observeSummary] deliberately
         * does not restart for one. Comparing the whole query here would drop the total to
         * "counting" with nothing left running to ever replace it.
         */
        private fun updateQueryInputs(transform: (LogViewerUiState) -> LogViewerUiState) {
            _state.update { current ->
                val next = transform(current)
                if (next.activeLogQuery().aggregateCriteria() == current.activeLogQuery().aggregateCriteria()) {
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
                activeAggregateCriteria.collectLatest { criteria ->
                    repository.summary(criteria).collect { summary ->
                        _state.update { current ->
                            if (current.activeLogQuery().aggregateCriteria() != criteria) {
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

        private companion object {
            /**
             * How long the typed search text has to stand still before it reaches the database.
             *
             * Long enough that the characters of one word do not each start their own Pager and
             * aggregate, short enough that a user who stops typing does not notice having waited.
             */
            const val SEARCH_DEBOUNCE_MILLIS = 300L
        }
    }
