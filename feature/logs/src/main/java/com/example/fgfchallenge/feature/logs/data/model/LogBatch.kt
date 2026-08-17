package com.example.fgfchallenge.feature.logs.data.model

import java.time.Instant

/**
 * The immutable application models `LogsRepository` exposes. They are the only log types that
 * cross the data boundary: DTOs, Retrofit, and OkHttp stay inside `data/remote`.
 *
 * Timestamps are UTC [Instant]s and remain UTC for the whole feature; formatting belongs to
 * presentation.
 */
internal data class LogBatch(
    /**
     * The `total_count` the response claims. It is kept for diagnostics only — it may disagree
     * with [entries], and consumers always count [entries] instead.
     */
    val reportedTotalCount: Int,
    /** Response-level session identifier; the payload has no per-entry session. */
    val sessionId: String,
    val entries: List<LogEntry>,
)

internal data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val severity: Severity,
    val tag: String,
    val message: String,
    val metadata: LogMetadata,
)

internal data class LogMetadata(
    val latencyMs: Long,
    val isAiGenerated: Boolean,
)

/**
 * The five severities the supplied dataset uses, plus [UNKNOWN] for forward compatibility with
 * values the payload may introduce later.
 */
internal enum class Severity {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    UNKNOWN,
}
