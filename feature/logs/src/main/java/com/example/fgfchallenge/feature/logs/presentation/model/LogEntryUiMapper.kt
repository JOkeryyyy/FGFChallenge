package com.example.fgfchallenge.feature.logs.presentation.model

import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import java.text.NumberFormat
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Turns the repository's `LogEntry` into the display-ready values the list and the details sheet
 * render.
 *
 * This is the only place a stored value becomes text. Design-system components format nothing, and
 * the mapping runs inside the Paging transformation rather than during composition, so a row is
 * formatted once when its page loads instead of on every recomposition.
 *
 * Every formatter is fixed to UTC and to `Locale.US`: UTC because the whole feature preserves it —
 * a header is the row's minute and the row's `ss.SSS` is the tail of that same minute — and a fixed
 * locale because the prototype ships English-only copy and the visual goldens must render the same
 * text on every machine.
 */

/** The grouping key: one value per UTC minute, and the source of the header's displayed label. */
private val MINUTE_ID_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).withZone(ZoneOffset.UTC)

/** Rows show only the seconds within their minute, since the minute is already in the header. */
private val ROW_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("ss.SSS", Locale.US).withZone(ZoneOffset.UTC)

/** Details show the complete instant, where no enclosing header supplies the missing fields. */
private val DETAILS_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).withZone(ZoneOffset.UTC)

private val LATENCY_FORMAT: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

/** The list row for one entry, tagged with the minute the separator transformation groups it by. */
internal fun LogEntry.toListItem(): LogViewerListItem.LogRow =
    LogViewerListItem.LogRow(
        utcMinuteId = MINUTE_ID_FORMATTER.format(timestamp),
        row =
            LogRowUi(
                id = id,
                severityLabel = severity.name,
                severityTone = severity.toBadgeTone(),
                tagLabel = tag,
                message = message,
                time = ROW_TIME_FORMATTER.format(timestamp),
            ),
    )

/**
 * The details-sheet values for one entry.
 *
 * "Yes"/"No" and the `ms` suffix are literals rather than string resources because this mapping runs
 * off the composition — a Paging transformation and a repository lookup have no `Context`. They are
 * the only UI copy outside `strings.xml`, and localizing them would mean moving the formatting into
 * composition, which is what the architecture places here instead.
 */
internal fun LogEntry.toLogDetailsUi(): LogDetailsUi =
    LogDetailsUi(
        severityLabel = severity.name,
        severityTone = severity.toBadgeTone(),
        message = message,
        tag = tag,
        timestampUtc = DETAILS_TIMESTAMP_FORMATTER.format(timestamp),
        latency = "${LATENCY_FORMAT.format(latencyMs)} ms",
        aiGenerated = if (isAiGenerated) "Yes" else "No",
        logId = id,
        sessionId = sessionId,
    )

/**
 * The minute header that belongs between two adjacent list rows, or `null` when none does.
 *
 * This is the grouping rule itself, kept in one place because two callers apply it to differently
 * shaped data: the Paging separator transformation, which sees one neighbouring pair at a time and
 * never the whole result, and the fixtures, which build a complete short list. Adjacency is all it
 * needs — which is why the paged list gets correct headers across page boundaries without knowing
 * where those boundaries are.
 *
 * `before == null` is the top of the list and takes a header; `after == null` is the bottom and
 * takes none, since a trailing header would introduce a group with no rows under it.
 */
internal fun minuteHeaderBetween(
    before: LogViewerListItem.LogRow?,
    after: LogViewerListItem.LogRow?,
): LogViewerListItem.MinuteHeader? =
    when {
        after == null -> null
        before?.utcMinuteId == after.utcMinuteId -> null
        else -> LogViewerListItem.MinuteHeader(after.utcMinuteId)
    }
