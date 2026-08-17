package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogRowUi
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.logRowKey
import com.example.fgfchallenge.feature.logs.presentation.model.minuteHeaderKey
import java.text.NumberFormat
import java.util.Locale

/**
 * Sample screen states used by the previews, the Paparazzi goldens, and — until Roadmap #4 wires
 * the ViewModel — the launched app itself.
 *
 * The severity summaries are real: the all-logs counts are the supplied dataset's distribution and
 * the filtered counts are the wireframe's `network` search. The row lists are deliberately short
 * representative samples, so a displayed total of `5,000 results` describes the dataset the screen
 * will show once networking lands, not the number of rows in this fixture.
 */
internal object LogViewerFixtures {
    const val ALL_LOGS_RESULT_COUNT: Int = 5_000
    const val FILTERED_RESULT_COUNT: Int = 718
    const val FILTERED_QUERY: String = "network"

    /** No supplied message, tag, or severity contains this, so it stands in for a dead-end search. */
    const val NONMATCHING_QUERY: String = "kubernetes"

    /** The complete dataset: 1,039 ERROR + 1,011 FATAL of 5,000 entries, so 41% error density. */
    val allLogsSummary: SeveritySummaryUi =
        SeveritySummaryUi(
            totalLogCount = ALL_LOGS_RESULT_COUNT,
            errorCount = 1_039,
            fatalCount = 1_011,
            legendItems =
                legendItems(
                    errorCount = 1_039,
                    fatalCount = 1_011,
                    warnCount = 1_006,
                    infoCount = 1_005,
                    debugCount = 939,
                ),
        )

    /** The wireframe's `network` result: 206 ERROR + 102 FATAL of 718 entries, so 43%. */
    val filteredSummary: SeveritySummaryUi =
        SeveritySummaryUi(
            totalLogCount = FILTERED_RESULT_COUNT,
            errorCount = 206,
            fatalCount = 102,
            legendItems =
                legendItems(
                    errorCount = 206,
                    fatalCount = 102,
                    warnCount = 154,
                    infoCount = 182,
                    debugCount = 74,
                ),
        )

    /** All five severities are kept so the card holds its shape when a search returns nothing. */
    val filteredEmptySummary: SeveritySummaryUi =
        SeveritySummaryUi(
            totalLogCount = 0,
            errorCount = 0,
            fatalCount = 0,
            legendItems =
                legendItems(
                    errorCount = 0,
                    fatalCount = 0,
                    warnCount = 0,
                    infoCount = 0,
                    debugCount = 0,
                ),
        )

    fun allLogsContent(
        resultCountLabel: String,
        sortLabel: String,
    ): LogViewerUiState.Content =
        LogViewerUiState.Content(
            query = "",
            resultCountLabel = resultCountLabel,
            sortLabel = sortLabel,
            severitySummary = allLogsSummary,
            items = allLogsItems,
        )

    fun filteredContent(
        resultCountLabel: String,
        sortLabel: String,
    ): LogViewerUiState.Content =
        LogViewerUiState.Content(
            query = FILTERED_QUERY,
            resultCountLabel = resultCountLabel,
            sortLabel = sortLabel,
            severitySummary = filteredSummary,
            items = filteredItems,
        )

    fun filteredEmptyContent(
        resultCountLabel: String,
        sortLabel: String,
    ): LogViewerUiState.Content =
        LogViewerUiState.Content(
            query = NONMATCHING_QUERY,
            resultCountLabel = resultCountLabel,
            sortLabel = sortLabel,
            severitySummary = filteredEmptySummary,
            items = emptyList(),
        )

    fun error(
        title: String,
        message: String,
    ): LogViewerUiState.Error = LogViewerUiState.Error(title = title, message = message)

    // Newest minute first, and newest row first inside each minute, matching the default sort.
    private val allLogsItems: List<LogViewerListItem> =
        minuteGroup(
            utcMinuteId = "2025-05-22T17:11Z",
            minute = "17:11",
            rows =
                listOf(
                    row("1711-58123", "ERROR", SeverityBadgeTone.Error, "network", "Connection timed out", "58.123"),
                    row("1711-46204", "FATAL", SeverityBadgeTone.Fatal, "auth", "Auth service unreachable", "46.204"),
                    row("1711-37812", "WARN", SeverityBadgeTone.Warn, "cache", "Cache miss", "37.812"),
                    row("1711-21439", "INFO", SeverityBadgeTone.Info, "network", "Request completed", "21.439"),
                    row("1711-11098", "DEBUG", SeverityBadgeTone.Debug, "cache", "Cache lookup key=1234", "11.098"),
                ),
        ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:10Z",
                minute = "17:10",
                rows =
                    listOf(
                        row("1710-59384", "ERROR", SeverityBadgeTone.Error, "network", "DNS resolution failed", "59.384"),
                        row("1710-48660", "WARN", SeverityBadgeTone.Warn, "auth", "Token expiring soon", "48.660"),
                        row("1710-33215", "INFO", SeverityBadgeTone.Info, "cache", "Cache write success", "33.215"),
                        row("1710-21078", "DEBUG", SeverityBadgeTone.Debug, "network", "Retry attempt 1/3", "21.078"),
                        row("1710-07026", "INFO", SeverityBadgeTone.Info, "auth", "User login success", "07.026"),
                    ),
            ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:09Z",
                minute = "17:09",
                rows =
                    listOf(
                        row("1709-45672", "WARN", SeverityBadgeTone.Warn, "network", "High latency detected", "45.672"),
                    ),
            )

    // Every row matches FILTERED_QUERY on its tag, so the sample stays consistent with the query.
    private val filteredItems: List<LogViewerListItem> =
        minuteGroup(
            utcMinuteId = "2025-05-22T17:11Z",
            minute = "17:11",
            rows =
                listOf(
                    row("1711-58123", "ERROR", SeverityBadgeTone.Error, "network", "Connection timed out", "58.123"),
                    row("1711-24673", "WARN", SeverityBadgeTone.Warn, "network", "Slow response detected", "24.673"),
                    row("1711-21121", "INFO", SeverityBadgeTone.Info, "network", "Request completed", "21.121"),
                ),
        ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:10Z",
                minute = "17:10",
                rows =
                    listOf(
                        row("1710-59384", "ERROR", SeverityBadgeTone.Error, "network", "DNS resolution failed", "59.384"),
                        row("1710-21087", "DEBUG", SeverityBadgeTone.Debug, "network", "Retry attempt 1/3", "21.087"),
                        row("1710-11011", "INFO", SeverityBadgeTone.Info, "network", "Connection established", "11.011"),
                    ),
            ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:09Z",
                minute = "17:09",
                rows =
                    listOf(
                        row("1709-45672", "WARN", SeverityBadgeTone.Warn, "network", "High latency detected", "45.672"),
                    ),
            )

    /** Legend order follows the wireframe: error-like severities first, then the rest. */
    private fun legendItems(
        errorCount: Int,
        fatalCount: Int,
        warnCount: Int,
        infoCount: Int,
        debugCount: Int,
    ): List<SeverityLegendItem> =
        listOf(
            SeverityLegendItem("ERROR", errorCount, SeverityBadgeTone.Error),
            SeverityLegendItem("FATAL", fatalCount, SeverityBadgeTone.Fatal),
            SeverityLegendItem("WARN", warnCount, SeverityBadgeTone.Warn),
            SeverityLegendItem("INFO", infoCount, SeverityBadgeTone.Info),
            SeverityLegendItem("DEBUG", debugCount, SeverityBadgeTone.Debug),
        )

    /** A header followed by its rows, so every header's count matches what renders beneath it. */
    private fun minuteGroup(
        utcMinuteId: String,
        minute: String,
        rows: List<LogRowUi>,
    ): List<LogViewerListItem> =
        buildList {
            add(
                LogViewerListItem.MinuteHeader(
                    stableKey = minuteHeaderKey(utcMinuteId),
                    minute = minute,
                    itemCount = rows.size,
                ),
            )
            rows.forEach { row -> add(LogViewerListItem.LogRow(stableKey = logRowKey(row.id), row = row)) }
        }

    private fun row(
        id: String,
        severityLabel: String,
        severityTone: SeverityBadgeTone,
        tagLabel: String,
        message: String,
        time: String,
    ): LogRowUi =
        LogRowUi(
            id = id,
            severityLabel = severityLabel,
            severityTone = severityTone,
            tagLabel = tagLabel,
            message = message,
            time = time,
        )
}

/** The fixture states a preview, a snapshot test, or the launched app can ask for. */
internal enum class LogViewerFixture {
    Loading,
    Error,
    AllLogs,
    Filtered,
    FilteredEmpty,
}

/**
 * Resolves a [LogViewerFixture] into screen state, reading the display copy from feature resources
 * so the fixtures themselves stay free of hardcoded UI strings.
 */
@Composable
internal fun logViewerFixtureState(fixture: LogViewerFixture): LogViewerUiState =
    when (fixture) {
        LogViewerFixture.Loading -> {
            LogViewerUiState.Loading
        }

        LogViewerFixture.Error -> {
            LogViewerFixtures.error(
                title = stringResource(R.string.log_viewer_error_title),
                message = stringResource(R.string.log_viewer_error_message),
            )
        }

        LogViewerFixture.AllLogs -> {
            LogViewerFixtures.allLogsContent(
                resultCountLabel = resultCountLabel(LogViewerFixtures.ALL_LOGS_RESULT_COUNT),
                sortLabel = stringResource(R.string.log_viewer_sort_newest_first),
            )
        }

        LogViewerFixture.Filtered -> {
            LogViewerFixtures.filteredContent(
                resultCountLabel = resultCountLabel(LogViewerFixtures.FILTERED_RESULT_COUNT),
                sortLabel = stringResource(R.string.log_viewer_sort_newest_first),
            )
        }

        LogViewerFixture.FilteredEmpty -> {
            LogViewerFixtures.filteredEmptyContent(
                resultCountLabel = resultCountLabel(count = 0),
                sortLabel = stringResource(R.string.log_viewer_sort_newest_first),
            )
        }
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
