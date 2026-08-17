package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

// The no-results item shares the flat list's key namespace, so it gets its own prefix.
private const val NO_RESULTS_KEY = "empty:log-viewer-no-results"
private const val NO_RESULTS_CONTENT_TYPE = "log-viewer-no-results"

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
 * localized labels its state stores as raw values, and reports every feature interaction through
 * one [onAction] callback. It holds no application state of its own, so every state is previewable
 * and directly snapshot-testable. Compose-owned keyboard focus is handled locally at the screen
 * boundary.
 *
 * The details sheet is not a separate branch of the layout: it renders whenever
 * [LogViewerUiState.selectedLog] is set, over whatever the body is showing.
 */
@Composable
internal fun LogViewerScreen(
    state: LogViewerUiState,
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
                when (val loadState = state.loadState) {
                    LogViewerLoadState.Loading -> {
                        LoadingContent()
                    }

                    is LogViewerLoadState.Error -> {
                        ErrorDialog(
                            title = loadState.title,
                            message = loadState.message,
                            onRetry = { onAction(LogViewerAction.RetryClicked) },
                            onDismiss = { onAction(LogViewerAction.ErrorDismissed) },
                        )
                    }

                    is LogViewerLoadState.Content -> {
                        LogViewerContent(
                            query = state.query,
                            sortOrder = state.sortOrder,
                            activeFilterCount = state.activeFilterCount,
                            content = loadState,
                            onAction = onAction,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
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
    query: String,
    sortOrder: LogSortOrder,
    activeFilterCount: Int,
    content: LogViewerLoadState.Content,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SeveritySummaryCard(summary = content.severitySummary, modifier = horizontalPadding)
        LogSearchField(
            query = query,
            onQueryChange = { onAction(LogViewerAction.QueryChanged(it)) },
            enabled = true,
            modifier = horizontalPadding,
        )
        FilterSortRow(
            activeFilterCount = activeFilterCount,
            sortOrder = sortOrder,
            onFilterClick = { onAction(LogViewerAction.FilterSheetOpened) },
            onSortToggle = { onAction(LogViewerAction.SortOrderToggled) },
            modifier = horizontalPadding,
        )
        ResultCountLabel(resultCount = content.resultCount, modifier = horizontalPadding)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding =
                PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    bottom = Spacing.lg,
                ),
        ) {
            if (content.items.isEmpty()) {
                // The no-results state is one item in the same list rather than a replacement
                // layout, so the summary card, search field, and result row stay exactly where
                // they were before the query stopped matching.
                item(key = NO_RESULTS_KEY, contentType = NO_RESULTS_CONTENT_TYPE) {
                    NoResultsContent(
                        title = stringResource(R.string.log_viewer_no_results_title),
                        message = stringResource(R.string.log_viewer_no_results_message),
                    )
                }
            } else {
                items(
                    items = content.items,
                    key = { item -> item.stableKey },
                    contentType = { item -> item.contentType },
                ) { item ->
                    when (item) {
                        is LogViewerListItem.MinuteHeader -> {
                            LogMinuteHeader(minute = item.minute, itemCount = item.itemCount)
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
                    }
                }
            }
        }
    }
}

/** Outlined container around the Canvas density indicator, matching the wireframe's summary box. */
@Composable
private fun SeveritySummaryCard(
    summary: SeveritySummaryUi,
    modifier: Modifier = Modifier,
) {
    val density = summary.toDensityUi()
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
    }
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

/** Result count sits directly above the grouped rows it describes. */
@Composable
private fun ResultCountLabel(
    resultCount: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = resultCountLabel(resultCount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Formats the count with grouping separators before inserting it, so the plural resource stays a
 * plain `%1$s`. The locale is fixed because the prototype ships English-only copy and the visual
 * goldens must render the same text on every machine.
 */
@Composable
private fun resultCountLabel(count: Int): String =
    pluralStringResource(
        R.plurals.log_viewer_result_count,
        count,
        NumberFormat.getIntegerInstance(Locale.US).format(count),
    )

@Composable
private fun sortLabel(sortOrder: LogSortOrder): String =
    stringResource(
        when (sortOrder) {
            LogSortOrder.NewestFirst -> R.string.log_viewer_sort_newest_first
            LogSortOrder.OldestFirst -> R.string.log_viewer_sort_oldest_first
        },
    )
