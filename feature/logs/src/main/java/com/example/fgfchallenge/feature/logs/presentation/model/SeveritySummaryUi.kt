package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityDensityUi
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import kotlin.math.roundToInt

/**
 * Severity counts for the currently displayed result set, the mapping that produces them from the
 * repository's `LogSummary`, and the mapping that turns them into the design system's
 * [SeverityDensityUi].
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
 * The legend's fixed order, from the wireframe: the two error-like severities first, then the rest.
 *
 * [Severity.UNKNOWN] is absent by design. It is valid stored data and counts toward the total, but
 * it is not a severity the product exposes, and folding it into the legend would imply it is one of
 * the five choices the filter offers.
 */
private val LEGEND_SEVERITIES: List<Severity> =
    listOf(
        Severity.ERROR,
        Severity.FATAL,
        Severity.WARN,
        Severity.INFO,
        Severity.DEBUG,
    )

/**
 * Turns the repository's full-result aggregate into the displayed summary.
 *
 * A severity with no matching row is absent from `countBySeverity`, and becomes an explicit zero
 * here so the legend keeps its shape between queries instead of losing rows as a search narrows.
 * [SeveritySummaryUi.totalLogCount] stays the query's complete count — including any `UNKNOWN`
 * rows the legend omits — because it is the denominator the density is defined against.
 */
internal fun LogSummary.toSeveritySummaryUi(): SeveritySummaryUi =
    SeveritySummaryUi(
        totalLogCount = totalCount,
        errorCount = countBySeverity[Severity.ERROR] ?: 0,
        fatalCount = countBySeverity[Severity.FATAL] ?: 0,
        legendItems =
            LEGEND_SEVERITIES.map { severity ->
                SeverityLegendItem(
                    label = severity.name,
                    count = countBySeverity[severity] ?: 0,
                    tone = severity.toBadgeTone(),
                )
            },
    )

/** The design system's tone for a stored severity; the two enums are intentionally not shared. */
internal fun Severity.toBadgeTone(): SeverityBadgeTone =
    when (this) {
        Severity.DEBUG -> SeverityBadgeTone.Debug
        Severity.INFO -> SeverityBadgeTone.Info
        Severity.WARN -> SeverityBadgeTone.Warn
        Severity.ERROR -> SeverityBadgeTone.Error
        Severity.FATAL -> SeverityBadgeTone.Fatal
        Severity.UNKNOWN -> SeverityBadgeTone.Unknown
    }

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
