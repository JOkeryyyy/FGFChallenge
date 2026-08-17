package com.example.fgfchallenge.feature.logs.presentation.model

import com.example.fgfchallenge.core.designsystem.model.AiGeneratedChoice
import com.example.fgfchallenge.core.designsystem.model.LogFilterDateTimeUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterLatencyUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterOptionUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetUi
import com.example.fgfchallenge.feature.logs.data.model.KNOWN_SEVERITIES
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.Severity
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * The translation between the filter draft the ViewModel owns and the design system's filter sheet,
 * in both directions.
 *
 * The two speak different languages by design: the sheet knows chips, UTC epoch milliseconds, and
 * `Float` slider positions, while the feature knows `Severity`, `LocalDate`, and millisecond
 * latencies. Concentrating the mapping here keeps `java.time` and the repository's models out of
 * `:core:designsystem`, and keeps it all host-testable as plain functions.
 *
 * The locale is fixed for the same reason the result count's is: the prototype ships English-only
 * copy, and every golden should render identical text on every machine.
 */

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val latencyFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

/** Renders the draft [LogFilterSelection] against the snapshot metadata the controls need. */
internal fun LogFilterSelection.toFilterSheetUi(options: LogFilterOptions): LogFilterSheetUi =
    LogFilterSheetUi(
        tags =
            options.availableTags.map { tag ->
                LogFilterOptionUi(id = tag, label = tag, selected = tag in tags)
            },
        // The five product-defined choices rather than a data-driven list: a severity absent from
        // the current snapshot is still a filter the user may select.
        severities =
            KNOWN_SEVERITIES.map { severity ->
                LogFilterOptionUi(
                    id = severity.name,
                    label = severity.name,
                    selected = severity in severities,
                )
            },
        aiGenerated = aiGenerated.toSheetChoice(),
        start = dateTimeUi(date = startDateUtc, time = startTimeUtc),
        end = dateTimeUi(date = endDateUtc, time = endTimeUtc),
        latency = options.latencyUi(selection = this),
    )

/** The extent the stored snapshot actually spans, or `null` when there is nothing to slide over. */
internal fun LogFilterOptions.latencyExtent(): LongRange? {
    val minimum = minimumLatencyMs ?: return null
    val maximum = maximumLatencyMs ?: return null
    // A single-valued extent is not a range the slider can express, and no bound inside it could
    // restrict anything, so it is reported as unavailable rather than as a zero-width slider.
    return if (minimum < maximum) minimum..maximum else null
}

internal fun AiGeneratedChoice.toFilterSelection(): AiGeneratedFilter =
    when (this) {
        AiGeneratedChoice.Any -> AiGeneratedFilter.Any
        AiGeneratedChoice.Yes -> AiGeneratedFilter.Yes
        AiGeneratedChoice.No -> AiGeneratedFilter.No
    }

/**
 * Resolves a severity chip's ID back to the severity it was built from. Anything else is `null`:
 * [Severity.UNKNOWN] is a storage fallback rather than a choice the sheet can offer, so an
 * unrecognized ID is dropped instead of quietly becoming a filter nobody selected.
 */
internal fun severityFilterFor(id: String): Severity? = KNOWN_SEVERITIES.firstOrNull { it.name == id }

/** The UTC day [millis] falls in — the pickers report a selected day as its UTC midnight. */
internal fun utcDateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun AiGeneratedFilter.toSheetChoice(): AiGeneratedChoice =
    when (this) {
        AiGeneratedFilter.Any -> AiGeneratedChoice.Any
        AiGeneratedFilter.Yes -> AiGeneratedChoice.Yes
        AiGeneratedFilter.No -> AiGeneratedChoice.No
    }

/**
 * Each value travels twice: formatted for the field, and raw for the picker that opens on it. An
 * unset bound stays `null` all the way through rather than defaulting to today, because an open
 * side of the range means "unrestricted", not "now".
 */
private fun dateTimeUi(
    date: LocalDate?,
    time: LocalTime?,
): LogFilterDateTimeUi =
    LogFilterDateTimeUi(
        dateLabel = date?.format(dateFormatter),
        timeLabel = time?.format(timeFormatter),
        dateUtcMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        hourOfDayUtc = time?.hour,
        minuteOfHourUtc = time?.minute,
    )

/**
 * An unset bound sits at its end of the extent, so an untouched slider spans the whole snapshot.
 * `toLogQuery` reads that same full-width selection back as "no latency predicate", which is what
 * keeps the control's inactive position and the query's inactive form in agreement.
 */
private fun LogFilterOptions.latencyUi(selection: LogFilterSelection): LogFilterLatencyUi? {
    val extent = latencyExtent() ?: return null
    val minimum = (selection.minimumLatencyMs ?: extent.first).coerceIn(extent.first, extent.last)
    // Coerced against `minimum`, not the extent's start: a reversed draft would otherwise reach
    // RangeSlider as a range whose start exceeds its end.
    val maximum = (selection.maximumLatencyMs ?: extent.last).coerceIn(minimum, extent.last)
    return LogFilterLatencyUi(
        bounds = extent.first.toFloat()..extent.last.toFloat(),
        selection = minimum.toFloat()..maximum.toFloat(),
        lowerBoundLabel = latencyFormat.format(extent.first),
        upperBoundLabel = latencyFormat.format(extent.last),
        selectionLabel = "${latencyFormat.format(minimum)} – ${latencyFormat.format(maximum)} ms",
    )
}
