package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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

private val SortIconSize = 18.dp
private val SummaryRingPlaceholderSize = 72.dp
private val SummaryTextPlaceholderHeight = 16.dp

// The list's own items share the paged rows' key namespace, so each gets its own prefix.
private const val NO_RESULTS_KEY = "empty:log-viewer-no-results"
private const val NO_RESULTS_CONTENT_TYPE = "log-viewer-no-results"
private const val APPEND_KEY = "append:log-viewer-append-state"
private const val APPEND_CONTENT_TYPE = "log-viewer-append-state"

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
 * state, [logs] the paged rows. The screen never merges them into one list — it reads counts from
 * [state] and rows from [logs], and the one place it combines them, the result line, says which
 * number came from which.
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

    Surface(
        modifier = modifier.fillMaxSize().clearFocusOnUnconsumedTap(focusManager),
        color = MaterialTheme.colorScheme.background,
    ) {
        // One centered pane: it grows with the window up to Dimens.contentMaxWidth and then stops,
        // so log rows never stretch into unscannable full-width lines on a large screen.
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier.widthIn(max = Dimens.contentMaxWidth).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                LogViewerTitle()
                if (state.refresh is LogViewerRefreshState.InProgress) {
                    // Skeletons rather than the stored rows: until the refresh resolves, nothing
                    // known about the snapshot is worth presenting as the current result.
                    LoadingContent()
                } else {
                    LogViewerContent(
                        state = state,
                        logs = logs,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
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
        // Same idiom for the filter sheet: it is open exactly while a draft exists, and every edit
        // it reports goes straight back out as an action. The screen translates between the design
        // system's vocabulary and the feature's, and decides nothing itself.
        state.filterDraft?.let { draft ->
            LogFilterSheet(
                filters = draft.toFilterSheetUi(state.filterOptions),
                onEvent = { event -> event.toAction()?.let(onAction) },
                onDismissRequest = { onAction(LogViewerAction.FilterSheetDismissed) },
            )
        }
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

@Composable
private fun LogViewerTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.log_viewer_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenHorizontalPadding, vertical = Spacing.xs),
    )
}

/**
 * Summary card, search field, and the result/sort row sit above exactly one flat [LazyColumn], as
 * the wireframe specifies. The list is the only scrolling region, so the search box and the density
 * reading stay in view while scanning results.
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
        LogSearchField(
            query = state.query,
            onQueryChange = { onAction(LogViewerAction.QueryChanged(it)) },
            enabled = true,
            modifier = horizontalPadding,
        )
        FilterSortRow(
            activeFilterCount = state.activeFilterCount,
            sortOrder = state.sortOrder,
            onFilterClick = { onAction(LogViewerAction.FilterSheetOpened) },
            onSortToggle = { onAction(LogViewerAction.SortOrderToggled) },
            modifier = horizontalPadding,
        )
        ResultCountLabel(
            summary = state.summary,
            loadedRowCount = logs.loadedRowCount(),
            modifier = horizontalPadding,
        )
        // Paging's own refresh: a new query generation, or a snapshot replacement invalidating the
        // source. It is a thin bar rather than a skeleton because the previous rows are still below
        // it — replacing them would be the launch refresh's behavior, not a re-query's.
        if (logs.loadState.refresh is LoadState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding =
                PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    bottom = Spacing.lg,
                ),
        ) {
            if (state.hasNoMatches) {
                // Only once the aggregate has *counted* zero — never because the loaded window
                // happens to be empty, which is also true of every query while its first page loads.
                // The state is one item in the same list rather than a replacement layout, so the
                // summary card, search field, and result row stay exactly where they were.
                item(key = NO_RESULTS_KEY, contentType = NO_RESULTS_CONTENT_TYPE) {
                    NoResultsContent(
                        title = stringResource(R.string.log_viewer_no_results_title),
                        message = stringResource(R.string.log_viewer_no_results_message),
                    )
                }
            } else {
                pagedLogItems(logs = logs, onAction = onAction)
                appendStateItem(logs = logs)
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
 * back to a component the screen is already holding.
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
                AppendError(onRetry = logs::retry)
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

@Composable
private fun AppendError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = stringResource(R.string.log_viewer_append_error_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.log_viewer_append_error_message),
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
    Text(
        text = stringResource(R.string.log_viewer_error_density, density.densityPercent),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = stringResource(R.string.log_viewer_error_density_caption),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
 * Filter entry and sort control, as the wireframe pairs them: both change what the list below
 * shows, and both stay visible while scanning it.
 *
 * The wireframe's ⇅ glyph has no equivalent in `material-icons-core`, so the arrow points in the
 * direction of the active order instead — downward for newest first, upward for oldest first.
 */
@Composable
private fun FilterSortRow(
    activeFilterCount: Int,
    sortOrder: LogSortOrder,
    onFilterClick: () -> Unit,
    onSortToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterButton(activeFilterCount = activeFilterCount, onClick = onFilterClick)
        TextButton(onClick = onSortToggle) {
            Text(text = sortLabel(sortOrder), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Icon(
                imageVector =
                    when (sortOrder) {
                        LogSortOrder.NewestFirst -> Icons.Default.KeyboardArrowDown
                        LogSortOrder.OldestFirst -> Icons.Default.KeyboardArrowUp
                    },
                contentDescription = stringResource(R.string.log_viewer_sort_action),
                modifier = Modifier.size(SortIconSize),
            )
        }
    }
}

/**
 * Opens the filter sheet, and states how many filter categories are currently narrowing the result.
 *
 * The count is a badge rather than only a highlight because the sheet's contents are out of sight
 * once it closes: without it, a result narrowed by an applied filter is indistinguishable from an
 * unfiltered one. Zero renders as no badge at all, matching the wireframe's inactive state.
 */
@Composable
private fun FilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Text(
            text = stringResource(R.string.log_viewer_filter_action),
            style = MaterialTheme.typography.labelLarge,
        )
        if (activeFilterCount > 0) {
            // "3" alone reads as a bare number to a screen reader, so the badge carries the phrase
            // the sighted reading gets from its position next to Filter.
            val countDescription =
                pluralStringResource(
                    R.plurals.log_viewer_active_filter_count,
                    activeFilterCount,
                    activeFilterCount,
                )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { contentDescription = countDescription },
            ) {
                Text(
                    text = activeFilterCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * The two counts, side by side, directly above the rows they describe: how many logs the query
 * matches in the database, and how many of them Paging currently holds.
 *
 * Naming both is the point. The matching count comes from the aggregate over the complete filtered
 * result and is routinely orders of magnitude larger than the loaded one; showing only the loaded
 * count would understate the result, and showing only the total would leave no sign that the list
 * is paged.
 */
@Composable
private fun ResultCountLabel(
    summary: LogViewerSummaryState,
    loadedRowCount: Int,
    modifier: Modifier = Modifier,
) {
    val loaded = groupedCount(loadedRowCount)
    Text(
        text =
            when (summary) {
                LogViewerSummaryState.Pending -> {
                    stringResource(R.string.log_viewer_result_count_pending, loaded)
                }

                is LogViewerSummaryState.Ready -> {
                    stringResource(
                        R.string.log_viewer_result_count,
                        groupedCount(summary.summary.totalLogCount),
                        loaded,
                    )
                }
            },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * How many *log rows* are loaded, excluding the minute headers the transformation inserted.
 *
 * `itemCount` would count those headers too and report more logs than the list holds. Scanning the
 * loaded window is bounded by Paging's own working set, not by the result size.
 */
private fun LazyPagingItems<LogViewerListItem>.loadedRowCount(): Int = itemSnapshotList.count { it is LogViewerListItem.LogRow }

/**
 * Groups a count before it is inserted into a resource, so the format string stays a plain `%s`.
 * The locale is fixed because the prototype ships English-only copy and the visual goldens must
 * render the same text on every machine.
 */
private fun groupedCount(count: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(count)

@Composable
private fun sortLabel(sortOrder: LogSortOrder): String =
    stringResource(
        when (sortOrder) {
            LogSortOrder.NewestFirst -> R.string.log_viewer_sort_newest_first
            LogSortOrder.OldestFirst -> R.string.log_viewer_sort_oldest_first
        },
    )
