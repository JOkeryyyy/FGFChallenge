package com.example.fgfchallenge.feature.logs.presentation.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.paging.compose.LazyPagingItems
import com.example.fgfchallenge.core.designsystem.component.ErrorDialog
import com.example.fgfchallenge.core.designsystem.component.LoadingContent
import com.example.fgfchallenge.core.designsystem.component.LogDetailsSheet
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem

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

    // Both counts below feed the app bar's title and badge, and both used to be computed inline in
    // the `topBar` slot — so every recomposition of this screen re-scanned the loaded window and
    // rebuilt a whole `LogQuery` to count five booleans.
    //
    // `derivedStateOf` for the loaded rows because `itemSnapshotList` is snapshot-backed: the scan
    // re-runs when Paging changes it, and the bar is invalidated only when the number it renders
    // actually moves — not on every append that leaves the count alone.
    val loadedRowCount by remember(logs) { derivedStateOf { logs.loadedRowCount() } }
    // `remember` for the badge because its inputs are plain state fields: the count is re-derived
    // when the applied filters or the snapshot's extent change, and a search keystroke or an arriving
    // summary no longer allocates a query to answer a question whose inputs did not move.
    val activeFilterCount = remember(state.filters, state.filterOptions) { state.activeFilterCount }

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
                loadedRowCount = loadedRowCount,
                activeFilterCount = activeFilterCount,
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pane =
                Modifier
                    .widthIn(max = Dimens.contentMaxWidth)
                    .fillMaxSize()
                    .padding(top = Spacing.sm)
            if (state.refresh is LogViewerRefreshState.InProgress) {
                // Skeletons rather than the stored rows: until the refresh resolves, nothing known
                // about the snapshot is worth presenting as the current result.
                LoadingContent(modifier = pane)
            } else {
                LogViewerContent(
                    // Read out here rather than passed as one state value: see LogViewerContent.
                    showsStaleSnapshotNotice = state.showsStaleSnapshotNotice,
                    summary = state.summary,
                    hasNoMatches = state.hasNoMatches,
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
    // The filter sheet is deliberately *not* here. It is a sibling of this screen under
    // `LogsFeature`, so `LogFilterSheetHost` can own the uncommitted edit without every chip tap
    // publishing a new `LogViewerUiState` and recomposing this whole tree. What reaches this screen
    // is the applied result, through `LogViewerUiState.filters`.
}

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
 * How many *log rows* are loaded, excluding the minute headers the transformation inserted.
 *
 * `itemCount` would count those headers too and report more logs than the title claims are loaded.
 * The loaded window is bounded by Paging\'s own working set, not by the result size.
 */
private fun LazyPagingItems<LogViewerListItem>.loadedRowCount(): Int = itemSnapshotList.count { it is LogViewerListItem.LogRow }
