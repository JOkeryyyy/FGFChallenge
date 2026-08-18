package com.example.fgfchallenge.feature.logs.data.model

import java.time.Instant

/**
 * The one immutable description of what the viewer is asking for. The paged rows and the
 * full-result summary are both built from the same value, which is what keeps the list and its
 * counts from drifting apart.
 *
 * Every property's default is its inactive form, so [LogQuery] with no arguments is the app's
 * default query: no search, no filters, newest first. Normalizing user input into these values —
 * trimming search text, rejecting reversed ranges, collapsing a full-width latency selection to
 * `null` — belongs to the domain query policy, not here.
 */
internal data class LogQuery(
    /** Matched as a case-insensitive literal substring of `message` or `id`. Blank is inactive. */
    val literalSearch: String = "",
    val selectedTags: Set<String> = emptySet(),
    val selectedSeverities: Set<Severity> = emptySet(),
    /** `null` is the "Any" choice; `true`/`false` constrain to that exact value. */
    val aiGeneratedConstraint: Boolean? = null,
    /** Half-open UTC interval: [startInclusiveUtc] is included, [endExclusiveUtc] is not. */
    val startInclusiveUtc: Instant? = null,
    val endExclusiveUtc: Instant? = null,
    /** Both latency bounds are inclusive, and either side may be constrained on its own. */
    val minimumLatencyInclusive: Long? = null,
    val maximumLatencyInclusive: Long? = null,
    val sortDirection: LogSortDirection = LogSortDirection.NewestFirst,
)

/**
 * The part of this query the full-result aggregate depends on.
 *
 * `LogQuerySql.severityCountSelect` never reads [LogQuery.sortDirection] — reordering rows cannot
 * change how many of each severity match — so two queries differing only in direction describe one
 * aggregate. Collapsing the direction to a single value is what lets a caller de-duplicate them.
 *
 * The returned value is a criteria key, not a query to page with: its [LogQuery.sortDirection] is
 * canonical rather than the user's, and only the count select may be built from it.
 */
internal fun LogQuery.aggregateCriteria(): LogQuery = copy(sortDirection = LogSortDirection.NewestFirst)

/**
 * Direction of the deterministic timestamp-then-ID ordering every select applies.
 *
 * This is the data layer's own type rather than presentation's `LogSortOrder`: the repository
 * contract cannot depend on presentation, and the two are mapped where the query is assembled.
 */
internal enum class LogSortDirection {
    NewestFirst,
    OldestFirst,
}
