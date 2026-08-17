package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.component.ErrorDialog
import com.example.fgfchallenge.core.designsystem.component.LoadingContent
import com.example.fgfchallenge.core.designsystem.component.LogMinuteHeader
import com.example.fgfchallenge.core.designsystem.component.LogRow
import com.example.fgfchallenge.core.designsystem.component.LogSearchField
import com.example.fgfchallenge.core.designsystem.component.NoResultsContent
import com.example.fgfchallenge.core.designsystem.component.SeverityIndicator
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogRowUi
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.toDensityUi

private val SortIconSize = 18.dp

// The no-results item shares the flat list's key namespace, so it gets its own prefix.
private const val NO_RESULTS_KEY = "empty:log-viewer-no-results"
private const val NO_RESULTS_CONTENT_TYPE = "log-viewer-no-results"

/**
 * The log viewer screen: it arranges the components exported by `:core:designsystem` and forwards
 * user input to its callbacks. It holds no state of its own and formats nothing, so every state is
 * previewable and directly snapshot-testable.
 *
 * Every callback is supplied by the caller. Until Roadmap #4 introduces the ViewModel they are
 * no-ops, which is why the search field stays enabled but does not filter.
 */
@Composable
internal fun LogViewerScreen(
    state: LogViewerUiState,
    onQueryChange: (String) -> Unit,
    onSortToggle: () -> Unit,
    onRetry: () -> Unit,
    onErrorDismiss: () -> Unit,
    onRowClick: (LogRowUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
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
                when (state) {
                    LogViewerUiState.Loading -> {
                        LoadingContent()
                    }

                    is LogViewerUiState.Error -> {
                        ErrorDialog(
                            title = state.title,
                            message = state.message,
                            onRetry = onRetry,
                            onDismiss = onErrorDismiss,
                        )
                    }

                    is LogViewerUiState.Content -> {
                        LogViewerContent(
                            state = state,
                            onQueryChange = onQueryChange,
                            onSortToggle = onSortToggle,
                            onRowClick = onRowClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
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
    state: LogViewerUiState.Content,
    onQueryChange: (String) -> Unit,
    onSortToggle: () -> Unit,
    onRowClick: (LogRowUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SeveritySummaryCard(summary = state.severitySummary, modifier = horizontalPadding)
        LogSearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            enabled = true,
            modifier = horizontalPadding,
        )
        ResultSortRow(
            resultCountLabel = state.resultCountLabel,
            sortLabel = state.sortLabel,
            onSortToggle = onSortToggle,
            modifier = horizontalPadding,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding =
                PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    bottom = Spacing.lg,
                ),
        ) {
            if (state.items.isEmpty()) {
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
                    items = state.items,
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
                                onClick = { onRowClick(item.row) },
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
 * Result count and sort control sit directly above the grouped rows they describe.
 *
 * The wireframe's ⇅ glyph has no equivalent in `material-icons-core`, so the arrow points in the
 * direction of the active order instead — downward for newest first.
 */
@Composable
private fun ResultSortRow(
    resultCountLabel: String,
    sortLabel: String,
    onSortToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = resultCountLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        TextButton(onClick = onSortToggle) {
            Text(text = sortLabel, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.log_viewer_sort_action),
                modifier = Modifier.size(SortIconSize),
            )
        }
    }
}
