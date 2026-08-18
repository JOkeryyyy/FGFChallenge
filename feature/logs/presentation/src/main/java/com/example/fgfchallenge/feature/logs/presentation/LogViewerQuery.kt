package com.example.fgfchallenge.feature.logs.presentation

import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSortDirection
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.latencyExtent
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerUiState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Derives the repository's canonical [LogQuery] from screen state.
 *
 * This is the whole of the prototype's "query layer": one pure function, called by
 * `LogViewerViewModel`, with no domain package or use case around it. It exists because the screen
 * and the database describe a query differently, and exactly one place should translate between
 * them — the paged rows and the aggregate summary are both derived from the value produced here, so
 * a normalization applied to one is applied to the other by construction.
 *
 * Normalization is the interesting part. Every category has an inactive form, and a value that
 * restricts nothing is turned into that inactive form rather than sent as a predicate the database
 * would evaluate against every row:
 *
 * - search text is trimmed, so whitespace alone is no search at all;
 * - empty tag/severity selections and AI-generated [AiGeneratedFilter.Any] are already inactive;
 * - the user's date/time choices become one half-open UTC interval, with an inclusive end minute
 *   converted to the following minute and a date-only end covering the complete selected UTC day;
 * - a reversed time or latency range is dropped instead of being sent as a query that matches
 *   nothing;
 * - latency bounds at or beyond [snapshotLatencyExtent] are dropped, which is what makes a
 *   full-width slider selection mean "unconstrained".
 *
 * [snapshotLatencyExtent] is the stored snapshot's `MIN`/`MAX` latency, or `null` when it is not
 * known. It stays a parameter so the collapse rule can be tested on its own; callers reach for
 * [activeLogQuery], which supplies the extent the state already holds. Collapsing here rather than
 * in the slider keeps the rule with the rest of the query policy instead of inside a composable.
 */
internal fun LogViewerUiState.toLogQuery(snapshotLatencyExtent: LongRange? = null): LogQuery {
    val interval = filters.utcInterval()
    val latency = filters.latencyRange(snapshotLatencyExtent)
    return LogQuery(
        literalSearch = query.trim(),
        selectedTags = filters.tags,
        selectedSeverities = filters.severities,
        aiGeneratedConstraint = filters.aiGenerated.toConstraint(),
        startInclusiveUtc = interval.startInclusive,
        endExclusiveUtc = interval.endExclusive,
        minimumLatencyInclusive = latency.minimum,
        maximumLatencyInclusive = latency.maximum,
        sortDirection = sortOrder.toSortDirection(),
    )
}

/**
 * The query the screen is currently asking for: [toLogQuery] applied with the snapshot extent the
 * state already carries.
 *
 * Every caller that needs "the active criteria" — the stream coordination, the stale-summary guard,
 * the active-filter badge — goes through this one function. Deriving it by hand with a different
 * extent argument would produce a value that compares unequal to the one the streams run on, which
 * looks like a query change that never happened.
 */
internal fun LogViewerUiState.activeLogQuery(): LogQuery = toLogQuery(filterOptions.latencyExtent())

private fun AiGeneratedFilter.toConstraint(): Boolean? =
    when (this) {
        AiGeneratedFilter.Any -> null
        AiGeneratedFilter.Yes -> true
        AiGeneratedFilter.No -> false
    }

private fun LogSortOrder.toSortDirection(): LogSortDirection =
    when (this) {
        LogSortOrder.NewestFirst -> LogSortDirection.NewestFirst
        LogSortOrder.OldestFirst -> LogSortDirection.OldestFirst
    }

/**
 * Converts the picker values into the database's half-open interval.
 *
 * Either side may stay open: a start with no end means "everything since", and the reverse is a
 * valid selection too. Only a range whose start is not strictly before its end is discarded — that
 * is a control error rather than a request for an empty result, and the assumptions document
 * requires it not to reach the repository.
 */
private fun LogFilterSelection.utcInterval(): UtcInterval {
    val startInclusive =
        startDateUtc
            ?.atTime(startTimeUtc?.truncatedTo(ChronoUnit.MINUTES) ?: LocalTime.MIDNIGHT)
            ?.toInstant(ZoneOffset.UTC)
    val endExclusive =
        endDateUtc?.let { endDate ->
            val endTime = endTimeUtc
            if (endTime == null) {
                // A date-only end includes the whole selected day, so the bound is the next day.
                endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
            } else {
                // The selected minute is inclusive to the user, so the exclusive bound is the
                // minute after it. `plusMinutes` rolls the date over for a 23:59 selection.
                endDate
                    .atTime(endTime.truncatedTo(ChronoUnit.MINUTES))
                    .plusMinutes(1)
                    .toInstant(ZoneOffset.UTC)
            }
        }

    if (startInclusive != null && endExclusive != null && !startInclusive.isBefore(endExclusive)) {
        return UtcInterval.Inactive
    }
    return UtcInterval(startInclusive = startInclusive, endExclusive = endExclusive)
}

/** Both latency bounds are inclusive to the database; either side may be constrained alone. */
private fun LogFilterSelection.latencyRange(extent: LongRange?): LatencyRange {
    val minimum = minimumLatencyMs
    val maximum = maximumLatencyMs
    if (minimum != null && maximum != null && minimum > maximum) return LatencyRange.Inactive
    return LatencyRange(
        minimum = minimum?.takeIf { extent == null || it > extent.first },
        maximum = maximum?.takeIf { extent == null || it < extent.last },
    )
}

private data class UtcInterval(
    val startInclusive: Instant?,
    val endExclusive: Instant?,
) {
    companion object {
        val Inactive = UtcInterval(startInclusive = null, endExclusive = null)
    }
}

private data class LatencyRange(
    val minimum: Long?,
    val maximum: Long?,
) {
    companion object {
        val Inactive = LatencyRange(minimum = null, maximum = null)
    }
}
