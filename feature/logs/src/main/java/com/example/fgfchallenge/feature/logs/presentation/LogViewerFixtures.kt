package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.SeveritySummaryUi
import com.example.fgfchallenge.feature.logs.presentation.model.minuteHeaderBetween
import com.example.fgfchallenge.feature.logs.presentation.model.toListItem
import com.example.fgfchallenge.feature.logs.presentation.model.toLogDetailsUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

/**
 * Sample screen states and paged rows used by the previews and the Paparazzi goldens.
 *
 * The app itself no longer uses them: `LogViewerViewModel` reads live repository data, and the
 * screen's two inputs — bounded state and a `PagingData` stream — are exactly the two things this
 * file fabricates.
 *
 * The rows start as [LogEntry] values, the same type the repository emits, and reach the list
 * through the production `toListItem`/`minuteHeaderBetween` mapping rather than a parallel
 * hand-written one. A golden therefore pins the real formatting: real UTC `ss.SSS` row times, real
 * minute headers, real severity tones. The severity summaries are real too — the all-logs counts
 * are the supplied dataset's distribution and the filtered counts are the wireframe's `network`
 * search — while the row lists are deliberately short representative samples, so a displayed total
 * of `5,000 matching` describes the dataset the screen shows in the app, not the number of rows
 * here.
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

    /** Launch refresh in flight: skeletons, and no summary to report yet. */
    val loadingState: LogViewerUiState = LogViewerUiState(refresh = LogViewerRefreshState.InProgress)

    /** A completed refresh with the whole snapshot matching. */
    fun allLogsState(): LogViewerUiState =
        LogViewerUiState(
            refresh = LogViewerRefreshState.Complete,
            summary = LogViewerSummaryState.Ready(allLogsSummary),
        )

    fun filteredState(): LogViewerUiState =
        allLogsState().copy(
            query = FILTERED_QUERY,
            summary = LogViewerSummaryState.Ready(filteredSummary),
        )

    fun filteredEmptyState(): LogViewerUiState =
        allLogsState().copy(
            query = NONMATCHING_QUERY,
            summary = LogViewerSummaryState.Ready(filteredEmptySummary),
        )

    /** A retryable launch failure the user has not dismissed, so the modal is up. */
    fun errorState(): LogViewerUiState = LogViewerUiState(refresh = LogViewerRefreshState.Failed())

    /**
     * The same failure after the modal was dismissed, over the snapshot Room kept: the counts and
     * rows are the previous refresh's, and the notice says so.
     */
    fun staleSnapshotState(): LogViewerUiState = allLogsState().copy(refresh = LogViewerRefreshState.Failed(dismissed = true))

    /** The first entry of the populated fixture, so callers can open its sheet without a lookup. */
    fun firstAllLogsEntry(): LogEntry = allLogsEntries.first()

    /** The details the repository lookup produces for [firstAllLogsEntry]. */
    fun firstAllLogsDetails(): LogDetailsUi = firstAllLogsEntry().toLogDetailsUi()

    val allLogsItems: List<LogViewerListItem> = allLogsEntries.toGroupedItems()

    val filteredItems: List<LogViewerListItem> = filteredEntries.toGroupedItems()

    /**
     * The paged rows for [fixture], as the one-generation stream a `LazyPagingItems` collects.
     *
     * The load states have to be spelled out: a static `PagingData` reports nothing on its own, so
     * without them the list would sit in a permanent refresh and never render its rows. Terminal on
     * every side means "one complete page, nothing more to append", which is what every fixture
     * except the two append states describes.
     */
    fun pagedItems(fixture: LogViewerFixture): Flow<PagingData<LogViewerListItem>> =
        when (fixture) {
            // No rows have loaded yet in either case: the launch refresh is still deciding whether
            // there is a current snapshot to query at all.
            LogViewerFixture.Loading, LogViewerFixture.Error -> pagingDataOf(emptyList())

            LogViewerFixture.AllLogs -> pagingDataOf(allLogsItems)

            LogViewerFixture.Filtered -> pagingDataOf(filteredItems)

            LogViewerFixture.FilteredEmpty -> pagingDataOf(emptyList())

            LogViewerFixture.StaleSnapshot -> pagingDataOf(allLogsItems)

            LogViewerFixture.AppendLoading -> pagingDataOf(allLogsItems, append = LoadState.Loading)

            LogViewerFixture.AppendError -> pagingDataOf(allLogsItems, append = LoadState.Error(AppendFailure))
        }

    private fun pagingDataOf(
        items: List<LogViewerListItem>,
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
    ): Flow<PagingData<LogViewerListItem>> =
        flowOf(
            PagingData.from(
                data = items,
                sourceLoadStates =
                    LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = true),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = append,
                    ),
            ),
        )

    /** Never surfaced: the append-failure UI reports its own copy rather than an exception. */
    private object AppendFailure : Throwable("Fixture append failure")

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
}

// Newest first, matching the default sort. The IDs echo each entry's minute and `ss.SSS` so a
// golden, an interaction test, and the row it names stay readable together.
private val allLogsEntries: List<LogEntry> =
    listOf(
        entry("1711-58123", "17:11:58.123", Severity.ERROR, "network", "Connection timed out", 3_245),
        entry("1711-46204", "17:11:46.204", Severity.FATAL, "auth", "Auth service unreachable", 5_012),
        entry("1711-37812", "17:11:37.812", Severity.WARN, "cache", "Cache miss", 128, aiGenerated = true),
        entry("1711-21439", "17:11:21.439", Severity.INFO, "network", "Request completed", 412),
        entry("1711-11098", "17:11:11.098", Severity.DEBUG, "cache", "Cache lookup key=1234", 12),
        entry("1710-59384", "17:10:59.384", Severity.ERROR, "network", "DNS resolution failed", 1_284),
        entry("1710-48660", "17:10:48.660", Severity.WARN, "auth", "Token expiring soon", 96),
        entry("1710-33215", "17:10:33.215", Severity.INFO, "cache", "Cache write success", 34),
        entry("1710-21078", "17:10:21.078", Severity.DEBUG, "network", "Retry attempt 1/3", 802),
        entry("1710-07026", "17:10:07.026", Severity.INFO, "auth", "User login success", 268),
        entry("1709-45672", "17:09:45.672", Severity.WARN, "network", "High latency detected", 2_190),
    )

// Every entry carries the `network` tag, so the sample stays consistent with FILTERED_QUERY.
private val filteredEntries: List<LogEntry> =
    listOf(
        entry("1711-58123", "17:11:58.123", Severity.ERROR, "network", "Connection timed out", 3_245),
        entry("1711-24673", "17:11:24.673", Severity.WARN, "network", "Slow response detected", 1_760),
        entry("1711-21121", "17:11:21.121", Severity.INFO, "network", "Request completed", 412),
        entry("1710-59384", "17:10:59.384", Severity.ERROR, "network", "DNS resolution failed", 1_284),
        entry("1710-21087", "17:10:21.087", Severity.DEBUG, "network", "Retry attempt 1/3", 802),
        entry("1710-11011", "17:10:11.011", Severity.INFO, "network", "Connection established", 155),
        entry("1709-45672", "17:09:45.672", Severity.WARN, "network", "High latency detected", 2_190),
    )

/**
 * Flattens entries the way the Paging transformation does, through the same two functions.
 *
 * Materializing the whole list is exactly what production must not do, which is why this is a
 * fixture: a short sample is what a preview and a golden need, and reusing the production rule is
 * what keeps them honest about the result.
 */
private fun List<LogEntry>.toGroupedItems(): List<LogViewerListItem> {
    val rows = map(LogEntry::toListItem)
    return buildList {
        rows.forEachIndexed { index, row ->
            minuteHeaderBetween(rows.getOrNull(index - 1), row)?.let(::add)
            add(row)
        }
    }
}

private fun entry(
    id: String,
    utcTimeOfDay: String,
    severity: Severity,
    tag: String,
    message: String,
    latencyMs: Long,
    aiGenerated: Boolean = false,
): LogEntry =
    LogEntry(
        id = id,
        timestamp = Instant.parse("2025-05-22T${utcTimeOfDay}Z"),
        severity = severity,
        tag = tag,
        message = message,
        latencyMs = latencyMs,
        isAiGenerated = aiGenerated,
        sessionId = LogViewerFixtures.SESSION_ID,
    )

/** The fixture states a preview or a snapshot test can ask for. */
internal enum class LogViewerFixture {
    Loading,
    Error,
    AllLogs,
    Filtered,
    FilteredEmpty,

    /** A dismissed refresh failure over the retained snapshot, where retry is all that is left. */
    StaleSnapshot,

    /** Loaded rows plus a page being appended, which no settled fixture can show. */
    AppendLoading,

    /** Loaded rows plus a failed append: the rows stay, the footer offers Retry. */
    AppendError,
}

/** Resolves a [LogViewerFixture] into the bounded screen state half of the screen's input. */
internal fun logViewerFixtureState(fixture: LogViewerFixture): LogViewerUiState =
    when (fixture) {
        LogViewerFixture.Loading -> {
            LogViewerFixtures.loadingState
        }

        LogViewerFixture.Error -> {
            LogViewerFixtures.errorState()
        }

        LogViewerFixture.StaleSnapshot -> {
            LogViewerFixtures.staleSnapshotState()
        }

        LogViewerFixture.AllLogs, LogViewerFixture.AppendLoading, LogViewerFixture.AppendError -> {
            LogViewerFixtures.allLogsState()
        }

        LogViewerFixture.Filtered -> {
            LogViewerFixtures.filteredState()
        }

        LogViewerFixture.FilteredEmpty -> {
            LogViewerFixtures.filteredEmptyState()
        }
    }

/**
 * Resolves a [LogViewerFixture] into the paged half, collected exactly as the app collects the
 * ViewModel's stream, so a preview and a golden exercise the real `LazyPagingItems` path rather
 * than a list rendered to look like one.
 */
@Composable
internal fun logViewerFixtureItems(fixture: LogViewerFixture): LazyPagingItems<LogViewerListItem> =
    LogViewerFixtures.pagedItems(fixture).collectAsLazyPagingItems()
