package com.example.fgfchallenge.feature.logs.presentation.model

import com.example.fgfchallenge.core.designsystem.model.AiGeneratedChoice
import com.example.fgfchallenge.core.designsystem.model.LogFilterDateTimeUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterLatencyUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterOptionUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetEvent
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
import kotlin.math.roundToLong

/*
 * The translation between the filter draft and the design system's filter sheet, in both
 * directions.
 *
 * The two speak different languages by design: the sheet knows chips, UTC epoch milliseconds, and
 * `Float` slider positions, while the feature knows `Severity`, `LocalDate`, and millisecond
 * latencies. Concentrating the mapping here keeps `java.time` and the repository's models out of
 * `:core:designsystem`, and keeps it all host-testable as plain functions.
 *
 * Which direction runs when is a performance decision as much as a modelling one. [toFilterSheetUi]
 * is the expensive one — it allocates a chip per tag and formats every label — so it runs when the
 * sheet *opens* and when the snapshot's options change, not while the user edits. During an edit
 * `LogFilterSheetHost` transforms the already-mapped value through [withEvent], which touches only
 * the control that changed, and [toFilterSelection] converts back once, on Apply.
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

/**
 * Applies one sheet interaction to the sheet's own model.
 *
 * This is what makes an edit cheap: a chip tap rewrites one [LogFilterOptionUi] and leaves every
 * other chip, both date/time fields, and the latency block as the same instances, where re-running
 * [toFilterSheetUi] would rebuild all of them and reformat every label. The slider is the one event
 * that reformats anything, because its readout states the value it is changing.
 *
 * [LogFilterSheetEvent.Applied] and [LogFilterSheetEvent.Cleared] are absent on purpose: they are
 * decisions about the edit rather than edits, so the host handles them.
 */
internal fun LogFilterSheetUi.withEvent(event: LogFilterSheetEvent): LogFilterSheetUi =
    when (event) {
        is LogFilterSheetEvent.TagToggled -> {
            copy(tags = tags.toggleOption(event.id))
        }

        is LogFilterSheetEvent.SeverityToggled -> {
            copy(severities = severities.toggleOption(event.id))
        }

        is LogFilterSheetEvent.AiGeneratedSelected -> {
            copy(aiGenerated = event.choice)
        }

        // The pickers report UTC epoch milliseconds; turning them into calendar dates is a UTC rule,
        // so it happens here rather than in the sheet. Each bound keeps the time it already had.
        is LogFilterSheetEvent.DateRangeSelected -> {
            copy(
                start = dateTimeUi(date = event.startUtcMillis?.let(::utcDateOf), time = start.localTime()),
                end = dateTimeUi(date = event.endUtcMillis?.let(::utcDateOf), time = end.localTime()),
            )
        }

        is LogFilterSheetEvent.StartTimeSelected -> {
            copy(
                start =
                    dateTimeUi(
                        date = start.localDate(),
                        time = LocalTime.of(event.hourOfDayUtc, event.minuteOfHourUtc),
                    ),
            )
        }

        is LogFilterSheetEvent.EndTimeSelected -> {
            copy(
                end =
                    dateTimeUi(
                        date = end.localDate(),
                        time = LocalTime.of(event.hourOfDayUtc, event.minuteOfHourUtc),
                    ),
            )
        }

        is LogFilterSheetEvent.LatencyRangeSelected -> {
            copy(latency = latency?.withSelection(event.range))
        }

        LogFilterSheetEvent.Applied, LogFilterSheetEvent.Cleared -> {
            this
        }
    }

/**
 * Converts the edited sheet back into the selection the query is derived from — the Apply step.
 *
 * The latency bounds are the one place this is not a straight read-back. The slider always sits
 * somewhere, so an untouched control reports the whole extent; a bound at the extent is returned as
 * `null` instead, because [LogFilterSelection] spells "unconstrained" as an absent bound and
 * `toLogQuery` would otherwise be handed a predicate that restricts nothing. That keeps a cleared
 * sheet round-tripping to exactly `LogFilterSelection()`.
 */
internal fun LogFilterSheetUi.toFilterSelection(): LogFilterSelection =
    LogFilterSelection(
        tags = tags.filter(LogFilterOptionUi::selected).mapTo(mutableSetOf(), LogFilterOptionUi::id),
        severities =
            severities
                .filter(LogFilterOptionUi::selected)
                .mapNotNullTo(mutableSetOf()) { severityFilterFor(it.id) },
        aiGenerated = aiGenerated.toFilterSelection(),
        startDateUtc = start.localDate(),
        endDateUtc = end.localDate(),
        startTimeUtc = start.localTime(),
        endTimeUtc = end.localTime(),
        minimumLatencyMs = latency?.selectedMinimum(),
        maximumLatencyMs = latency?.selectedMaximum(),
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

/** Flips the one option the tap named and returns every other element unchanged. */
private fun List<LogFilterOptionUi>.toggleOption(id: String): List<LogFilterOptionUi> =
    map { option -> if (option.id == id) option.copy(selected = !option.selected) else option }

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

/** The raw halves read back out of a rendered bound, so one edit can rebuild the other half. */
private fun LogFilterDateTimeUi.localDate(): LocalDate? = dateUtcMillis?.let(::utcDateOf)

private fun LogFilterDateTimeUi.localTime(): LocalTime? =
    hourOfDayUtc?.let { hour -> minuteOfHourUtc?.let { minute -> LocalTime.of(hour, minute) } }

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
        selectionLabel = latencySelectionLabel(minimum, maximum),
    )
}

/**
 * Moves the thumbs, snapping to whole milliseconds.
 *
 * Snapping is what keeps the rendered value and its readout the same number: latency is a
 * millisecond quantity, and at any realistic extent one millisecond is far finer than one pixel, so
 * nothing about the gesture is coarsened by it.
 */
private fun LogFilterLatencyUi.withSelection(range: ClosedFloatingPointRange<Float>): LogFilterLatencyUi {
    val lowerBound = bounds.start.roundToLong()
    val upperBound = bounds.endInclusive.roundToLong()
    val minimum = range.start.roundToLong().coerceIn(lowerBound, upperBound)
    val maximum = range.endInclusive.roundToLong().coerceIn(minimum, upperBound)
    return copy(
        selection = minimum.toFloat()..maximum.toFloat(),
        selectionLabel = latencySelectionLabel(minimum, maximum),
    )
}

/** A bound sitting at the extent restricts nothing, and is reported as no bound at all. */
private fun LogFilterLatencyUi.selectedMinimum(): Long? = selection.start.roundToLong().takeIf { it > bounds.start.roundToLong() }

private fun LogFilterLatencyUi.selectedMaximum(): Long? =
    selection.endInclusive.roundToLong().takeIf { it < bounds.endInclusive.roundToLong() }

/** The current selection in words, e.g. `128 – 3,245 ms`; a slider with no readout is guesswork. */
private fun latencySelectionLabel(
    minimum: Long,
    maximum: Long,
): String = "${latencyFormat.format(minimum)} – ${latencyFormat.format(maximum)} ms"
