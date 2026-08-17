package com.example.fgfchallenge.feature.logs.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.startsWith
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.toDensityUi
import org.junit.Test

/** One header paired with the rows that follow it, which is what the flat list renders. */
private typealias MinuteGroup = Pair<LogViewerListItem.MinuteHeader, List<LogViewerListItem.LogRow>>

/**
 * Contract tests for the fixture states that back the screen until Roadmap #4 wires real actions.
 *
 * They pin the values the wireframe and the dataset analysis state — summary counts, the derived
 * error density, flat-list key uniqueness and namespacing, per-group header counts, and
 * zero-result behavior — so a later edit to the fixtures cannot silently change what the previews,
 * the snapshot goldens, and the launched app claim.
 */
class LogViewerFixturesTest {
    private val resultCountLabel = "unused by these assertions"
    private val sortLabel = "unused by these assertions"

    @Test
    fun `all logs summary matches the supplied dataset severity distribution`() {
        val summary = LogViewerFixtures.allLogsSummary

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
        assertThat(summary.legendItems.sumOf { it.count }).isEqualTo(summary.totalLogCount)
    }

    @Test
    fun `all logs summary derives forty one percent error density`() {
        val density = LogViewerFixtures.allLogsSummary.toDensityUi()

        assertThat(density.densityPercent).isEqualTo(41)
        assertThat(density.errorFraction).isEqualTo(1_039f / 5_000f)
        assertThat(density.fatalFraction).isEqualTo(1_011f / 5_000f)
        assertThat(density.legendItems).isEqualTo(LogViewerFixtures.allLogsSummary.legendItems)
    }

    @Test
    fun `filtered summary matches the wireframe filtered distribution`() {
        val summary = LogViewerFixtures.filteredSummary

        assertThat(summary.totalLogCount).isEqualTo(718)
        assertThat(summary.errorCount).isEqualTo(206)
        assertThat(summary.fatalCount).isEqualTo(102)
        assertThat(summary.legendItems).containsExactly(
            SeverityLegendItem("ERROR", 206, SeverityBadgeTone.Error),
            SeverityLegendItem("FATAL", 102, SeverityBadgeTone.Fatal),
            SeverityLegendItem("WARN", 154, SeverityBadgeTone.Warn),
            SeverityLegendItem("INFO", 182, SeverityBadgeTone.Info),
            SeverityLegendItem("DEBUG", 74, SeverityBadgeTone.Debug),
        )
        assertThat(summary.legendItems.sumOf { it.count }).isEqualTo(summary.totalLogCount)
    }

    @Test
    fun `filtered summary derives forty three percent error density`() {
        val density = LogViewerFixtures.filteredSummary.toDensityUi()

        assertThat(density.densityPercent).isEqualTo(43)
        assertThat(density.errorFraction).isEqualTo(206f / 718f)
        assertThat(density.fatalFraction).isEqualTo(102f / 718f)
    }

    @Test
    fun `filtered empty summary reports zero counts and zero density`() {
        val summary = LogViewerFixtures.filteredEmptySummary
        val density = summary.toDensityUi()

        assertThat(summary.totalLogCount).isEqualTo(0)
        assertThat(summary.legendItems.sumOf { it.count }).isEqualTo(0)
        assertThat(density.densityPercent).isEqualTo(0)
        assertThat(density.errorFraction).isEqualTo(0f)
        assertThat(density.fatalFraction).isEqualTo(0f)
        // The legend keeps all five severities so the card does not change shape when a search
        // returns nothing.
        assertThat(density.legendItems).hasSize(5)
    }

    @Test
    fun `every flat list key is unique`() {
        contentFixtures().forEach { content ->
            val keys = content.items.map { it.stableKey }

            assertThat(keys).isNotEmpty()
            assertThat(keys.distinct()).hasSize(keys.size)
        }
    }

    @Test
    fun `keys are namespaced by item type`() {
        contentFixtures().forEach { content ->
            content.items.forEach { item ->
                when (item) {
                    is LogViewerListItem.MinuteHeader -> assertThat(item.stableKey).startsWith("minute:")
                    is LogViewerListItem.LogRow -> assertThat(item.stableKey).isEqualTo("log:${item.row.id}")
                }
            }
        }
    }

    @Test
    fun `minute header keys carry the full UTC minute identity`() {
        val headers = LogViewerFixtures.allLogsContent(resultCountLabel, sortLabel).minuteGroups()

        assertThat(headers.map { (header, _) -> header.stableKey }).containsExactly(
            "minute:2025-05-22T17:11Z",
            "minute:2025-05-22T17:10Z",
            "minute:2025-05-22T17:09Z",
        )
    }

    @Test
    fun `headers and rows expose distinct content types`() {
        val items = LogViewerFixtures.allLogsContent(resultCountLabel, sortLabel).items
        val headerTypes = items.filterIsInstance<LogViewerListItem.MinuteHeader>().map { it.contentType }
        val rowTypes = items.filterIsInstance<LogViewerListItem.LogRow>().map { it.contentType }

        assertThat(headerTypes.distinct()).hasSize(1)
        assertThat(rowTypes.distinct()).hasSize(1)
        assertThat(headerTypes.first()).isNotEqualTo(rowTypes.first())
    }

    @Test
    fun `each minute header counts the rows that follow it`() {
        contentFixtures().forEach { content ->
            val groups = content.minuteGroups()

            assertThat(groups).isNotEmpty()
            groups.forEach { (header, rows) ->
                assertThat(header.itemCount).isEqualTo(rows.size)
            }
        }
    }

    @Test
    fun `all logs content reproduces the wireframe minute groups`() {
        val groups = LogViewerFixtures.allLogsContent(resultCountLabel, sortLabel).minuteGroups()

        assertThat(groups.map { (header, _) -> header.minute }).containsExactly("17:11", "17:10", "17:09")
        assertThat(groups.map { (header, _) -> header.itemCount }).containsExactly(5, 5, 1)
    }

    @Test
    fun `all logs content starts blank and reports every record`() {
        val content = LogViewerFixtures.allLogsContent(resultCountLabel, sortLabel)

        assertThat(content.query).isEqualTo("")
        assertThat(LogViewerFixtures.ALL_LOGS_RESULT_COUNT).isEqualTo(5_000)
        assertThat(content.severitySummary).isEqualTo(LogViewerFixtures.allLogsSummary)
    }

    @Test
    fun `filtered content keeps the active query`() {
        val content = LogViewerFixtures.filteredContent(resultCountLabel, sortLabel)

        assertThat(content.query).isEqualTo(LogViewerFixtures.FILTERED_QUERY)
        assertThat(LogViewerFixtures.FILTERED_RESULT_COUNT).isEqualTo(718)
        assertThat(content.items.filterIsInstance<LogViewerListItem.LogRow>()).hasSize(7)
        assertThat(content.severitySummary).isEqualTo(LogViewerFixtures.filteredSummary)
    }

    @Test
    fun `filtered empty content keeps the query and renders no list items`() {
        val content = LogViewerFixtures.filteredEmptyContent(resultCountLabel, sortLabel)

        assertThat(content.query).isEqualTo(LogViewerFixtures.NONMATCHING_QUERY)
        assertThat(content.query).isNotEmpty()
        assertThat(content.items).isEmpty()
        assertThat(content.severitySummary).isEqualTo(LogViewerFixtures.filteredEmptySummary)
    }

    private fun contentFixtures(): List<LogViewerUiState.Content> =
        listOf(
            LogViewerFixtures.allLogsContent(resultCountLabel, sortLabel),
            LogViewerFixtures.filteredContent(resultCountLabel, sortLabel),
        )

    private fun LogViewerUiState.Content.minuteGroups(): List<MinuteGroup> {
        val groups = mutableListOf<Pair<LogViewerListItem.MinuteHeader, MutableList<LogViewerListItem.LogRow>>>()
        items.forEach { item ->
            when (item) {
                is LogViewerListItem.MinuteHeader -> groups += item to mutableListOf()
                is LogViewerListItem.LogRow -> groups.last().second += item
            }
        }
        return groups.map { (header, rows) -> header to rows.toList() }
    }
}
