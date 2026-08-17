package com.example.fgfchallenge.core.designsystem.showcase

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fgfchallenge.core.designsystem.component.ErrorDialog
import com.example.fgfchallenge.core.designsystem.component.LoadingContent
import com.example.fgfchallenge.core.designsystem.component.LogDetailsSheet
import com.example.fgfchallenge.core.designsystem.component.LogMinuteHeader
import com.example.fgfchallenge.core.designsystem.component.LogRow
import com.example.fgfchallenge.core.designsystem.component.LogSearchField
import com.example.fgfchallenge.core.designsystem.component.NoResultsContent
import com.example.fgfchallenge.core.designsystem.component.SeverityBadge
import com.example.fgfchallenge.core.designsystem.component.SeverityIndicator
import com.example.fgfchallenge.core.designsystem.component.TagBadge
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing

private data class ShowcaseRowFixture(
    val severityLabel: String,
    val severityTone: SeverityBadgeTone,
    val tagLabel: String,
    val message: String,
    val time: String,
)

private val ShowcaseRows =
    listOf(
        ShowcaseRowFixture("ERROR", SeverityBadgeTone.Error, "network", "Connection timed out", "58.123"),
        ShowcaseRowFixture("FATAL", SeverityBadgeTone.Fatal, "auth", "Auth service unreachable", "46.204"),
        ShowcaseRowFixture("WARN", SeverityBadgeTone.Warn, "cache", "Cache miss", "37.812"),
        ShowcaseRowFixture("INFO", SeverityBadgeTone.Info, "network", "Request completed", "21.439"),
        ShowcaseRowFixture("DEBUG", SeverityBadgeTone.Debug, "cache", "Cache lookup key=1234", "11.098"),
    )

private val ShowcaseLegendItems =
    listOf(
        SeverityLegendItem("ERROR", 1_256, SeverityBadgeTone.Error),
        SeverityLegendItem("FATAL", 794, SeverityBadgeTone.Fatal),
        SeverityLegendItem("WARN", 1_143, SeverityBadgeTone.Warn),
        SeverityLegendItem("INFO", 1_207, SeverityBadgeTone.Info),
        SeverityLegendItem("DEBUG", 600, SeverityBadgeTone.Debug),
    )

private val ShowcaseDetails =
    LogDetailsUi(
        severityLabel = "ERROR",
        severityTone = SeverityBadgeTone.Error,
        message = "Connection timed out",
        tag = "network",
        timestampUtc = "2025-05-22T17:11:58.123Z",
        latency = "3,245 ms",
        aiGenerated = "No",
        logId = "a1b2c3d4-e5f6-7890-abcd-1234567890ef",
        sessionId = "sess-7f3a9b21-7cd4-4d6d-9a12-3f5e7d9a1b2c",
    )

/**
 * Developer-facing catalog of every design-system component, built from private fixtures only.
 * It never imports feature models or assembles real screen state — every callback here is a
 * no-op, and visual state comes entirely from the fixture values passed in.
 */
@Composable
fun LogComponentsShowcase(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = Dimens.contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenHorizontalPadding, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            ShowcaseSection(title = "Severity badge") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SeverityBadgeTone.entries.forEach { tone ->
                        SeverityBadge(label = tone.name.uppercase(), tone = tone)
                    }
                }
            }

            ShowcaseSection(title = "Tag badge") {
                TagBadge(label = "network")
            }

            ShowcaseSection(title = "Log row") {
                Column {
                    ShowcaseRows.forEach { row ->
                        LogRow(
                            severityLabel = row.severityLabel,
                            severityTone = row.severityTone,
                            tagLabel = row.tagLabel,
                            message = row.message,
                            time = row.time,
                            onClick = {},
                        )
                    }
                }
            }

            ShowcaseSection(title = "Minute header") {
                Column {
                    LogMinuteHeader(minute = "17:11", itemCount = 12)
                    LogMinuteHeader(
                        minute = "17:10",
                        itemCount = 8,
                        isCollapsed = false,
                        onCollapsedChange = {},
                    )
                    LogMinuteHeader(
                        minute = "17:09",
                        itemCount = 5,
                        isCollapsed = true,
                        onCollapsedChange = {},
                    )
                }
            }

            ShowcaseSection(title = "Search field") {
                LogSearchField(query = "network", onQueryChange = {}, enabled = true)
            }

            ShowcaseSection(title = "Severity indicator") {
                SeverityIndicator(
                    totalLogCount = 5_000,
                    errorCount = 1_039,
                    fatalCount = 1_011,
                    legendItems = ShowcaseLegendItems,
                )
            }

            ShowcaseSection(title = "Loading content") {
                LoadingContent()
            }

            ShowcaseSection(title = "Error dialog") {
                ErrorDialog(
                    title = "Unable to load logs",
                    message = "We couldn't fetch logs from the server.",
                    onRetry = {},
                    onDismiss = {},
                )
            }

            ShowcaseSection(title = "No results") {
                NoResultsContent(
                    title = "No matching logs",
                    message = "Try a different search term.",
                )
            }

            ShowcaseSection(title = "Log details sheet") {
                LogDetailsSheet(details = ShowcaseDetails, onDismissRequest = {})
            }
        }
    }
}

@Composable
private fun ShowcaseSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Preview(name = "360 light", widthDp = 360, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogComponentsShowcaseLightPreview() {
    FGFChallengeTheme(darkTheme = false) {
        LogComponentsShowcase()
    }
}

@Preview(
    name = "360 dark",
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF101416L,
)
@Composable
private fun LogComponentsShowcaseDarkPreview() {
    FGFChallengeTheme(darkTheme = true) {
        LogComponentsShowcase()
    }
}

@Preview(name = "320 narrow", widthDp = 320, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogComponentsShowcaseNarrowPreview() {
    FGFChallengeTheme {
        LogComponentsShowcase()
    }
}

@Preview(name = "760 wide", widthDp = 760, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogComponentsShowcaseWidePreview() {
    FGFChallengeTheme {
        LogComponentsShowcase()
    }
}
