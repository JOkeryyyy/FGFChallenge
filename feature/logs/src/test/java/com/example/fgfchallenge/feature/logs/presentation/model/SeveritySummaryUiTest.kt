package com.example.fgfchallenge.feature.logs.presentation.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import com.example.fgfchallenge.feature.logs.data.model.Severity
import org.junit.Test

/**
 * Tests for the repository aggregate to displayed summary mapping.
 *
 * The interesting cases are the ones where the two representations do not line up one to one: a
 * severity the query matched no rows for, and a stored `UNKNOWN` the product does not display but
 * still has to count.
 */
class SeveritySummaryUiTest {
    @Test
    fun `counts become the legend in the wireframe's order`() {
        val summary =
            LogSummary(
                totalCount = 5_000,
                countBySeverity =
                    mapOf(
                        Severity.DEBUG to 939,
                        Severity.INFO to 1_005,
                        Severity.WARN to 1_006,
                        Severity.ERROR to 1_039,
                        Severity.FATAL to 1_011,
                    ),
            ).toSeveritySummaryUi()

        assertThat(summary.totalLogCount).isEqualTo(5_000)
        assertThat(summary.errorCount).isEqualTo(1_039)
        assertThat(summary.fatalCount).isEqualTo(1_011)
        assertThat(summary.legendItems).containsExactly(
            SeverityLegendItem("ERROR", 1_039, SeverityBadgeTone.Error),
            SeverityLegendItem("FATAL", 1_011, SeverityBadgeTone.Fatal),
            SeverityLegendItem("WARN", 1_006, SeverityBadgeTone.Warn),
            SeverityLegendItem("INFO", 1_005, SeverityBadgeTone.Info),
            SeverityLegendItem("DEBUG", 939, SeverityBadgeTone.Debug),
        )
    }

    @Test
    fun `a severity the query matched nothing for is an explicit zero, not a missing row`() {
        val summary =
            LogSummary(
                totalCount = 2,
                countBySeverity = mapOf(Severity.ERROR to 2),
            ).toSeveritySummaryUi()

        // The legend keeps its shape as a search narrows; rows do not appear and disappear.
        assertThat(summary.legendItems.map { it.label })
            .containsExactly("ERROR", "FATAL", "WARN", "INFO", "DEBUG")
        assertThat(summary.legendItems.map { it.count }).containsExactly(2, 0, 0, 0, 0)
    }

    @Test
    fun `an unknown severity counts toward the total but is neither displayed nor treated as an error`() {
        val summary =
            LogSummary(
                totalCount = 10,
                countBySeverity = mapOf(Severity.ERROR to 1, Severity.UNKNOWN to 9),
            ).toSeveritySummaryUi()

        assertThat(summary.totalLogCount).isEqualTo(10)
        assertThat(summary.errorCount).isEqualTo(1)
        assertThat(summary.fatalCount).isEqualTo(0)
        assertThat(summary.legendItems.map { it.label })
            .containsExactly("ERROR", "FATAL", "WARN", "INFO", "DEBUG")
        // 1 of 10, not 1 of 1: the unknown rows are part of the denominator.
        assertThat(summary.toDensityUi().densityPercent).isEqualTo(10)
    }

    @Test
    fun `an empty result is zero percent rather than an undefined division`() {
        val summary = LogSummary(totalCount = 0, countBySeverity = emptyMap()).toSeveritySummaryUi()

        assertThat(summary.toDensityUi().densityPercent).isEqualTo(0)
        assertThat(summary.toDensityUi().errorFraction).isEqualTo(0f)
    }
}
