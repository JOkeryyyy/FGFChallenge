package com.example.fgfchallenge.feature.logs.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The persisted representation of one log record: every field the viewer can query, filter,
 * order, or show in details, so no screen read ever has to consult the network.
 *
 * This type never leaves `data` — the repository exposes `LogEntry` instead.
 */
@Entity(
    tableName = LogEntity.TABLE_NAME,
    indices = [
        // Ordering and the UTC range filter both drive off the timestamp, so it is the one
        // column indexed for traversal rather than selection.
        Index(value = [LogEntity.COLUMN_TIMESTAMP]),
        Index(value = [LogEntity.COLUMN_SEVERITY]),
        Index(value = [LogEntity.COLUMN_TAG]),
    ],
)
internal data class LogEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: String,
    /**
     * Epoch milliseconds rather than a formatted string: it preserves the source precision, sorts
     * as a UTC instant without parsing, and makes the range filter an integer comparison.
     */
    @ColumnInfo(name = COLUMN_TIMESTAMP)
    val timestampEpochMillis: Long,
    /** The `Severity` name, including `UNKNOWN` for values the payload may introduce later. */
    @ColumnInfo(name = COLUMN_SEVERITY)
    val severity: String,
    @ColumnInfo(name = COLUMN_TAG)
    val tag: String,
    @ColumnInfo(name = COLUMN_MESSAGE)
    val message: String,
    @ColumnInfo(name = COLUMN_LATENCY_MS)
    val latencyMs: Long,
    @ColumnInfo(name = COLUMN_IS_AI_GENERATED)
    val isAiGenerated: Boolean,
    /**
     * Denormalized from the response envelope onto every row. The snapshot is replaced whole, so
     * a separate session table would add a join to every details lookup and buy nothing.
     */
    @ColumnInfo(name = COLUMN_SESSION_ID)
    val sessionId: String,
) {
    /**
     * Table and column names are constants because the paged and aggregate selects are built as
     * raw SQL: a renamed column must not be able to drift away from the queries that use it.
     */
    internal companion object {
        const val TABLE_NAME = "logs"
        const val COLUMN_ID = "id"
        const val COLUMN_TIMESTAMP = "timestamp_epoch_millis"
        const val COLUMN_SEVERITY = "severity"
        const val COLUMN_TAG = "tag"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_LATENCY_MS = "latency_ms"
        const val COLUMN_IS_AI_GENERATED = "is_ai_generated"
        const val COLUMN_SESSION_ID = "session_id"
    }
}
