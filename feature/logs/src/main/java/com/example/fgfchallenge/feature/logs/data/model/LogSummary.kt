package com.example.fgfchallenge.feature.logs.data.model

/**
 * Aggregates over the *complete* result of a `LogQuery`, not over the rows Paging happens to have
 * loaded. Presentation derives its result count and `(ERROR + FATAL) / total` density from these
 * values; the loaded-item count is never a substitute.
 */
internal data class LogSummary(
    val totalCount: Int = 0,
    /**
     * Contains all five known severities, zero-valued when absent, so the legend does not change
     * shape between queries. [Severity.UNKNOWN] appears only when the snapshot actually holds an
     * unrecognized value — it is valid data, and never counted as an error.
     */
    val countBySeverity: Map<Severity, Int> = KNOWN_SEVERITIES.associateWith { 0 },
)

/**
 * Unfiltered metadata about the whole snapshot, used to populate the filter controls before any
 * filter is applied. It is derived with aggregate selects rather than by reading every row.
 */
internal data class LogFilterOptions(
    val availableTags: List<String> = emptyList(),
    /** Both are null while no snapshot is stored, which the UI shows as an unavailable range. */
    val minimumLatencyMs: Long? = null,
    val maximumLatencyMs: Long? = null,
)

/**
 * The severities the product exposes as filter choices and legend rows. [Severity.UNKNOWN] is
 * deliberately excluded: it is a storage fallback, not a value users select.
 */
internal val KNOWN_SEVERITIES: List<Severity> =
    listOf(
        Severity.DEBUG,
        Severity.INFO,
        Severity.WARN,
        Severity.ERROR,
        Severity.FATAL,
    )
