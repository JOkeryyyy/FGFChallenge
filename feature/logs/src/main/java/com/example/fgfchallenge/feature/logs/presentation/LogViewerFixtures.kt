package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogRowUi
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.logRowKey
import com.example.fgfchallenge.feature.logs.presentation.model.minuteHeaderKey
import java.text.NumberFormat
import java.util.Locale

/**
 * Sample screen states used by the previews, the Paparazzi goldens, and — until networking and
 * processing land — `LogViewerViewModel` itself.
 *
 * The severity summaries are real: the all-logs counts are the supplied dataset's distribution and
 * the filtered counts are the wireframe's `network` search. The row lists are deliberately short
 * representative samples, so a displayed total of `5,000 results` describes the dataset the screen
 * will show once networking lands, not the number of rows in this fixture.
 *
 * Every state here is a plain value rather than a `@Composable` call, so the ViewModel can produce
 * it. Only the error copy still needs resources, which is why [logViewerFixtureState] resolves it.
 */
internal object LogViewerFixtures {
    const val ALL_LOGS_RESULT_COUNT: Int = 5_000
    const val FILTERED_RESULT_COUNT: Int = 718
    const val FILTERED_QUERY: String = "network"

    /** No supplied message, tag, or severity contains this, so it stands in for a dead-end search. */
    const val NONMATCHING_QUERY: String = "kubernetes"

    /** The payload carries one session for the whole response, so every entry reports the same ID. */
    const val SESSION_ID: String = "sess-7f3a9b21-7cd4-4d6d-9a12-3f5e7d9a1b2c"

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

    val loadingState: LogViewerUiState = LogViewerUiState(loadState = LogViewerLoadState.Loading)

    /** The ViewModel's starting point and what Retry and error dismissal return to. */
    fun allLogsState(): LogViewerUiState =
        LogViewerUiState(
            query = "",
            sortOrder = LogSortOrder.NewestFirst,
            selectedLog = null,
            loadState =
                LogViewerLoadState.Content(
                    resultCount = ALL_LOGS_RESULT_COUNT,
                    severitySummary = allLogsSummary,
                    items = allLogsItems,
                ),
        )

    fun filteredState(): LogViewerUiState =
        LogViewerUiState(
            query = FILTERED_QUERY,
            loadState =
                LogViewerLoadState.Content(
                    resultCount = FILTERED_RESULT_COUNT,
                    severitySummary = filteredSummary,
                    items = filteredItems,
                ),
        )

    fun filteredEmptyState(): LogViewerUiState =
        LogViewerUiState(
            query = NONMATCHING_QUERY,
            loadState =
                LogViewerLoadState.Content(
                    resultCount = 0,
                    severitySummary = filteredEmptySummary,
                    items = emptyList(),
                ),
        )

    fun errorState(
        title: String,
        message: String,
    ): LogViewerUiState = LogViewerUiState(loadState = LogViewerLoadState.Error(title = title, message = message))

    /** The first row of the populated fixture, so callers can open its sheet without a lookup. */
    fun firstAllLogsRow(): LogViewerListItem.LogRow = allLogsItems.filterIsInstance<LogViewerListItem.LogRow>().first()

    // Newest minute first, and newest row first inside each minute, matching the default sort.
    private val allLogsItems: List<LogViewerListItem> =
        minuteGroup(
            utcMinuteId = "2025-05-22T17:11Z",
            minute = "17:11",
            rows =
                listOf(
                    row("1711-58123", "ERROR", SeverityBadgeTone.Error, "network", "Connection timed out", "58.123", 3_245),
                    row("1711-46204", "FATAL", SeverityBadgeTone.Fatal, "auth", "Auth service unreachable", "46.204", 5_012),
                    row("1711-37812", "WARN", SeverityBadgeTone.Warn, "cache", "Cache miss", "37.812", 128, true),
                    row("1711-21439", "INFO", SeverityBadgeTone.Info, "network", "Request completed", "21.439", 412),
                    row("1711-11098", "DEBUG", SeverityBadgeTone.Debug, "cache", "Cache lookup key=1234", "11.098", 12),
                ),
        ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:10Z",
                minute = "17:10",
                rows =
                    listOf(
                        row("1710-59384", "ERROR", SeverityBadgeTone.Error, "network", "DNS resolution failed", "59.384", 1_284),
                        row("1710-48660", "WARN", SeverityBadgeTone.Warn, "auth", "Token expiring soon", "48.660", 96),
                        row("1710-33215", "INFO", SeverityBadgeTone.Info, "cache", "Cache write success", "33.215", 34),
                        row("1710-21078", "DEBUG", SeverityBadgeTone.Debug, "network", "Retry attempt 1/3", "21.078", 802),
                        row("1710-07026", "INFO", SeverityBadgeTone.Info, "auth", "User login success", "07.026", 268),
                    ),
            ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:09Z",
                minute = "17:09",
                rows =
                    listOf(
                        row("1709-45672", "WARN", SeverityBadgeTone.Warn, "network", "High latency detected", "45.672", 2_190),
                    ),
            )

    // Every row matches FILTERED_QUERY on its tag, so the sample stays consistent with the query.
    private val filteredItems: List<LogViewerListItem> =
        minuteGroup(
            utcMinuteId = "2025-05-22T17:11Z",
            minute = "17:11",
            rows =
                listOf(
                    row("1711-58123", "ERROR", SeverityBadgeTone.Error, "network", "Connection timed out", "58.123", 3_245),
                    row("1711-24673", "WARN", SeverityBadgeTone.Warn, "network", "Slow response detected", "24.673", 1_760),
                    row("1711-21121", "INFO", SeverityBadgeTone.Info, "network", "Request completed", "21.121", 412),
                ),
        ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:10Z",
                minute = "17:10",
                rows =
                    listOf(
                        row("1710-59384", "ERROR", SeverityBadgeTone.Error, "network", "DNS resolution failed", "59.384", 1_284),
                        row("1710-21087", "DEBUG", SeverityBadgeTone.Debug, "network", "Retry attempt 1/3", "21.087", 802),
                        row("1710-11011", "INFO", SeverityBadgeTone.Info, "network", "Connection established", "11.011", 155),
                    ),
            ) +
            minuteGroup(
                utcMinuteId = "2025-05-22T17:09Z",
                minute = "17:09",
                rows =
                    listOf(
                        row("1709-45672", "WARN", SeverityBadgeTone.Warn, "network", "High latency detected", "45.672", 2_190),
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
        rows: List<FixtureRow>,
    ): List<LogViewerListItem> =
        buildList {
            add(
                LogViewerListItem.MinuteHeader(
                    stableKey = minuteHeaderKey(utcMinuteId),
                    minute = minute,
                    itemCount = rows.size,
                ),
            )
            rows.forEach { fixtureRow ->
                add(
                    LogViewerListItem.LogRow(
                        stableKey = logRowKey(fixtureRow.row.id),
                        row = fixtureRow.row,
                        details = fixtureRow.toDetails(utcMinuteId),
                    ),
                )
            }
        }

    private fun row(
        id: String,
        severityLabel: String,
        severityTone: SeverityBadgeTone,
        tagLabel: String,
        message: String,
        time: String,
        latencyMs: Int,
        aiGenerated: Boolean = false,
    ): FixtureRow =
        FixtureRow(
            row =
                LogRowUi(
                    id = id,
                    severityLabel = severityLabel,
                    severityTone = severityTone,
                    tagLabel = tagLabel,
                    message = message,
                    time = time,
                ),
            latencyMs = latencyMs,
            aiGenerated = aiGenerated,
        )

    /**
     * One row plus the two values only the details sheet renders, kept out of [LogRowUi] because the
     * list itself never shows them.
     */
    private data class FixtureRow(
        val row: LogRowUi,
        val latencyMs: Int,
        val aiGenerated: Boolean,
    )

    /**
     * Details are derived from the row and its enclosing minute rather than restated, which is what
     * keeps `logId` equal to the row's ID and the full timestamp consistent with the group header.
     *
     * The formatted latency and the Yes/No flag are display values the mapping milestone will
     * produce from application models; the locale is fixed for the same reason the result count's
     * is — identical text in every golden on every machine.
     */
    private fun FixtureRow.toDetails(utcMinuteId: String): LogDetailsUi =
        LogDetailsUi(
            severityLabel = row.severityLabel,
            severityTone = row.severityTone,
            message = row.message,
            tag = row.tagLabel,
            timestampUtc = "${utcMinuteId.removeSuffix("Z")}:${row.time}Z",
            latency = "${NumberFormat.getIntegerInstance(Locale.US).format(latencyMs)} ms",
            aiGenerated = if (aiGenerated) "Yes" else "No",
            logId = row.id,
            sessionId = SESSION_ID,
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
 * Resolves a [LogViewerFixture] into screen state, reading the error copy from feature resources so
 * the fixtures themselves stay free of hardcoded UI strings.
 */
@Composable
internal fun logViewerFixtureState(fixture: LogViewerFixture): LogViewerUiState =
    when (fixture) {
        LogViewerFixture.Loading -> {
            LogViewerFixtures.loadingState
        }

        LogViewerFixture.Error -> {
            LogViewerFixtures.errorState(
                title = stringResource(R.string.log_viewer_error_title),
                message = stringResource(R.string.log_viewer_error_message),
            )
        }

        LogViewerFixture.AllLogs -> {
            LogViewerFixtures.allLogsState()
        }

        LogViewerFixture.Filtered -> {
            LogViewerFixtures.filteredState()
        }

        LogViewerFixture.FilteredEmpty -> {
            LogViewerFixtures.filteredEmptyState()
        }
    }
