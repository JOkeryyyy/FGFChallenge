package com.example.fgfchallenge.feature.logs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.feature.logs.presentation.ui.LogFilterSheetHost
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerAction
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerFixture
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerScreen
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerViewModel
import com.example.fgfchallenge.feature.logs.presentation.ui.logViewerFixtureItems
import com.example.fgfchallenge.feature.logs.presentation.ui.logViewerFixtureState

/**
 * The log viewer's only public entry point, composed by `:app`.
 *
 * This is the feature's root composable: it owns the ViewModel, collects its state with lifecycle
 * awareness, and hands `LogViewerScreen` nothing but state and one action callback, which is what
 * keeps the screen previewable and directly testable.
 *
 * The ViewModel is resolved through `viewModel()` rather than `hiltViewModel()` because `:app`'s
 * host activity is `@AndroidEntryPoint`, so its default factory already is Hilt's — this avoids
 * pulling in `hilt-navigation-compose` for a prototype that has no navigation graph.
 *
 * The two streams are collected differently on purpose: bounded state through
 * `collectAsStateWithLifecycle`, and the paged rows through `collectAsLazyPagingItems`, which keeps
 * Paging in charge of what is materialized. The alternate screen states remain reachable through
 * this file's previews rather than an in-app selector.
 *
 * The filter sheet is composed here as the screen's *sibling* rather than from inside it, and that
 * placement is load-bearing. `LogFilterSheetHost` owns the uncommitted edit, so a chip tap or a
 * latency drag recomposes only the host: this function re-runs, but `state` and `logs` are the same
 * instances it was already holding, so `LogViewerScreen` skips — and with it the `Scaffold`, the
 * summary card, and the row list. Composing the sheet inside the screen would put the edit back in
 * the screen's own scope and undo that.
 */
@Composable
fun LogsFeature(modifier: Modifier = Modifier) {
    val viewModel = viewModel<LogViewerViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val logs = viewModel.pagedLogs.collectAsLazyPagingItems()
    LogViewerScreen(
        state = state,
        logs = logs,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
    if (state.isFilterSheetOpen) {
        LogFilterSheetHost(
            appliedFilters = state.filters,
            options = state.filterOptions,
            onApply = { selection -> viewModel.onAction(LogViewerAction.FiltersApplied(selection)) },
            onDismissRequest = { viewModel.onAction(LogViewerAction.FilterSheetDismissed) },
        )
    }
}

@Composable
private fun LogViewerFixturePreview(
    fixture: LogViewerFixture,
    darkTheme: Boolean,
) {
    FGFChallengeTheme(darkTheme = darkTheme) {
        LogViewerScreen(
            state = logViewerFixtureState(fixture),
            logs = logViewerFixtureItems(fixture),
            onAction = {},
        )
    }
}

@Preview(name = "Loading light", widthDp = 360, heightDp = 640)
@Composable
private fun LoadingLightPreview() = LogViewerFixturePreview(LogViewerFixture.Loading, darkTheme = false)

@Preview(name = "Loading dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingDarkPreview() = LogViewerFixturePreview(LogViewerFixture.Loading, darkTheme = true)

@Preview(name = "Error light", widthDp = 360, heightDp = 640)
@Composable
private fun ErrorLightPreview() = LogViewerFixturePreview(LogViewerFixture.Error, darkTheme = false)

@Preview(name = "Error dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorDarkPreview() = LogViewerFixturePreview(LogViewerFixture.Error, darkTheme = true)

@Preview(name = "Populated light", widthDp = 360, heightDp = 640)
@Composable
private fun PopulatedContentLightPreview() = LogViewerFixturePreview(LogViewerFixture.AllLogs, darkTheme = false)

@Preview(name = "Populated dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PopulatedContentDarkPreview() = LogViewerFixturePreview(LogViewerFixture.AllLogs, darkTheme = true)

@Preview(name = "Filtered light", widthDp = 360, heightDp = 640)
@Composable
private fun FilteredContentLightPreview() = LogViewerFixturePreview(LogViewerFixture.Filtered, darkTheme = false)

@Preview(name = "Filtered dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilteredContentDarkPreview() = LogViewerFixturePreview(LogViewerFixture.Filtered, darkTheme = true)

@Preview(name = "Filtered empty light", widthDp = 360, heightDp = 640)
@Composable
private fun FilteredEmptyLightPreview() = LogViewerFixturePreview(LogViewerFixture.FilteredEmpty, darkTheme = false)

@Preview(name = "Filtered empty dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FilteredEmptyDarkPreview() = LogViewerFixturePreview(LogViewerFixture.FilteredEmpty, darkTheme = true)

// The two widths that change the layout: the narrowest supported width, where the severity legend
// stacks under the ring, and Dimens.contentMaxWidth, where the pane centers and the legend sits
// beside it.
@Preview(name = "Populated 320dp", widthDp = 320, heightDp = 640)
@Composable
private fun PopulatedContentNarrowPreview() = LogViewerFixturePreview(LogViewerFixture.AllLogs, darkTheme = false)

@Preview(name = "Populated 760dp", widthDp = 760, heightDp = 900)
@Composable
private fun PopulatedContentWidePreview() = LogViewerFixturePreview(LogViewerFixture.AllLogs, darkTheme = false)

@Preview(name = "Search expanded light", widthDp = 360, heightDp = 640)
@Composable
private fun SearchExpandedLightPreview() = LogViewerFixturePreview(LogViewerFixture.SearchExpanded, darkTheme = false)

@Preview(name = "Stale snapshot light", widthDp = 360, heightDp = 640)
@Composable
private fun StaleSnapshotLightPreview() = LogViewerFixturePreview(LogViewerFixture.StaleSnapshot, darkTheme = false)

@Preview(name = "Stale snapshot dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StaleSnapshotDarkPreview() = LogViewerFixturePreview(LogViewerFixture.StaleSnapshot, darkTheme = true)

@Preview(name = "Append progress 360dp", widthDp = 360, heightDp = 640)
@Composable
private fun AppendLoadingPreview() = LogViewerFixturePreview(LogViewerFixture.AppendLoading, darkTheme = false)

@Preview(name = "Append retry 360dp", widthDp = 360, heightDp = 640)
@Composable
private fun AppendErrorPreview() = LogViewerFixturePreview(LogViewerFixture.AppendError, darkTheme = false)

@Preview(name = "Page refresh retry 360dp", widthDp = 360, heightDp = 640)
@Composable
private fun PageRefreshErrorPreview() = LogViewerFixturePreview(LogViewerFixture.PageRefreshError, darkTheme = false)
