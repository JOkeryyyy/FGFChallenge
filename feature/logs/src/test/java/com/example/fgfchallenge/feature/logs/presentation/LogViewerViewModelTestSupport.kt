package com.example.fgfchallenge.feature.logs.presentation

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.data.repository.LogsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Instant

/**
 * Test doubles for `LogViewerViewModel`: the `Dispatchers.Main` replacement its `viewModelScope`
 * needs, and a `LogsRepository` fake that records the queries it is asked for.
 *
 * The fake deliberately does not filter anything. What these tests verify is coordination — which
 * query value reaches which repository operation, and when a result is discarded as obsolete —
 * and reimplementing the predicate here would only assert that two fakes agree. The real predicate
 * is covered against real SQLite in the DAO and repository tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        // Unconfined, so the collection the ViewModel's `init` starts is running before the test
        // body's first action rather than at the next scheduler pass.
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

internal class FakeLogsRepository : LogsRepository {
    /** Every query each read was called with, in order, so replacement can be asserted directly. */
    val pagedQueries = mutableListOf<LogQuery>()
    val summaryQueries = mutableListOf<LogQuery>()

    /** Rows the paged stream serves for a query. The default is an empty result set. */
    var rowsFor: (LogQuery) -> List<LogEntry> = { emptyList() }

    /**
     * One replayed channel per query, created on demand.
     *
     * A query nothing was emitted for stays silent, which is how a test holds the summary in its
     * pending state; the replay buffer means [emitSummary] works whether the ViewModel has started
     * collecting yet or not.
     */
    private val summaryFlows = mutableMapOf<LogQuery, MutableSharedFlow<LogSummary>>()

    override suspend fun refreshSnapshot(): EmptyResult<LogsDataError> = Result.Success(Unit)

    override fun pagedLogs(query: LogQuery): Flow<PagingData<LogEntry>> {
        pagedQueries += query
        // The load states have to be spelled out: without them a static `PagingData` never reports
        // a settled refresh, and anything waiting for the generation to load — `asSnapshot`, a
        // real list — waits forever. Terminal on every side means "one complete page, no more".
        return flowOf(PagingData.from(rowsFor(query), sourceLoadStates = LOADED))
    }

    override fun summary(query: LogQuery): Flow<LogSummary> {
        summaryQueries += query
        return summaryFlow(query)
    }

    /**
     * Snapshot metadata for the filter controls, as a hot flow so a test can deliver options after
     * the ViewModel has already started — which is what happens in the app, where Room answers
     * after the screen is on-screen.
     */
    private val options = MutableStateFlow(LogFilterOptions())

    override fun filterOptions(): Flow<LogFilterOptions> = options

    fun emitFilterOptions(value: LogFilterOptions) {
        options.value = value
    }

    override suspend fun logById(id: String): Result<LogEntry?, LogsDataError> =
        Result.Success(rowsFor(LogQuery()).firstOrNull { it.id == id })

    /** Publishes [summary] to whichever collection is reading [query]. */
    fun emitSummary(
        query: LogQuery,
        summary: LogSummary,
    ) {
        summaryFlow(query).tryEmit(summary)
    }

    private fun summaryFlow(query: LogQuery): MutableSharedFlow<LogSummary> =
        summaryFlows.getOrPut(query) {
            // Spare capacity plus DROP_OLDEST is what keeps `tryEmit` from ever failing, so a test
            // never has to reason about buffer pressure.
            MutableSharedFlow(replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }

    private companion object {
        val LOADED =
            LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = true),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true),
            )
    }
}

/** A stored entry with only the fields a coordination test cares about spelled out. */
internal fun testLogEntry(
    id: String,
    severity: Severity = Severity.INFO,
    message: String = "message $id",
): LogEntry =
    LogEntry(
        id = id,
        timestamp = Instant.parse("2025-05-22T17:11:58.123Z"),
        severity = severity,
        tag = "network",
        message = message,
        latencyMs = 128,
        isAiGenerated = false,
        sessionId = LogViewerFixtures.SESSION_ID,
    )
