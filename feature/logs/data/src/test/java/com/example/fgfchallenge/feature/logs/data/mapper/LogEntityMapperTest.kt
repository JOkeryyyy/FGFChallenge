package com.example.fgfchallenge.feature.logs.data.mapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.fgfchallenge.feature.logs.data.local.LatencyBoundsRow
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import com.example.fgfchallenge.feature.logs.data.local.SeverityCountRow
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.Severity
import org.junit.Test
import java.time.Instant

/**
 * Covers the database boundary's conversions: that a round trip through storage preserves every
 * field and the exact UTC instant, and that grouped severity rows fold into a summary whose total
 * and per-severity counts come from the same source.
 */
class LogEntityMapperTest {
    @Test
    fun `a log survives a round trip through its stored form`() {
        val entry =
            LogEntry(
                id = "log-1",
                // Millisecond precision is the storage representation's limit and the payload's.
                timestamp = Instant.parse("2026-08-16T17:10:05.250Z"),
                severity = Severity.ERROR,
                tag = "network",
                message = "Connection timeout",
                latencyMs = 2040,
                isAiGenerated = true,
                sessionId = "session-666",
            )

        assertThat(entry.toEntity().toLogEntry()).isEqualTo(entry)
    }

    @Test
    fun `a stored severity the app does not recognize reads back as unknown`() {
        val entity =
            LogEntity(
                id = "log-1",
                timestampEpochMillis = 0,
                severity = "TRACE",
                tag = "ui",
                message = "Span opened",
                latencyMs = 3,
                isAiGenerated = false,
                sessionId = "session-666",
            )

        assertThat(entity.toLogEntry().severity).isEqualTo(Severity.UNKNOWN)
    }

    @Test
    fun `the summary totals the same rows it reports per severity`() {
        val rows =
            listOf(
                SeverityCountRow(severity = "ERROR", count = 3),
                SeverityCountRow(severity = "FATAL", count = 2),
                SeverityCountRow(severity = "INFO", count = 5),
            )

        val summary = rows.toLogSummary()

        assertThat(summary.totalCount).isEqualTo(10)
        assertThat(summary.countBySeverity).isEqualTo(
            mapOf(
                Severity.DEBUG to 0,
                Severity.INFO to 5,
                Severity.WARN to 0,
                Severity.ERROR to 3,
                Severity.FATAL to 2,
            ),
        )
    }

    @Test
    fun `an unknown severity is counted but is not one of the fixed legend entries`() {
        val summary = listOf(SeverityCountRow(severity = "TRACE", count = 4)).toLogSummary()

        assertThat(summary.totalCount).isEqualTo(4)
        assertThat(summary.countBySeverity[Severity.UNKNOWN]).isEqualTo(4)
        // The five known severities stay present at zero so the legend keeps its shape.
        assertThat(summary.countBySeverity[Severity.ERROR]).isEqualTo(0)
    }

    @Test
    fun `an empty result still reports every known severity at zero`() {
        val summary = emptyList<SeverityCountRow>().toLogSummary()

        assertThat(summary.totalCount).isEqualTo(0)
        assertThat(summary.countBySeverity[Severity.FATAL]).isEqualTo(0)
        assertThat(summary.countBySeverity[Severity.UNKNOWN]).isNull()
    }

    @Test
    fun `filter options carry the tag list and latency extent as queried`() {
        val options =
            LatencyBoundsRow(minimumLatencyMs = 12, maximumLatencyMs = 5000)
                .toLogFilterOptions(availableTags = listOf("cache", "db"))

        assertThat(options.availableTags).isEqualTo(listOf("cache", "db"))
        assertThat(options.minimumLatencyMs).isEqualTo(12)
        assertThat(options.maximumLatencyMs).isEqualTo(5000)
    }
}
