package com.example.fgfchallenge.feature.logs.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.model.LogViewerListItem
import com.example.fgfchallenge.feature.logs.presentation.model.toDensityUi
import org.junit.Test

/** One header paired with the rows that follow it, which is what the flat list renders. */
private typealias MinuteGroup = Pair<LogViewerListItem.MinuteHeader, List<LogViewerListItem.LogRow>>

/**
 * Contract tests for the fixture states and rows that back the previews and the goldens.
 *
 * They pin the values the wireframe and the dataset analysis state — summary counts, the derived
 * error density, flat-list key uniqueness and namespacing, the minute grouping, and the shape of
 * each screen state — so a later edit to the fixtures cannot silently change what any of those
 * consumers claim. What the fixtures *derive* rather than declare is covered next door in
 * `LogEntryUiMapperTest`, against the same production mapping they run through.
 */
class LogViewerFixturesTest {
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
        itemFixtures().forEach { items ->
            val keys = items.map { it.stableKey }

            assertThat(keys).isNotEmpty()
            assertThat(keys.distinct()).hasSize(keys.size)
        }
    }

    @Test
    fun `keys are namespaced by item type`() {
        itemFixtures().forEach { items ->
            items.forEach { item ->
                when (item) {
                    is LogViewerListItem.MinuteHeader -> assertThat(item.stableKey).startsWith("minute:")
                    is LogViewerListItem.LogRow -> assertThat(item.stableKey).isEqualTo("log:${item.row.id}")
                }
            }
        }
    }

    @Test
    fun `minute header keys carry the full UTC minute identity`() {
        val headers = LogViewerFixtures.allLogsItems.minuteGroups()

        assertThat(headers.map { (header, _) -> header.stableKey }).containsExactly(
            "minute:2025-05-22T17:11Z",
            "minute:2025-05-22T17:10Z",
            "minute:2025-05-22T17:09Z",
        )
    }

    @Test
    fun `headers and rows expose distinct content types`() {
        val items = LogViewerFixtures.allLogsItems
        val headerTypes = items.filterIsInstance<LogViewerListItem.MinuteHeader>().map { it.contentType }
        val rowTypes = items.filterIsInstance<LogViewerListItem.LogRow>().map { it.contentType }

        assertThat(headerTypes.distinct()).hasSize(1)
        assertThat(rowTypes.distinct()).hasSize(1)
        assertThat(headerTypes.first()).isNotEqualTo(rowTypes.first())
    }

    @Test
    fun `all logs content reproduces the wireframe minute groups`() {
        val groups = LogViewerFixtures.allLogsItems.minuteGroups()

        assertThat(groups.map { (header, _) -> header.minute }).containsExactly("17:11", "17:10", "17:09")
        assertThat(groups.map { (_, rows) -> rows.size }).containsExactly(5, 5, 1)
    }

    @Test
    fun `every row sits under the header for its own minute`() {
        itemFixtures().forEach { items ->
            items.minuteGroups().forEach { (header, rows) ->
                rows.forEach { row ->
                    assertThat(row.utcMinuteId).isEqualTo(header.utcMinuteId)
                }
            }
        }
    }

    @Test
    fun `all logs state starts blank, newest first, unselected, and reports every record`() {
        val state = LogViewerFixtures.allLogsState()

        assertThat(state.query).isEqualTo("")
        assertThat(state.sortOrder).isEqualTo(LogSortOrder.NewestFirst)
        assertThat(state.selectedLog).isNull()
        assertThat(state.refresh).isEqualTo(LogViewerRefreshState.Complete)
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Ready(LogViewerFixtures.allLogsSummary))
        assertThat(LogViewerFixtures.ALL_LOGS_RESULT_COUNT).isEqualTo(5_000)
    }

    @Test
    fun `filtered state applies one structured filter and reports it as active`() {
        val state = LogViewerFixtures.filteredState()

        // Narrowed by a tag rather than by search text: the field searches message or ID only, so a
        // sample of `network`-tagged rows can only be reached through the filter that selects them.
        assertThat(state.query).isEmpty()
        assertThat(state.filters.tags).isEqualTo(setOf(LogViewerFixtures.FILTERED_TAG))
        assertThat(state.activeFilterCount).isEqualTo(1)
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Ready(LogViewerFixtures.filteredSummary))
        assertThat(LogViewerFixtures.FILTERED_RESULT_COUNT).isEqualTo(718)
        assertThat(LogViewerFixtures.filteredItems.filterIsInstance<LogViewerListItem.LogRow>()).hasSize(7)
    }

    @Test
    fun `filtered empty state keeps the query and counts zero matches`() {
        val state = LogViewerFixtures.filteredEmptyState()

        assertThat(state.query).isEqualTo(LogViewerFixtures.NONMATCHING_QUERY)
        assertThat(state.query).isNotEmpty()
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Ready(LogViewerFixtures.filteredEmptySummary))
        // Counted zero, not merely uncounted: this is the condition the no-results state waits for.
        assertThat(state.hasNoMatches).isTrue()
    }

    @Test
    fun `the search expanded state shows the field without narrowing anything`() {
        val state = LogViewerFixtures.searchExpandedState()

        // Expanding search is a visibility change and nothing else, so the sample that shows the
        // field open reports the same unfiltered result the collapsed one does.
        assertThat(state.isSearchExpanded).isTrue()
        assertThat(state.query).isEmpty()
        assertThat(state.activeFilterCount).isEqualTo(0)
        assertThat(state.summary).isEqualTo(LogViewerSummaryState.Ready(LogViewerFixtures.allLogsSummary))
    }

    @Test
    fun `loading and error states report no counted summary`() {
        assertThat(LogViewerFixtures.loadingState.refresh).isEqualTo(LogViewerRefreshState.InProgress)
        assertThat(LogViewerFixtures.loadingState.summary).isEqualTo(LogViewerSummaryState.Pending)
        assertThat(LogViewerFixtures.loadingState.hasNoMatches).isFalse()

        val errorState = LogViewerFixtures.errorState()
        assertThat(errorState.refresh).isEqualTo(LogViewerRefreshState.Failed(dismissed = false))
        assertThat(errorState.showsRefreshFailure).isTrue()
        assertThat(errorState.showsStaleSnapshotNotice).isFalse()
    }

    @Test
    fun `a dismissed failure keeps the failure and swaps the dialog for the stale notice`() {
        val stale = LogViewerFixtures.staleSnapshotState()

        assertThat(stale.refresh).isEqualTo(LogViewerRefreshState.Failed(dismissed = true))
        assertThat(stale.showsRefreshFailure).isFalse()
        assertThat(stale.showsStaleSnapshotNotice).isTrue()
        // The retained snapshot is still queryable underneath, which is what the notice qualifies.
        assertThat(stale.summary).isEqualTo(LogViewerSummaryState.Ready(LogViewerFixtures.allLogsSummary))
    }

    @Test
    fun `the first all-logs entry helper returns the entry the first row renders`() {
        val firstRow = LogViewerFixtures.allLogsItems.filterIsInstance<LogViewerListItem.LogRow>().first()

        assertThat(LogViewerFixtures.firstAllLogsEntry().id).isEqualTo(firstRow.row.id)
        assertThat(LogViewerFixtures.firstAllLogsDetails().logId).isEqualTo(firstRow.row.id)
    }

    private fun itemFixtures(): List<List<LogViewerListItem>> =
        listOf(
            LogViewerFixtures.allLogsItems,
            LogViewerFixtures.filteredItems,
        )

    private fun List<LogViewerListItem>.minuteGroups(): List<MinuteGroup> {
        val groups = mutableListOf<Pair<LogViewerListItem.MinuteHeader, MutableList<LogViewerListItem.LogRow>>>()
        forEach { item ->
            when (item) {
                is LogViewerListItem.MinuteHeader -> groups += item to mutableListOf()
                is LogViewerListItem.LogRow -> groups.last().second += item
            }
        }
        return groups.map { (header, rows) -> header to rows.toList() }
    }
}
