package com.example.fgfchallenge.feature.logs.data.local

import androidx.room.ColumnInfo

/**
 * Result rows for the projections that do not return whole entities. They exist only so Room has
 * a type to map each aggregate select onto, and stay inside `data` like the DAO itself.
 */
internal data class SeverityCountRow(
    @ColumnInfo(name = LogEntity.COLUMN_SEVERITY)
    val severity: String,
    @ColumnInfo(name = COLUMN_COUNT)
    val count: Int,
) {
    internal companion object {
        const val COLUMN_COUNT = "match_count"
    }
}

/**
 * The dataset's latency extent, used to seed the filter slider. Both values are null while the
 * table is empty, because `MIN`/`MAX` over no rows is null rather than zero.
 */
internal data class LatencyBoundsRow(
    @ColumnInfo(name = COLUMN_MINIMUM)
    val minimumLatencyMs: Long?,
    @ColumnInfo(name = COLUMN_MAXIMUM)
    val maximumLatencyMs: Long?,
) {
    internal companion object {
        const val COLUMN_MINIMUM = "minimum_latency_ms"
        const val COLUMN_MAXIMUM = "maximum_latency_ms"
    }
}
