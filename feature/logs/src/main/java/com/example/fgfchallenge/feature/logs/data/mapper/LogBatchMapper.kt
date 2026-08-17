package com.example.fgfchallenge.feature.logs.data.mapper

import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogBatch
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogMetadata
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.data.remote.LogEntryDto
import com.example.fgfchallenge.feature.logs.data.remote.LogsPayloadDto
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Turns a decoded [LogsPayloadDto] into the immutable [LogBatch] the repository exposes, and
 * rejects payloads whose values decode but violate the documented semantics.
 *
 * The split is deliberate: structural problems (missing or null keys) already fail during
 * decoding as `LogsDataError.Serialization`, so everything here is a semantic check reported as
 * [LogsDataError.Schema]. A partially valid payload is never returned — an invalid entry fails
 * the whole load rather than silently dropping rows the user would never know were missing.
 */
internal fun LogsPayloadDto.toLogBatch(): Result<LogBatch, LogsDataError> {
    if (totalCount < 0 || sessionId.isBlank()) return Result.Error(LogsDataError.Schema)

    val mapped = ArrayList<LogEntry>(entries.size)
    for (dto in entries) {
        mapped += dto.toLogEntryOrNull() ?: return Result.Error(LogsDataError.Schema)
    }

    // A reported/actual count mismatch is not a failure: consumers count `entries`, and the
    // usable data is still complete. `reportedTotalCount` is preserved only for diagnostics.
    return Result.Success(
        LogBatch(
            reportedTotalCount = totalCount,
            sessionId = sessionId,
            entries = mapped,
        ),
    )
}

private fun LogEntryDto.toLogEntryOrNull(): LogEntry? {
    if (id.isBlank() || tag.isBlank() || message.isBlank()) return null
    if (metadata.latencyMs < 0) return null
    val parsedTimestamp = timestamp.toInstantOrNull() ?: return null

    return LogEntry(
        id = id,
        timestamp = parsedTimestamp,
        // Severity is normalized rather than validated: an unrecognized value is a forward
        // compatibility case, not a corrupt payload.
        severity = severity.toSeverity(),
        tag = tag,
        message = message,
        metadata =
            LogMetadata(
                latencyMs = metadata.latencyMs,
                isAiGenerated = metadata.isAiGenerated,
            ),
    )
}

private fun String.toInstantOrNull(): Instant? =
    try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }

private fun String.toSeverity(): Severity {
    val normalized = trim().uppercase(Locale.ROOT)
    return Severity.entries.firstOrNull { it.name == normalized } ?: Severity.UNKNOWN
}
