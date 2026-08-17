package com.example.fgfchallenge.feature.logs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.feature.logs.presentation.LogViewerFixture
import com.example.fgfchallenge.feature.logs.presentation.LogViewerScreen
import com.example.fgfchallenge.feature.logs.presentation.logViewerFixtureState

/**
 * The log viewer's only public entry point, composed by `:app`.
 *
 * This milestone mounts the populated fixture with no-op callbacks: the screen, its states, and its
 * layout are complete, while query, sort, retry, dismissal, and row selection start doing real work
 * in Roadmap #4, when the ViewModel replaces the fixture. The alternate states are reachable
 * through this file's previews rather than an in-app selector.
 */
@Composable
fun LogsFeature(modifier: Modifier = Modifier) {
    LogViewerScreen(
        state = logViewerFixtureState(LogViewerFixture.AllLogs),
        onQueryChange = {},
        onSortToggle = {},
        onRetry = {},
        onErrorDismiss = {},
        onRowClick = {},
        modifier = modifier,
    )
}

@Composable
private fun LogViewerFixturePreview(
    fixture: LogViewerFixture,
    darkTheme: Boolean,
) {
    FGFChallengeTheme(darkTheme = darkTheme) {
        LogViewerScreen(
            state = logViewerFixtureState(fixture),
            onQueryChange = {},
            onSortToggle = {},
            onRetry = {},
            onErrorDismiss = {},
            onRowClick = {},
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
