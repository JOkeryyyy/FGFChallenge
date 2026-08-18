package com.example.fgfchallenge.feature.logs.presentation.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.Severity
import org.junit.Test
import java.time.Instant

/**
 * Tests for the one mapping between a stored `LogEntry` and what the screen renders: row values,
 * details values, and the rule that decides where a UTC minute header belongs.
 *
 * The header rule gets the most attention because it is the part with no second chance. The Paging
 * transformation applies it to one adjacent pair at a time and never sees the whole result, so
 * "correct headers across page boundaries" is entirely a property of this function — there is no
 * later pass that could notice a missing or duplicated group.
 *
 * The formatting tests pin UTC and the fixed locale. Both are load-bearing: a device in another
 * zone must not shift a row into a different minute than its header, and a device in another locale
 * must not regroup a latency figure the goldens contain.
 */
class LogEntryUiMapperTest {
    @Test
    fun `a row shows only the seconds within its minute`() {
        val row = entryAt("2025-05-22T17:11:58.123Z").toListItem()

        assertThat(row.row.time).isEqualTo("58.123")
        assertThat(row.utcMinuteId).isEqualTo("2025-05-22T17:11Z")
    }

    @Test
    fun `a row carries the entry's identity, severity, tag, and message unchanged`() {
        val row =
            entryAt("2025-05-22T17:11:58.123Z", severity = Severity.FATAL, tag = "auth")
                .copy(id = "1711-58123", message = "Auth service unreachable")
                .toListItem()

        assertThat(row.row.id).isEqualTo("1711-58123")
        assertThat(row.row.severityLabel).isEqualTo("FATAL")
        assertThat(row.row.severityTone).isEqualTo(SeverityBadgeTone.Fatal)
        assertThat(row.row.tagLabel).isEqualTo("auth")
        assertThat(row.row.message).isEqualTo("Auth service unreachable")
        assertThat(row.stableKey).isEqualTo("log:1711-58123")
    }

    @Test
    fun `the minute id and its header label are the same UTC minute`() {
        val row = entryAt("2025-05-22T17:09:45.672Z").toListItem()
        val header = LogViewerListItem.MinuteHeader(row.utcMinuteId)

        assertThat(header.minute).isEqualTo("17:09")
        assertThat(header.stableKey).isEqualTo("minute:2025-05-22T17:09Z")
    }

    @Test
    fun `an unrecognized severity stays valid data rather than becoming an error`() {
        val row = entryAt("2025-05-22T17:11:00.000Z", severity = Severity.UNKNOWN).toListItem()

        assertThat(row.row.severityLabel).isEqualTo("UNKNOWN")
        assertThat(row.row.severityTone).isEqualTo(SeverityBadgeTone.Unknown)
    }

    @Test
    fun `an instant is grouped by its UTC minute regardless of the default time zone`() {
        // A zone offset that is not a whole hour would move the header's minute if the mapping ever
        // formatted in local time; Kathmandu is UTC+05:45.
        withDefaultTimeZone("Asia/Kathmandu") {
            val row = entryAt("2025-05-22T17:11:58.123Z").toListItem()

            assertThat(row.utcMinuteId).isEqualTo("2025-05-22T17:11Z")
            assertThat(row.row.time).isEqualTo("58.123")
        }
    }

    @Test
    fun `details report the complete UTC instant, grouped latency, and a yes or no AI flag`() {
        val details =
            entryAt("2025-05-22T17:11:58.123Z")
                .copy(id = "1711-58123", latencyMs = 3_245, isAiGenerated = true, sessionId = "sess-1")
                .toLogDetailsUi()

        assertThat(details.timestampUtc).isEqualTo("2025-05-22T17:11:58.123Z")
        assertThat(details.latency).isEqualTo("3,245 ms")
        assertThat(details.aiGenerated).isEqualTo("Yes")
        assertThat(details.logId).isEqualTo("1711-58123")
        assertThat(details.sessionId).isEqualTo("sess-1")
    }

    @Test
    fun `details report a non-AI entry as No`() {
        assertThat(entryAt("2025-05-22T17:11:58.123Z").copy(isAiGenerated = false).toLogDetailsUi().aiGenerated)
            .isEqualTo("No")
    }

    @Test
    fun `details and the row it belongs to describe the same entry`() {
        val entry = entryAt("2025-05-22T17:11:58.123Z", severity = Severity.ERROR, tag = "network")
        val row = entry.toListItem()
        val details = entry.toLogDetailsUi()

        // Selection resolves details by ID from the repository rather than from the row, so this
        // identity is what makes the sheet's contents match the row the user tapped.
        assertThat(details.logId).isEqualTo(row.row.id)
        assertThat(details.message).isEqualTo(row.row.message)
        assertThat(details.tag).isEqualTo(row.row.tagLabel)
        assertThat(details.severityLabel).isEqualTo(row.row.severityLabel)
        assertThat(details.severityTone).isEqualTo(row.row.severityTone)
    }

    @Test
    fun `the first row of the list takes a header`() {
        val first = rowAt("2025-05-22T17:11:58.123Z")

        assertThat(minuteHeaderBetween(null, first)?.utcMinuteId).isEqualTo("2025-05-22T17:11Z")
    }

    @Test
    fun `a row in the same minute as the one above it takes no header`() {
        val above = rowAt("2025-05-22T17:11:58.123Z")
        val below = rowAt("2025-05-22T17:11:11.098Z")

        assertThat(minuteHeaderBetween(above, below)).isNull()
    }

    @Test
    fun `a row that starts a new minute takes a header for its own minute`() {
        val above = rowAt("2025-05-22T17:11:11.098Z")
        val below = rowAt("2025-05-22T17:10:59.384Z")

        val header = minuteHeaderBetween(above, below)

        assertThat(header).isNotNull()
        assertThat(header?.utcMinuteId).isEqualTo(below.utcMinuteId)
        assertThat(header?.minute).isEqualTo("17:10")
    }

    @Test
    fun `the same clock minute on a different day is a different group`() {
        val above = rowAt("2025-05-23T17:11:00.000Z")
        val below = rowAt("2025-05-22T17:11:00.000Z")

        // Minute-of-day would collapse these into one header spanning two dates.
        assertThat(minuteHeaderBetween(above, below)?.utcMinuteId).isEqualTo("2025-05-22T17:11Z")
    }

    @Test
    fun `the end of the loaded list takes no trailing header`() {
        assertThat(minuteHeaderBetween(rowAt("2025-05-22T17:11:58.123Z"), null)).isNull()
        assertThat(minuteHeaderBetween(null, null)).isNull()
    }

    @Test
    fun `a minute split across a page boundary is headed once`() {
        // The pair that straddles the boundary is the only thing the transformation sees of it: the
        // last row of one page and the first row of the next, both inside 17:11.
        val lastOfPage = rowAt("2025-05-22T17:11:20.000Z")
        val firstOfNextPage = rowAt("2025-05-22T17:11:19.000Z")

        assertThat(minuteHeaderBetween(lastOfPage, firstOfNextPage)).isNull()
    }

    private fun rowAt(timestamp: String): LogViewerListItem.LogRow = entryAt(timestamp).toListItem()

    private fun entryAt(
        timestamp: String,
        severity: Severity = Severity.INFO,
        tag: String = "network",
    ): LogEntry =
        LogEntry(
            id = "log-$timestamp",
            timestamp = Instant.parse(timestamp),
            severity = severity,
            tag = tag,
            message = "message",
            latencyMs = 128,
            isAiGenerated = false,
            sessionId = "sess-1",
        )

    /** Restores the previous default afterwards, so one test cannot leak a zone into the next. */
    private fun withDefaultTimeZone(
        zoneId: String,
        body: () -> Unit,
    ) {
        val previous = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zoneId))
        try {
            body()
        } finally {
            java.util.TimeZone.setDefault(previous)
        }
    }
}
