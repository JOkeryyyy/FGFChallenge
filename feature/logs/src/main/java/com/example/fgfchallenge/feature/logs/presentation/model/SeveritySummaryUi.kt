package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.SeverityDensityUi
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import kotlin.math.roundToInt

/**
 * Severity counts for the currently displayed result set, plus the mapping that turns them into the
 * design system's [SeverityDensityUi].
 *
 * Presentation owns the density calculation — `SeverityIndicator` only draws what it is handed —
 * so this file is the single place where counts become ring geometry.
 */
@Immutable
internal data class SeveritySummaryUi(
    val totalLogCount: Int,
    val errorCount: Int,
    val fatalCount: Int,
    val legendItems: List<SeverityLegendItem>,
)

/**
 * Error density is `(ERROR + FATAL) / displayed entries`; an empty result set is `0%` rather than
 * an undefined division.
 */
internal fun SeveritySummaryUi.toDensityUi(): SeverityDensityUi {
    if (totalLogCount <= 0) {
        return SeverityDensityUi(
            densityPercent = 0,
            errorFraction = 0f,
            fatalFraction = 0f,
            legendItems = legendItems,
        )
    }
    return SeverityDensityUi(
        densityPercent = (((errorCount + fatalCount).toFloat() / totalLogCount) * 100).roundToInt(),
        errorFraction = errorCount.toFloat() / totalLogCount,
        fatalFraction = fatalCount.toFloat() / totalLogCount,
        legendItems = legendItems,
    )
}
