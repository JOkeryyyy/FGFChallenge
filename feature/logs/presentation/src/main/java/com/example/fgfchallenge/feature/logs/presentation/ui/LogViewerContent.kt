package com.example.fgfchallenge.feature.logs.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.fgfchallenge.core.designsystem.component.LogMinuteHeader
import com.example.fgfchallenge.core.designsystem.component.LogRow
import com.example.fgfchallenge.core.designsystem.component.NoResultsContent
import com.example.fgfchallenge.core.designsystem.component.SeverityIndicator
import com.example.fgfchallenge.core.designsystem.modifier.shimmerEffect
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.toDensityUi

/**
 * The summary card sits above exactly one flat [androidx.compose.foundation.lazy.LazyColumn]. The list is the only scrolling region,
 * so the density reading stays in view while scanning results — and the counts, filter, search, and
 * sort controls stay in view above it, in the pinned app bar.
 *
 * The three state-derived inputs arrive as separate values rather than as the whole
 * [LogViewerUiState], and that is load-bearing rather than stylistic. The `LazyColumn`'s content
 * lambda closes over whatever this function is handed, and the Compose compiler keys the lambda's
 * memoization on those captures; a captured state value therefore produced a *new* content lambda
 * on every keystroke, since search text is reflected in state immediately. Foundation compares the
 * resulting `LazyListIntervalContent` by reference to decide whether item compositions can be
 * reused, so a new lambda invalidated every composed row — on the frames the keyboard was
 * delivering characters. These three are the only state the list actually reads, and none of them
 * moves while typing, so the lambda instance survives and the rows stay untouched.
 */
@Composable
internal fun LogViewerContent(
    showsStaleSnapshotNotice: Boolean,
    summary: LogViewerSummaryState,
    hasNoMatches: Boolean,
    logs: LazyPagingItems<LogViewerListItem>,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (showsStaleSnapshotNotice) {
            StaleSnapshotNotice(
                onRetry = { onAction(LogViewerAction.RetryClicked) },
                modifier = horizontalPadding,
            )
        }
        SeveritySummaryCard(summary = summary, modifier = horizontalPadding)
        // Paging's own refresh: a new query generation, or a snapshot replacement invalidating the
        // source. It is a thin bar rather than a skeleton because the previous rows are still below
        // it — replacing them would be the launch refresh's behavior, not a re-query's.
        if (logs.loadState.refresh is LoadState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            // Only the list itself is tagged: scrolling is a list-level gesture, and rows stay keyed
            // by the production stable IDs rather than gaining per-row automation tags.
            modifier =
                Modifier
                    .testTag(LIST_TEST_TAG)
                    .fillMaxWidth()
                    .weight(1f),
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
                hasNoMatches -> {
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

            // Placeholders are disabled, so a null is only ever an item dropped mid-frame, which
            // happens when a snapshot replacement or a new query invalidates the source.
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Spacing.md),
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
        modifier =
            modifier
                .fillMaxWidth()
                .padding(Spacing.md),
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
                SummarySkeleton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(SummaryTextPlaceholderHeight),
                )
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

// The fourth anchor the out-of-process Macrobenchmark suite drives; the app bar's three live in
// `LogViewerTopBar.kt`. It is a resource-style id rather than an accessibility label because UI
// Automator has to find it by a value that does not change with the query or the locale.
// See `documentation/performanceBenchmark.md`.
private const val LIST_TEST_TAG = "log_viewer_list"

// The list's own items share the paged rows' key namespace, so each gets its own prefix.
private const val NO_RESULTS_KEY = "empty:log-viewer-no-results"
private const val NO_RESULTS_CONTENT_TYPE = "log-viewer-no-results"
private const val APPEND_KEY = "append:log-viewer-append-state"
private const val APPEND_CONTENT_TYPE = "log-viewer-append-state"
private const val REFRESH_ERROR_KEY = "refresh:log-viewer-refresh-error"
private const val REFRESH_ERROR_CONTENT_TYPE = "log-viewer-refresh-error"

private val SortIconSize = 18.dp
private val SummaryRingPlaceholderSize = 72.dp
private val SummaryTextPlaceholderHeight = 16.dp
