package com.example.fgfchallenge.feature.logs.data.mapper

import com.example.fgfchallenge.feature.logs.data.local.LatencyBoundsRow
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import com.example.fgfchallenge.feature.logs.data.local.SeverityCountRow
import com.example.fgfchallenge.feature.logs.data.model.KNOWN_SEVERITIES
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import java.time.Instant

/**
 * Converts between the stored [LogEntity] rows and the models the repository exposes.
 *
 * The database boundary is where UTC instants become epoch milliseconds and severities become
 * plain names, so nothing above `data` has to know how a log is stored. These conversions run once
 * per row of a ~100,000-record import, so they stay allocation-light and do no parsing beyond the
 * severity lookup.
 */
internal fun LogEntry.toEntity(): LogEntity =
    LogEntity(
        id = id,
        timestampEpochMillis = timestamp.toEpochMilli(),
        severity = severity.name,
        tag = tag,
        message = message,
        latencyMs = latencyMs,
        isAiGenerated = isAiGenerated,
        sessionId = sessionId,
    )

internal fun LogEntity.toLogEntry(): LogEntry =
    LogEntry(
        id = id,
        timestamp = Instant.ofEpochMilli(timestampEpochMillis),
        severity = Severity.fromNameOrUnknown(severity),
        tag = tag,
        message = message,
        latencyMs = latencyMs,
        isAiGenerated = isAiGenerated,
        sessionId = sessionId,
    )

/**
 * Folds the grouped severity counts into the summary presentation reads.
 *
 * The total is the sum of the same rows the per-severity counts come from, so the two cannot
 * disagree. Every known severity is present even at zero, which keeps the legend a fixed shape;
 * `UNKNOWN` is added only when the snapshot actually contains one.
 */
internal fun List<SeverityCountRow>.toLogSummary(): LogSummary {
    val counted = KNOWN_SEVERITIES.associateWithTo(LinkedHashMap<Severity, Int>()) { 0 }
    var total = 0
    for (row in this) {
        val severity = Severity.fromNameOrUnknown(row.severity)
        counted[severity] = (counted[severity] ?: 0) + row.count
        total += row.count
    }
    return LogSummary(totalCount = total, countBySeverity = counted)
}

/**
 * Combines the two unfiltered option queries. They are separate selects because tags come from a
 * `DISTINCT` scan and the latency extent from a `MIN`/`MAX` aggregate; neither reads whole rows.
 */
internal fun LatencyBoundsRow.toLogFilterOptions(availableTags: List<String>): LogFilterOptions =
    LogFilterOptions(
        availableTags = availableTags,
        minimumLatencyMs = minimumLatencyMs,
        maximumLatencyMs = maximumLatencyMs,
    )
