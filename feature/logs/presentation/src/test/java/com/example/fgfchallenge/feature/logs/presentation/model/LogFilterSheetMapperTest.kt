package com.example.fgfchallenge.feature.logs.presentation.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.example.fgfchallenge.core.designsystem.model.AiGeneratedChoice
import com.example.fgfchallenge.core.designsystem.model.LogFilterOptionUi
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.Severity
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests for the filter draft's translation into the design system's sheet model.
 *
 * The interesting cases are the ones where the two vocabularies do not line up: a severity the
 * snapshot never produced is still a choice, an unset slider bound has to be shown *somewhere* on
 * the slider, and a snapshot with no usable latency extent has no slider at all.
 */
class LogFilterSheetMapperTest {
    @Test
    fun `tag chips come from the snapshot and are marked from the draft`() {
        val sheet =
            LogFilterSelection(tags = setOf("network")).toFilterSheetUi(
                LogFilterOptions(availableTags = listOf("auth", "cache", "network")),
            )

        assertThat(sheet.tags.map(LogFilterOptionUi::label)).containsExactly("auth", "cache", "network")
        assertThat(sheet.tags.single { it.id == "network" }.selected).isTrue()
        assertThat(sheet.tags.single { it.id == "auth" }.selected).isFalse()
    }

    @Test
    fun `severity chips are the five product choices regardless of what the snapshot holds`() {
        val sheet = LogFilterSelection(severities = setOf(Severity.ERROR)).toFilterSheetUi(LogFilterOptions())

        // No tags exist in this snapshot, but the severity choices are fixed by the product, and
        // UNKNOWN is deliberately absent: it is a storage fallback, not something a user filters by.
        assertThat(sheet.severities.map(LogFilterOptionUi::id))
            .containsExactly("DEBUG", "INFO", "WARN", "ERROR", "FATAL")
        assertThat(sheet.severities.single { it.id == "ERROR" }.selected).isTrue()
    }

    @Test
    fun `the AI tri-state round-trips through the sheet's own vocabulary`() {
        AiGeneratedFilter.entries.forEach { choice ->
            val sheet = LogFilterSelection(aiGenerated = choice).toFilterSheetUi(LogFilterOptions())

            assertThat(sheet.aiGenerated.toFilterSelection()).isEqualTo(choice)
        }
    }

    @Test
    fun `Any is the AI choice a default selection renders as`() {
        val sheet = LogFilterSelection().toFilterSheetUi(LogFilterOptions())

        assertThat(sheet.aiGenerated).isEqualTo(AiGeneratedChoice.Any)
    }

    @Test
    fun `a chosen date and time travel as both a label and the value its picker reopens on`() {
        val sheet =
            LogFilterSelection(
                startDateUtc = LocalDate.of(2025, 5, 22),
                startTimeUtc = LocalTime.of(17, 9),
            ).toFilterSheetUi(LogFilterOptions())

        assertThat(sheet.start.dateLabel).isEqualTo("2025-05-22")
        assertThat(sheet.start.timeLabel).isEqualTo("17:09")
        assertThat(sheet.start.dateUtcMillis).isEqualTo(MAY_22_UTC_MIDNIGHT)
        assertThat(sheet.start.hourOfDayUtc).isEqualTo(17)
        assertThat(sheet.start.minuteOfHourUtc).isEqualTo(9)
    }

    @Test
    fun `an unset bound stays unset rather than defaulting to a date`() {
        val sheet = LogFilterSelection().toFilterSheetUi(LogFilterOptions())

        assertThat(sheet.end.dateLabel).isNull()
        assertThat(sheet.end.timeLabel).isNull()
        assertThat(sheet.end.dateUtcMillis).isNull()
    }

    @Test
    fun `an untouched latency slider spans the whole snapshot extent`() {
        val latency =
            LogFilterSelection()
                .toFilterSheetUi(LogFilterOptions(minimumLatencyMs = 5, maximumLatencyMs = 9_000))
                .latency

        assertThat(latency).isNotNull()
        assertThat(latency?.bounds).isEqualTo(5f..9_000f)
        // Which is also the position `toLogQuery` reads back as "no latency predicate".
        assertThat(latency?.selection).isEqualTo(5f..9_000f)
        assertThat(latency?.lowerBoundLabel).isEqualTo("5")
        assertThat(latency?.upperBoundLabel).isEqualTo("9,000")
        assertThat(latency?.selectionLabel).isEqualTo("5 – 9,000 ms")
    }

    @Test
    fun `a chosen latency range sits inside the extent and is spelled out`() {
        val latency =
            LogFilterSelection(minimumLatencyMs = 250, maximumLatencyMs = 7_500)
                .toFilterSheetUi(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))
                .latency

        assertThat(latency?.selection).isEqualTo(250f..7_500f)
        assertThat(latency?.selectionLabel).isEqualTo("250 – 7,500 ms")
    }

    @Test
    fun `bounds outside the stored extent are pulled back onto the slider`() {
        val latency =
            LogFilterSelection(minimumLatencyMs = -50, maximumLatencyMs = 50_000)
                .toFilterSheetUi(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))
                .latency

        // A stale draft from a previous snapshot must not be handed to RangeSlider as a value
        // outside its own range.
        assertThat(latency?.selection).isEqualTo(0f..10_000f)
    }

    @Test
    fun `a reversed draft cannot produce a range whose start exceeds its end`() {
        val latency =
            LogFilterSelection(minimumLatencyMs = 7_500, maximumLatencyMs = 250)
                .toFilterSheetUi(LogFilterOptions(minimumLatencyMs = 0, maximumLatencyMs = 10_000))
                .latency

        assertThat(latency?.selection).isEqualTo(7_500f..7_500f)
    }

    @Test
    fun `no snapshot latency extent means no slider at all`() {
        assertThat(LogFilterSelection().toFilterSheetUi(LogFilterOptions()).latency).isNull()
    }

    @Test
    fun `a single-valued extent is reported as unavailable rather than a zero-width slider`() {
        val options = LogFilterOptions(minimumLatencyMs = 400, maximumLatencyMs = 400)

        assertThat(options.latencyExtent()).isNull()
        assertThat(LogFilterSelection().toFilterSheetUi(options).latency).isNull()
    }

    @Test
    fun `a severity chip resolves back to the severity it was built from`() {
        assertThat(severityFilterFor("ERROR")).isEqualTo(Severity.ERROR)
        assertThat(severityFilterFor("UNKNOWN")).isNull()
        assertThat(severityFilterFor("not-a-severity")).isNull()
    }

    @Test
    fun `a picked date is the UTC day it falls in`() {
        assertThat(utcDateOf(MAY_22_UTC_MIDNIGHT)).isEqualTo(LocalDate.of(2025, 5, 22))
        // The last millisecond of that UTC day is still that day, not the next one.
        assertThat(utcDateOf(MAY_22_UTC_MIDNIGHT + 86_399_999)).isEqualTo(LocalDate.of(2025, 5, 22))
    }

    private companion object {
        const val MAY_22_UTC_MIDNIGHT = 1_747_872_000_000L
    }
}
