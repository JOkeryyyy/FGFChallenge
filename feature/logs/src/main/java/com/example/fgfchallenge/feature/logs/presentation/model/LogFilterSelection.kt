package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.feature.logs.data.model.Severity
import java.time.LocalDate
import java.time.LocalTime

/**
 * The structured filters currently *applied* to the result set, as the user chose them.
 *
 * This is deliberately the user's vocabulary rather than the database's: dates and times are the
 * separate UTC values the Material 3 pickers produce, the end of a range is the one the user
 * selected (inclusive), and latency bounds are whatever the slider reported. Turning that into the
 * repository's half-open interval and inactive-category rules is `toLogQuery`'s job, so this type
 * never has to hold a value the UI cannot round-trip back into its controls.
 *
 * Every default is the inactive form, so `LogFilterSelection()` is "no structured filters".
 */
@Immutable
internal data class LogFilterSelection(
    val tags: Set<String> = emptySet(),
    val severities: Set<Severity> = emptySet(),
    val aiGenerated: AiGeneratedFilter = AiGeneratedFilter.Any,
    /** Start of the UTC date range; the whole day when [startTimeUtc] is absent. */
    val startDateUtc: LocalDate? = null,
    /** End of the UTC date range, inclusive to the user in both its date-only and timed forms. */
    val endDateUtc: LocalDate? = null,
    val startTimeUtc: LocalTime? = null,
    val endTimeUtc: LocalTime? = null,
    val minimumLatencyMs: Long? = null,
    val maximumLatencyMs: Long? = null,
)

/**
 * The AI-generated control's three choices.
 *
 * [Any] is the inactive one: it adds no predicate, as opposed to [No], which restricts the result to
 * entries the payload flagged as not AI-generated.
 */
internal enum class AiGeneratedFilter {
    Any,
    Yes,
    No,
}

/**
 * Adds [tag] to the selection, or removes it when it is already selected.
 *
 * Toggling lives here rather than in the chip that reports the tap: a chip says what the user
 * pressed, and what that means for the selection is the state producer's decision.
 */
internal fun LogFilterSelection.toggleTag(tag: String): LogFilterSelection = copy(tags = tags.toggle(tag))

internal fun LogFilterSelection.toggleSeverity(severity: Severity): LogFilterSelection = copy(severities = severities.toggle(severity))

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
