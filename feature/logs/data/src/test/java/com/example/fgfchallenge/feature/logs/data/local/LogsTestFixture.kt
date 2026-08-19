package com.example.fgfchallenge.feature.logs.data.local

import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadResult
import com.example.fgfchallenge.feature.logs.data.mapper.toLogSummary
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * The snapshot the query tests run against, plus the two helpers that execute a [LogQuery].
 *
 * Six rows are enough to distinguish every documented condition: two share a timestamp so the
 * ID tie-break is observable, one message carries a literal `%` and another a literal `_` so
 * wildcard escaping can be told apart from wildcard matching, and "timeout" appears in three
 * different cases so search case-insensitivity is not accidentally satisfied by exact matches.
 */
internal object LogsTestFixture {
    val ERROR_NETWORK_TIMEOUT =
        entity(
            id = "log-1",
            timestamp = "2026-08-16T17:10:00Z",
            severity = "ERROR",
            tag = "network",
            message = "Connection timeout",
            latencyMs = 2040,
            isAiGenerated = true,
        )

    /** Shares [ERROR_NETWORK_TIMEOUT]'s timestamp, so ordering has to fall back to the ID. */
    val WARN_DB_RETRY =
        entity(
            id = "log-2",
            timestamp = "2026-08-16T17:10:00Z",
            severity = "WARN",
            tag = "db",
            message = "Retry scheduled",
            latencyMs = 120,
            isAiGenerated = false,
        )

    /** The only row containing a literal `%`. */
    val INFO_UI_FRAMES =
        entity(
            id = "log-3",
            timestamp = "2026-08-16T17:10:30.500Z",
            severity = "INFO",
            tag = "ui",
            message = "Rendered 100% of frames",
            latencyMs = 12,
            isAiGenerated = true,
        )

    /** The only row containing a literal `_`. */
    val DEBUG_CACHE_MISS =
        entity(
            id = "log-4",
            timestamp = "2026-08-16T17:11:00Z",
            severity = "DEBUG",
            tag = "cache",
            message = "cache_miss for key",
            latencyMs = 300,
            isAiGenerated = false,
        )

    val FATAL_NETWORK_TIMEOUT =
        entity(
            id = "log-5",
            timestamp = "2026-08-16T17:12:00Z",
            severity = "FATAL",
            tag = "network",
            message = "Timeout escalated",
            latencyMs = 5000,
            isAiGenerated = true,
        )

    val ERROR_DB_TIMEOUT =
        entity(
            id = "log-6",
            timestamp = "2026-08-16T17:13:00Z",
            severity = "ERROR",
            tag = "db",
            message = "TIMEOUT while flushing",
            latencyMs = 45,
            isAiGenerated = false,
        )

    val SNAPSHOT =
        listOf(
            ERROR_NETWORK_TIMEOUT,
            WARN_DB_RETRY,
            INFO_UI_FRAMES,
            DEBUG_CACHE_MISS,
            FATAL_NETWORK_TIMEOUT,
            ERROR_DB_TIMEOUT,
        )

    fun entity(
        id: String,
        timestamp: String,
        severity: String,
        tag: String,
        message: String,
        latencyMs: Long,
        isAiGenerated: Boolean,
        sessionId: String = SESSION_ID,
    ): LogEntity =
        LogEntity(
            id = id,
            timestampEpochMillis = Instant.parse(timestamp).toEpochMilli(),
            severity = severity,
            tag = tag,
            message = message,
            latencyMs = latencyMs,
            isAiGenerated = isAiGenerated,
            sessionId = sessionId,
        )

    const val SESSION_ID = "session-666"
}

/**
 * Loads the first page for [query] and returns the matching IDs in the order the list would show
 * them. [loadSize] defaults well above the fixture size so a test asserts the complete result
 * unless it is deliberately exercising a page boundary.
 */
internal suspend fun LogsDao.matchingIds(
    query: LogQuery,
    loadSize: Int = 50,
): List<String> {
    val page =
        pagedLogs(LogQuerySql.pagedSelect(query)).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = loadSize,
                placeholdersEnabled = false,
            ),
        )
    return (page as LoadResult.Page).data.map { it.id }
}

/** Runs the aggregate half of [query] — the same predicate, counted rather than listed. */
internal suspend fun LogsDao.summaryOf(query: LogQuery): LogSummary =
    severityCounts(LogQuerySql.severityCountSelect(query)).first().toLogSummary()
