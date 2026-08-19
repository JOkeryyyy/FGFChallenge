package com.example.fgfchallenge.feature.logs.data.model

import java.time.Instant
import java.util.Locale

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

/**
 * One log record as the rest of the app sees it.
 *
 * The fields are flat rather than nesting the payload's `metadata` object: they are all equally
 * queryable columns once persisted, and a snapshot is ~100,000 records, where a second object per
 * row buys nothing. [sessionId] comes from the response envelope and is carried on every entry so
 * a details lookup by ID needs no second read.
 */
data class LogEntry(
    val id: String,
    val timestamp: Instant,
    val severity: Severity,
    val tag: String,
    val message: String,
    val latencyMs: Long,
    val isAiGenerated: Boolean,
    val sessionId: String,
)

/**
 * The five severities the supplied dataset uses, plus [UNKNOWN] for forward compatibility with
 * values the payload may introduce later.
 */
enum class Severity {
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL,
    UNKNOWN,
    ;

    internal companion object {
        /**
         * Resolves a severity name from either boundary — the remote payload or a stored row.
         *
         * An unrecognized value is [UNKNOWN] rather than a failure: a new severity in the payload
         * is forward compatibility, not a corrupt snapshot. It stays valid data that counts toward
         * the total, and is never folded into the error numerator.
         */
        fun fromNameOrUnknown(value: String): Severity {
            val normalized = value.trim().uppercase(Locale.ROOT)
            return entries.firstOrNull { it.name == normalized } ?: UNKNOWN
        }
    }
}
