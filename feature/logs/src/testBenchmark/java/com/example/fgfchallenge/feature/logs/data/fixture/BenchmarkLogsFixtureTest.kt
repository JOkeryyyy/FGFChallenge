package com.example.fgfchallenge.feature.logs.data.fixture

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.example.fgfchallenge.feature.logs.data.local.LogEntity
import org.junit.Test

/**
 * Pins the deterministic counts consumed by UI waits in the Macrobenchmark suite.
 *
 * The suite waits for an exact app-bar label rather than "any result", so these numbers are part of
 * the benchmark's contract: if the generator changes, the waits stop matching and every scenario
 * times out instead of quietly measuring a different query.
 */
class BenchmarkLogsFixtureTest {
    @Test
    fun `fixture has stable scenario counts and unique ids`() {
        val rows = BenchmarkLogsFixture.create()

        assertThat(rows).hasSize(100_000)
        assertThat(rows.map(LogEntity::id).toSet()).hasSize(100_000)
        assertThat(rows.first().id).isEqualTo("benchmark-log-000000")
        assertThat(rows.last().id).isEqualTo("benchmark-log-099999")
        assertThat(rows.count { "timed out" in it.message.lowercase() }).isEqualTo(20_020)
        assertThat(
            rows.count {
                it.tag == "network" &&
                    it.severity in setOf("ERROR", "FATAL") &&
                    it.isAiGenerated
            },
        ).isEqualTo(2_858)
    }

    @Test
    fun `fixture publishes the counts the benchmark waits on`() {
        assertThat(BenchmarkLogsFixture.SIZE).isEqualTo(100_000)
        assertThat(BenchmarkLogsFixture.SEARCH_MATCH_COUNT).isEqualTo(20_020)
        assertThat(BenchmarkLogsFixture.COMBINED_FILTER_MATCH_COUNT).isEqualTo(2_858)
        assertThat(BenchmarkLogsFixture.FIRST_ID).isEqualTo("benchmark-log-000000")
        assertThat(BenchmarkLogsFixture.LAST_ID).isEqualTo("benchmark-log-099999")
    }

    @Test
    fun `generating the fixture twice produces identical rows`() {
        assertThat(BenchmarkLogsFixture.create()).isEqualTo(BenchmarkLogsFixture.create())
    }
}
