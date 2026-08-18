package com.example.fgfchallenge.feature.logs.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerUiState
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests for the screen-state-to-[LogQuery] derivation.
 *
 * They are concentrated on normalization, because that is where the two vocabularies disagree: the
 * screen holds what the user picked, the repository expects active conditions only, and an
 * unnormalized value would either restrict nothing while costing a predicate or — for a reversed
 * range — quietly return no rows at all.
 */
class LogViewerQueryTest {
    @Test
    fun `the default screen state is the default query`() {
        assertThat(LogViewerUiState().toLogQuery()).isEqualTo(LogQuery())
    }

    @Test
    fun `search text is trimmed and whitespace alone is no search`() {
        assertThat(stateWith(query = "  timeout ").toLogQuery().literalSearch).isEqualTo("timeout")
        assertThat(stateWith(query = "   ").toLogQuery().literalSearch).isEqualTo("")
    }

    @Test
    fun `selected tags and severities are carried through unchanged`() {
        val query =
            stateWith(
                filters =
                    LogFilterSelection(
                        tags = setOf("network", "auth"),
                        severities = setOf(Severity.ERROR, Severity.FATAL),
                    ),
            ).toLogQuery()

        assertThat(query.selectedTags).isEqualTo(setOf("network", "auth"))
        assertThat(query.selectedSeverities).isEqualTo(setOf(Severity.ERROR, Severity.FATAL))
    }

    @Test
    fun `the AI-generated control's three choices become no constraint, true, and false`() {
        assertThat(aiConstraintFor(AiGeneratedFilter.Any)).isNull()
        assertThat(aiConstraintFor(AiGeneratedFilter.Yes)).isEqualTo(true)
        assertThat(aiConstraintFor(AiGeneratedFilter.No)).isEqualTo(false)
    }

    @Test
    fun `sort order maps to the matching database direction`() {
        assertThat(stateWith(sortOrder = LogSortOrder.NewestFirst).toLogQuery().sortDirection)
            .isEqualTo(LogSortDirection.NewestFirst)
        assertThat(stateWith(sortOrder = LogSortOrder.OldestFirst).toLogQuery().sortDirection)
            .isEqualTo(LogSortDirection.OldestFirst)
    }

    @Test
    fun `a date-only range covers whole UTC days from the start of the first to the end of the last`() {
        val query =
            stateWith(
                filters =
                    LogFilterSelection(
                        startDateUtc = LocalDate.of(2025, 5, 20),
                        endDateUtc = LocalDate.of(2025, 5, 22),
                    ),
            ).toLogQuery()

        assertThat(query.startInclusiveUtc).isEqualTo(Instant.parse("2025-05-20T00:00:00Z"))
        // Exclusive, so the whole of the 22nd is still included.
        assertThat(query.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-23T00:00:00Z"))
    }

    @Test
    fun `a single selected day is that complete UTC day`() {
        val query = stateWith(filters = LogFilterSelection(startDateUtc = MAY_22, endDateUtc = MAY_22)).toLogQuery()

        assertThat(query.startInclusiveUtc).isEqualTo(Instant.parse("2025-05-22T00:00:00Z"))
        assertThat(query.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-23T00:00:00Z"))
    }

    @Test
    fun `a selected end minute is inclusive, so the bound is the minute after it`() {
        val query =
            stateWith(
                filters =
                    LogFilterSelection(
                        startDateUtc = MAY_22,
                        endDateUtc = MAY_22,
                        startTimeUtc = LocalTime.of(17, 9),
                        endTimeUtc = LocalTime.of(17, 11),
                    ),
            ).toLogQuery()

        assertThat(query.startInclusiveUtc).isEqualTo(Instant.parse("2025-05-22T17:09:00Z"))
        // Everything within 17:11 matches, including 17:11:59.999.
        assertThat(query.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-22T17:12:00Z"))
    }

    @Test
    fun `an end minute at the end of the day rolls the exclusive bound into the next day`() {
        val query =
            stateWith(
                filters = LogFilterSelection(endDateUtc = MAY_22, endTimeUtc = LocalTime.of(23, 59)),
            ).toLogQuery()

        assertThat(query.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-23T00:00:00Z"))
    }

    @Test
    fun `sub-minute precision in a picked time is discarded before the minute is advanced`() {
        val query =
            stateWith(
                filters =
                    LogFilterSelection(
                        startDateUtc = MAY_22,
                        endDateUtc = MAY_22,
                        startTimeUtc = LocalTime.of(17, 9, 45),
                        endTimeUtc = LocalTime.of(17, 11, 45),
                    ),
            ).toLogQuery()

        assertThat(query.startInclusiveUtc).isEqualTo(Instant.parse("2025-05-22T17:09:00Z"))
        assertThat(query.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-22T17:12:00Z"))
    }

    @Test
    fun `an open-ended range constrains only the side that was chosen`() {
        val fromOnly = stateWith(filters = LogFilterSelection(startDateUtc = MAY_22)).toLogQuery()
        assertThat(fromOnly.startInclusiveUtc).isEqualTo(Instant.parse("2025-05-22T00:00:00Z"))
        assertThat(fromOnly.endExclusiveUtc).isNull()

        val untilOnly = stateWith(filters = LogFilterSelection(endDateUtc = MAY_22)).toLogQuery()
        assertThat(untilOnly.startInclusiveUtc).isNull()
        assertThat(untilOnly.endExclusiveUtc).isEqualTo(Instant.parse("2025-05-23T00:00:00Z"))
    }

    @Test
    fun `a reversed time range is dropped rather than sent as a query matching nothing`() {
        val query =
            stateWith(
                filters =
                    LogFilterSelection(
                        startDateUtc = LocalDate.of(2025, 5, 23),
                        endDateUtc = LocalDate.of(2025, 5, 22),
                    ),
            ).toLogQuery()

        assertThat(query.startInclusiveUtc).isNull()
        assertThat(query.endExclusiveUtc).isNull()
    }

    @Test
    fun `latency bounds inside the stored extent are kept as inclusive bounds`() {
        val query =
            stateWith(
                filters = LogFilterSelection(minimumLatencyMs = 100, maximumLatencyMs = 2_000),
            ).toLogQuery(snapshotLatencyExtent = 0L..5_000L)

        assertThat(query.minimumLatencyInclusive).isEqualTo(100L)
        assertThat(query.maximumLatencyInclusive).isEqualTo(2_000L)
    }

    @Test
    fun `a full-width latency selection restricts nothing and is dropped`() {
        val query =
            stateWith(
                filters = LogFilterSelection(minimumLatencyMs = 0, maximumLatencyMs = 5_000),
            ).toLogQuery(snapshotLatencyExtent = 0L..5_000L)

        assertThat(query.minimumLatencyInclusive).isNull()
        assertThat(query.maximumLatencyInclusive).isNull()
    }

    @Test
    fun `only the latency bound that reaches the extent is dropped`() {
        val query =
            stateWith(
                filters = LogFilterSelection(minimumLatencyMs = 100, maximumLatencyMs = 5_000),
            ).toLogQuery(snapshotLatencyExtent = 0L..5_000L)

        assertThat(query.minimumLatencyInclusive).isEqualTo(100L)
        assertThat(query.maximumLatencyInclusive).isNull()
    }

    @Test
    fun `latency bounds are kept as chosen while the stored extent is unknown`() {
        val query =
            stateWith(
                filters = LogFilterSelection(minimumLatencyMs = 0, maximumLatencyMs = 5_000),
            ).toLogQuery()

        assertThat(query.minimumLatencyInclusive).isEqualTo(0L)
        assertThat(query.maximumLatencyInclusive).isEqualTo(5_000L)
    }

    @Test
    fun `a reversed latency range is dropped`() {
        val query =
            stateWith(
                filters = LogFilterSelection(minimumLatencyMs = 2_000, maximumLatencyMs = 100),
            ).toLogQuery()

        assertThat(query.minimumLatencyInclusive).isNull()
        assertThat(query.maximumLatencyInclusive).isNull()
    }

    @Test
    fun `every category active at once produces one query carrying all of them`() {
        val query =
            stateWith(
                query = "timeout",
                filters =
                    LogFilterSelection(
                        tags = setOf("network"),
                        severities = setOf(Severity.ERROR),
                        aiGenerated = AiGeneratedFilter.No,
                        startDateUtc = MAY_22,
                        endDateUtc = MAY_22,
                        startTimeUtc = LocalTime.of(17, 0),
                        endTimeUtc = LocalTime.of(17, 59),
                        minimumLatencyMs = 100,
                        maximumLatencyMs = 2_000,
                    ),
                sortOrder = LogSortOrder.OldestFirst,
            ).toLogQuery(snapshotLatencyExtent = 0L..5_000L)

        assertThat(query).isEqualTo(
            LogQuery(
                literalSearch = "timeout",
                selectedTags = setOf("network"),
                selectedSeverities = setOf(Severity.ERROR),
                aiGeneratedConstraint = false,
                startInclusiveUtc = Instant.parse("2025-05-22T17:00:00Z"),
                endExclusiveUtc = Instant.parse("2025-05-22T18:00:00Z"),
                minimumLatencyInclusive = 100,
                maximumLatencyInclusive = 2_000,
                sortDirection = LogSortDirection.OldestFirst,
            ),
        )
    }

    private fun aiConstraintFor(choice: AiGeneratedFilter): Boolean? =
        stateWith(filters = LogFilterSelection(aiGenerated = choice)).toLogQuery().aiGeneratedConstraint

    private fun stateWith(
        query: String = "",
        filters: LogFilterSelection = LogFilterSelection(),
        sortOrder: LogSortOrder = LogSortOrder.NewestFirst,
    ): LogViewerUiState = LogViewerUiState(query = query, filters = filters, sortOrder = sortOrder)

    private companion object {
        val MAY_22: LocalDate = LocalDate.of(2025, 5, 22)
    }
}
