package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.fgfchallenge.core.designsystem.component.ErrorDialog
import com.example.fgfchallenge.core.designsystem.component.LoadingContent
import com.example.fgfchallenge.core.designsystem.component.LogDetailsSheet
import com.example.fgfchallenge.core.designsystem.component.LogFilterSheet
import com.example.fgfchallenge.core.designsystem.component.LogMinuteHeader
import com.example.fgfchallenge.core.designsystem.component.LogRow
import com.example.fgfchallenge.core.designsystem.component.LogSearchField
import com.example.fgfchallenge.core.designsystem.component.NoResultsContent
import com.example.fgfchallenge.core.designsystem.component.SeverityIndicator
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetEvent
import com.example.fgfchallenge.core.designsystem.modifier.shimmerEffect
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.severityFilterFor
import com.example.fgfchallenge.feature.logs.presentation.model.toDensityUi
import com.example.fgfchallenge.feature.logs.presentation.model.toFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.toFilterSheetUi
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Held rather than rebuilt per call: the title formats two counts on every recomposition, and the
 * bar recomposes on every paged update because it reads the loaded-row count. `getIntegerInstance`
 * clones a cached prototype, so calling it there allocated a `DecimalFormat` per number rendered.
 *
 * `NumberFormat` is not thread-safe; this one is only ever touched from composition, on the main
 * thread. The equivalent formatters in `LogEntryUiMapper` are held the same way.
 */
private val COUNT_FORMAT: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

private val SortIconSize = 18.dp
private val SummaryRingPlaceholderSize = 72.dp
private val SummaryTextPlaceholderHeight = 16.dp

// The list's own items share the paged rows' key namespace, so each gets its own prefix.
private const val NO_RESULTS_KEY = "empty:log-viewer-no-results"
private const val NO_RESULTS_CONTENT_TYPE = "log-viewer-no-results"
private const val APPEND_KEY = "append:log-viewer-append-state"
private const val APPEND_CONTENT_TYPE = "log-viewer-append-state"
private const val REFRESH_ERROR_KEY = "refresh:log-viewer-refresh-error"
private const val REFRESH_ERROR_CONTENT_TYPE = "log-viewer-refresh-error"

// The four anchors the out-of-process Macrobenchmark suite drives. They are resource-style ids
// rather than accessibility labels because UI Automator has to find them by a value that does not
// change with the query, the locale, or the count in the title.
// See `documentation/performanceBenchmark.md`.
private const val SEARCH_TEST_TAG = "log_viewer_search"
private const val FILTER_TEST_TAG = "log_viewer_filter"
private const val RESULT_TEST_TAG = "log_viewer_result"
private const val LIST_TEST_TAG = "log_viewer_list"

/**
 * Removes text focus for a tap no child has claimed, allowing the system keyboard to dismiss.
 *
 * This stays at the screen boundary: a reusable search field cannot know which parts of a feature
 * screen count as an outside tap. `detectTapGestures` ignores gestures a child consumes, so a row,
 * button, text field, or list scroll retains its existing handling.
 */
private fun Modifier.clearFocusOnUnconsumedTap(focusManager: FocusManager): Modifier =
    pointerInput(focusManager) {
        detectTapGestures {
            focusManager.clearFocus()
        }
    }

/**
 * The log viewer screen: it arranges the components exported by `:core:designsystem`, resolves the
 * localized copy its state stores as typed values, and reports every feature interaction through
 * one [onAction] callback. It holds no application state of its own, so every state is previewable
 * and directly snapshot-testable. Compose-owned keyboard focus is handled locally at the screen
 * boundary.
 *
 * Its input arrives as two values because the ViewModel produces two: [state] is the bounded screen
 * state, [logs] the paged rows. The screen never merges them into one list — the app bar's title
 * reports both counts and says which is which, taking the total from the aggregate in [state] and
 * the loaded figure from [logs], and neither is ever computed from the other's source.
 *
 * [logs] also carries Paging's own load states, which are deliberately *not* mirrored into [state]:
 * they belong to the list's loading, they change as the user scrolls, and Paging's own `retry()` is
 * what resolves them. The launch refresh in [LogViewerUiState.refresh] is the separate question of
 * whether the stored snapshot is current at all.
 *
 * Neither sheet is a branch of the layout: each renders whenever its state is set, over whatever
 * the body is showing.
 */
@Composable
internal fun LogViewerScreen(
    state: LogViewerUiState,
    logs: LazyPagingItems<LogViewerListItem>,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        // Publishes every `testTag` below as an Android resource id, which is the only way an
        // out-of-process tool can address a Compose node. It adds semantics and consumes no input.
        modifier =
            modifier
                .fillMaxSize()
                .clearFocusOnUnconsumedTap(focusManager)
                .semantics { testTagsAsResourceId = true },
        containerColor = MaterialTheme.colorScheme.background,
        // Scaffold subtracts what the bar already consumed, so the top inset is handled once by
        // TopAppBar and this only adds the cutout, IME, and navigation-bar sides back to the body.
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            LogViewerTopBar(
                summary = state.summary,
                loadedRowCount = logs.loadedRowCount(),
                activeFilterCount = state.activeFilterCount,
                sortOrder = state.sortOrder,
                query = state.query,
                isSearchExpanded = state.isSearchExpanded,
                onAction = onAction,
            )
        },
    ) { contentPadding ->
        // One centered pane: it grows with the window up to Dimens.contentMaxWidth and then stops,
        // so log rows never stretch into unscannable full-width lines on a large screen.
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pane = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxSize().padding(top = Spacing.sm)
            if (state.refresh is LogViewerRefreshState.InProgress) {
                // Skeletons rather than the stored rows: until the refresh resolves, nothing known
                // about the snapshot is worth presenting as the current result.
                LoadingContent(modifier = pane)
            } else {
                LogViewerContent(
                    state = state,
                    logs = logs,
                    onAction = onAction,
                    modifier = pane,
                )
            }
        }
    }
    // The failure is a modal over whatever snapshot Room kept, so dismissing it reveals the
    // retained rows without either action having claimed they are current.
    if (state.showsRefreshFailure) {
        ErrorDialog(
            title = stringResource(R.string.log_viewer_error_title),
            message = stringResource(R.string.log_viewer_error_message),
            onRetry = { onAction(LogViewerAction.RetryClicked) },
            onDismiss = { onAction(LogViewerAction.ErrorDismissed) },
        )
    }
    // Close, swipe-down, and Back all reach LogDetailsSheet's single onDismissRequest, so every
    // dismissal path reports the same action and the ViewModel needs no per-path handling.
    state.selectedLog?.let { details ->
        LogDetailsSheet(
            details = details,
            onDismissRequest = { onAction(LogViewerAction.DetailsDismissed) },
        )
    }
    // Same idiom for the filter sheet: it is open exactly while a draft exists, and every edit it
    // reports goes straight back out as an action. The screen translates between the design system's
    // vocabulary and the feature's, and decides nothing itself.
    state.filterDraft?.let { draft ->
        LogFilterSheet(
            filters = draft.toFilterSheetUi(state.filterOptions),
            onEvent = { event -> event.toAction()?.let(onAction) },
            onDismissRequest = { onAction(LogViewerAction.FilterSheetDismissed) },
        )
    }
}

/**
 * Maps one sheet event to the action it reports.
 *
 * `null` for a severity chip whose ID names no known severity — the sheet cannot produce one, and
 * inventing a filter from an unrecognized value would be worse than ignoring the tap. The latency
 * range crosses back into milliseconds here because the slider's `Float` domain is a control detail
 * and `LogViewerAction` speaks the feature's units.
 */
private fun LogFilterSheetEvent.toAction(): LogViewerAction? =
    when (this) {
        is LogFilterSheetEvent.TagToggled -> {
            LogViewerAction.FilterTagToggled(id)
        }

        is LogFilterSheetEvent.SeverityToggled -> {
            severityFilterFor(id)?.let(LogViewerAction::FilterSeverityToggled)
        }

        is LogFilterSheetEvent.AiGeneratedSelected -> {
            LogViewerAction.FilterAiGeneratedChanged(choice.toFilterSelection())
        }

        is LogFilterSheetEvent.DateRangeSelected -> {
            LogViewerAction.FilterDateRangeChanged(startUtcMillis, endUtcMillis)
        }

        is LogFilterSheetEvent.StartTimeSelected -> {
            LogViewerAction.FilterStartTimeChanged(hourOfDayUtc, minuteOfHourUtc)
        }

        is LogFilterSheetEvent.EndTimeSelected -> {
            LogViewerAction.FilterEndTimeChanged(hourOfDayUtc, minuteOfHourUtc)
        }

        is LogFilterSheetEvent.LatencyRangeSelected -> {
            LogViewerAction.FilterLatencyRangeChanged(
                minimumMs = range.start.roundToLong(),
                maximumMs = range.endInclusive.roundToLong(),
            )
        }

        LogFilterSheetEvent.Applied -> {
            LogViewerAction.FiltersApplied
        }

        LogFilterSheetEvent.Cleared -> {
            LogViewerAction.FiltersCleared
        }
    }

/**
 * The screen's one persistent control surface: what the query currently matches, and the three
 * controls that change it.
 *
 * A pinned app bar rather than a title above a row of buttons. Filter, search, and sort are the only
 * screen-level commands the viewer has, and as icons they occupy one bar height instead of three
 * stacked rows — which is height the list gets back, on a screen whose whole purpose is scanning
 * rows. Each control keeps a text label for a screen reader, since an icon carries none.
 *
 * The title is the two counts rather than a fixed product name: the numbers that describe the query
 * are worth more in the most prominent position than a name that never changes, and stating them
 * together — loaded of matching — is what keeps a paged list from reading as the whole result.
 *
 * The search *field* is the one control that is not an icon, because it takes typing. It expands
 * under the bar rather than replacing the title, so the count that says how the search is going
 * stays visible while it is being typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerTopBar(
    summary: LogViewerSummaryState,
    loadedRowCount: Int,
    activeFilterCount: Int,
    sortOrder: LogSortOrder,
    query: String,
    isSearchExpanded: Boolean,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One surface behind both rows, so the expanded field reads as part of the bar rather than as
    // the first item of the content below it.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = logCountTitle(summary = summary, loadedRowCount = loadedRowCount),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(RESULT_TEST_TAG),
                    )
                },
                navigationIcon = {
                    FilterAction(
                        activeFilterCount = activeFilterCount,
                        onClick = { onAction(LogViewerAction.FilterSheetOpened) },
                    )
                },
                actions = {
                    SearchAction(
                        isExpanded = isSearchExpanded,
                        hasQuery = query.isNotBlank(),
                        onClick = {
                            onAction(
                                if (isSearchExpanded) {
                                    LogViewerAction.SearchDismissed
                                } else {
                                    LogViewerAction.SearchOpened
                                },
                            )
                        },
                    )
                    SortAction(
                        sortOrder = sortOrder,
                        onClick = { onAction(LogViewerAction.SortOrderToggled) },
                    )
                },
            )
            if (isSearchExpanded) {
                ExpandedSearchField(
                    query = query,
                    onQueryChange = { onAction(LogViewerAction.QueryChanged(it)) },
                )
            }
        }
    }
}

/**
 * How many rows Paging holds of how many the query matches, as the bar's title.
 *
 * Both numbers or neither. The two are routinely orders of magnitude apart, so the loaded figure
 * alone would understate the result and the total alone would leave no sign that the list is paged —
 * and a loaded count paired with a total counted for *previous* criteria would be worse than either.
 * That last case is what [LogViewerSummaryState.Pending] exists to prevent, so an uncounted query
 * says so rather than showing a number for half the pair.
 */
@Composable
private fun logCountTitle(
    summary: LogViewerSummaryState,
    loadedRowCount: Int,
): String =
    when (summary) {
        LogViewerSummaryState.Pending -> {
            stringResource(R.string.log_viewer_title_counting)
        }

        is LogViewerSummaryState.Ready -> {
            stringResource(
                R.string.log_viewer_title_count,
                groupedCount(loadedRowCount),
                groupedCount(summary.summary.totalLogCount),
            )
        }
    }

/**
 * Opens the filter sheet, and states how many filter categories are currently narrowing the result.
 *
 * The count is a badge rather than only a highlight because the sheet's contents are out of sight
 * once it closes: without it, a result narrowed by an applied filter is indistinguishable from an
 * unfiltered one. Zero renders as no badge at all.
 *
 * The badge's number replaces the icon's own label rather than sitting beside it, because a screen
 * reader announcing "Filter" then "3" leaves the relationship between them to be guessed.
 */
@Composable
private fun FilterAction(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    val label =
        if (activeFilterCount > 0) {
            pluralStringResource(
                R.plurals.log_viewer_filter_action_active,
                activeFilterCount,
                activeFilterCount,
            )
        } else {
            stringResource(R.string.log_viewer_filter_action)
        }
    IconButton(onClick = onClick, modifier = Modifier.testTag(FILTER_TEST_TAG)) {
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(text = activeFilterCount.toString()) }
                }
            },
        ) {
            Icon(imageVector = Icons.Default.FilterList, contentDescription = label)
        }
    }
}

/**
 * Shows or hides the search field, and says whether a search is in effect while it is hidden.
 *
 * The indicator is the point of collapsing the field at all: the text outlives the control, so
 * without it a result narrowed by a search the user typed a minute ago would look unfiltered. It is
 * a dot rather than a count because a search is one condition, and it is announced in the label
 * rather than left as a decoration.
 */
@Composable
private fun SearchAction(
    isExpanded: Boolean,
    hasQuery: Boolean,
    onClick: () -> Unit,
) {
    val label =
        stringResource(
            when {
                isExpanded -> R.string.log_viewer_search_close_action
                hasQuery -> R.string.log_viewer_search_active_action
                else -> R.string.log_viewer_search_action
            },
        )
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (hasQuery && !isExpanded) {
                    Badge()
                }
            },
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Search,
                contentDescription = label,
            )
        }
    }
}

/**
 * Tap-to-toggle between the two orders, as one icon.
 *
 * The icon is the same in both directions because it names the control, not the direction; the
 * direction lives in the label, which states the order in effect *and* the one a tap produces —
 * without both, a toggle whose only visible state is an icon is unusable without sight.
 */
@Composable
private fun SortAction(
    sortOrder: LogSortOrder,
    onClick: () -> Unit,
) {
    val label =
        stringResource(
            when (sortOrder) {
                LogSortOrder.NewestFirst -> R.string.log_viewer_sort_action_newest_first
                LogSortOrder.OldestFirst -> R.string.log_viewer_sort_action_oldest_first
            },
        )
    IconButton(onClick = onClick) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = label)
    }
}

/**
 * The search field, revealed under the bar and focused as it appears.
 *
 * Autofocus is what makes a hidden field worth hiding: the tap that opened it is the same intent as
 * the tap that would have selected it, so requiring a second one would make the compact bar cost the
 * user an interaction. The request runs in an effect rather than during composition because the node
 * has to exist before it can take focus.
 */
@Composable
private fun ExpandedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // Skipped for a preview or a golden: those are still renders of the screen, and taking focus
    // there opens a text-input session — which is an interaction they cannot have, and whose IME
    // machinery a host-side render has no thread to run.
    val isInspecting = LocalInspectionMode.current
    LaunchedEffect(isInspecting) {
        if (!isInspecting) {
            focusRequester.requestFocus()
        }
    }
    // Centered and width-limited like the content below, while the bar itself spans the window.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LogSearchField(
            query = query,
            onQueryChange = onQueryChange,
            enabled = true,
            modifier =
                Modifier
                    .testTag(SEARCH_TEST_TAG)
                    .widthIn(max = Dimens.contentMaxWidth)
                    .padding(horizontal = Dimens.screenHorizontalPadding)
                    .padding(bottom = Spacing.xs)
                    .focusRequester(focusRequester),
        )
    }
}

/**
 * The summary card sits above exactly one flat [LazyColumn]. The list is the only scrolling region,
 * so the density reading stays in view while scanning results — and the counts, filter, search, and
 * sort controls stay in view above it, in the pinned app bar.
 */
@Composable
private fun LogViewerContent(
    state: LogViewerUiState,
    logs: LazyPagingItems<LogViewerListItem>,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (state.showsStaleSnapshotNotice) {
            StaleSnapshotNotice(
                onRetry = { onAction(LogViewerAction.RetryClicked) },
                modifier = horizontalPadding,
            )
        }
        SeveritySummaryCard(summary = state.summary, modifier = horizontalPadding)
        // Paging's own refresh: a new query generation, or a snapshot replacement invalidating the
        // source. It is a thin bar rather than a skeleton because the previous rows are still below
        // it — replacing them would be the launch refresh's behavior, not a re-query's.
        if (logs.loadState.refresh is LoadState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            // Only the list itself is tagged: scrolling is a list-level gesture, and rows stay keyed
            // by the production stable IDs rather than gaining per-row automation tags.
            modifier = Modifier.testTag(LIST_TEST_TAG).fillMaxWidth().weight(1f),
            contentPadding =
                PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    bottom = Spacing.lg,
                ),
        ) {
            when {
                // A failed generation takes precedence over both other branches: it is why the list
                // is short or empty, and "no matching logs" would state a result the query never
                // produced. The rows follow the notice rather than being replaced by it, so a retry
                // that fails again costs nothing the previous generation had already loaded.
                logs.loadState.refresh is LoadState.Error -> {
                    item(key = REFRESH_ERROR_KEY, contentType = REFRESH_ERROR_CONTENT_TYPE) {
                        ListLoadError(
                            title = stringResource(R.string.log_viewer_page_error_title),
                            message = stringResource(R.string.log_viewer_page_error_message),
                            onRetry = logs::retry,
                        )
                    }
                    pagedLogItems(logs = logs, onAction = onAction)
                }

                // Only once the aggregate has *counted* zero — never because the loaded window
                // happens to be empty, which is also true of every query while its first page loads.
                // The state is one item in the same list rather than a replacement layout, so the
                // summary card, search field, and result row stay exactly where they were.
                state.hasNoMatches -> {
                    item(key = NO_RESULTS_KEY, contentType = NO_RESULTS_CONTENT_TYPE) {
                        NoResultsContent(
                            title = stringResource(R.string.log_viewer_no_results_title),
                            message = stringResource(R.string.log_viewer_no_results_message),
                        )
                    }
                }

                else -> {
                    pagedLogItems(logs = logs, onAction = onAction)
                    appendStateItem(logs = logs)
                }
            }
        }
    }
}

/**
 * The flat header/row list, keyed and content-typed straight off each item.
 *
 * `itemKey`/`itemContentType` are Paging's own wrappers rather than plain lambdas: they keep the
 * key stable while an item is being loaded or evicted, which is what stops the list from losing its
 * scroll position when a page arrives.
 */
private fun LazyListScope.pagedLogItems(
    logs: LazyPagingItems<LogViewerListItem>,
    onAction: (LogViewerAction) -> Unit,
) {
    items(
        count = logs.itemCount,
        key = logs.itemKey { it.stableKey },
        contentType = logs.itemContentType { it.contentType },
    ) { index ->
        when (val item = logs[index]) {
            is LogViewerListItem.MinuteHeader -> {
                // No entry count: a paged list holds part of the result, so the size of the minute
                // is unknown until the whole group happens to be loaded.
                LogMinuteHeader(minute = item.minute)
            }

            is LogViewerListItem.LogRow -> {
                LogRow(
                    severityLabel = item.row.severityLabel,
                    severityTone = item.row.severityTone,
                    tagLabel = item.row.tagLabel,
                    message = item.row.message,
                    time = item.row.time,
                    onClick = { onAction(LogViewerAction.LogSelected(item.row.id)) },
                )
            }

            // Placeholders are disabled, so a null is only ever an item being evicted mid-frame.
            null -> {
                Unit
            }
        }
    }
}

/**
 * Append progress and append failure, as the list's last item so the rows already loaded stay
 * exactly where they are — reaching the end of a page must not cost the user their place.
 *
 * Retry calls Paging's own [LazyPagingItems.retry] rather than travelling through
 * `LogViewerAction`: the failed load belongs to the `LazyPagingItems` instance, which the ViewModel
 * neither owns nor can address, so routing it through the reducer would mean inventing a channel
 * back to a component the screen is already holding. The refresh failure above retries the same
 * way and for the same reason.
 */
private fun LazyListScope.appendStateItem(logs: LazyPagingItems<LogViewerListItem>) {
    when (logs.loadState.append) {
        is LoadState.Loading -> {
            item(key = APPEND_KEY, contentType = APPEND_CONTENT_TYPE) {
                AppendProgress()
            }
        }

        is LoadState.Error -> {
            item(key = APPEND_KEY, contentType = APPEND_CONTENT_TYPE) {
                ListLoadError(
                    title = stringResource(R.string.log_viewer_append_error_title),
                    message = stringResource(R.string.log_viewer_append_error_message),
                    onRetry = logs::retry,
                )
            }
        }

        is LoadState.NotLoading -> {
            Unit
        }
    }
}

@Composable
private fun AppendProgress(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(SortIconSize))
        Text(
            text = stringResource(R.string.log_viewer_append_loading),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The one in-list failure notice, used by both Paging load states: only the copy differs, and
 * neither variant names the underlying cause — `ARCHITECTURE.md` requires generic load-state UI and
 * Paging's own retry rather than branching on an infrastructure exception.
 */
@Composable
private fun ListLoadError(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.log_viewer_retry_action))
        }
    }
}

/**
 * What is left of a dismissed refresh failure: a statement that the rows below may be stale, and the
 * only retry the screen still offers.
 *
 * It sits above the summary card rather than inside the list because it qualifies everything on the
 * screen — the counts as much as the rows — and because a notice that scrolls away is a notice the
 * user stops seeing.
 */
@Composable
private fun StaleSnapshotNotice(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.log_viewer_stale_snapshot),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.log_viewer_retry_action))
            }
        }
    }
}

/**
 * Outlined container around the Canvas density indicator, matching the wireframe's summary box.
 *
 * A pending aggregate draws the same card with skeletons in place of the numbers. Carrying the
 * previous query's percentage across would be the one thing this card must never do — it would
 * state a density for a result set that is no longer on screen — and blanking the card entirely
 * would move everything below it on every keystroke.
 */
@Composable
private fun SeveritySummaryCard(
    summary: LogViewerSummaryState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when (summary) {
                LogViewerSummaryState.Pending -> {
                    Text(
                        text = stringResource(R.string.log_viewer_error_density_pending),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SeverityIndicatorPlaceholder()
                }

                is LogViewerSummaryState.Ready -> {
                    ReadySeveritySummary(summary.summary)
                }
            }
        }
    }
}

@Composable
private fun ReadySeveritySummary(summary: SeveritySummaryUi) {
    val density = summary.toDensityUi()
    SeverityIndicator(density = density)
}

/** The ring and legend's shape without their values, so nothing here can be read as a count. */
@Composable
private fun SeverityIndicatorPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SummarySkeleton(modifier = Modifier.size(SummaryRingPlaceholderSize), shape = CircleShape)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            repeat(3) {
                SummarySkeleton(modifier = Modifier.fillMaxWidth().height(SummaryTextPlaceholderHeight))
            }
        }
    }
}

@Composable
private fun SummarySkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier =
            modifier
                // clip() first so the shimmer band, which draws an unclipped rect, stays inside the
                // block's shape instead of tinting the corners around it.
                .clip(shape)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .shimmerEffect(highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
    )
}

/**
 * How many *log rows* are loaded, excluding the minute headers the transformation inserted.
 *
 * `itemCount` would count those headers too and report more logs than the title claims are loaded.
 * The loaded window is bounded by Paging\'s own working set, not by the result size.
 */
private fun LazyPagingItems<LogViewerListItem>.loadedRowCount(): Int = itemSnapshotList.count { it is LogViewerListItem.LogRow }

/**
 * Groups a count before it is inserted into a resource, so the format string stays a plain `%s`.
 * The locale is fixed because the prototype ships English-only copy and the visual goldens must
 * render the same text on every machine.
 */
private fun groupedCount(count: Int): String = COUNT_FORMAT.format(count)
