package com.example.fgfchallenge.feature.logs.data.fixture

import com.example.fgfchallenge.feature.logs.data.local.LogEntity

/**
 * Builds the fixed 100,000-row Room-only dataset used by physical-device Macrobenchmarks.
 *
 * It exists only in the `benchmark` source set, so no shipping variant can generate it. Every field
 * is a pure function of the row index — no clock, no randomness, no seed — because the benchmark
 * waits on exact result labels: the same commit must produce the same counts on every device and
 * every run, or a timing comparison would be comparing two different queries.
 *
 * The field schedule is chosen so one 70-row block covers every tag/severity/AI combination, which
 * is what makes [SEARCH_MATCH_COUNT] and [COMBINED_FILTER_MATCH_COUNT] stable and non-trivial
 * fractions of the table rather than everything or almost nothing.
 */
internal object BenchmarkLogsFixture {
    const val SIZE: Int = 100_000

    /** Rows whose message contains `timed out`, which the search scenario waits for. */
    const val SEARCH_MATCH_COUNT: Int = 20_020

    /** Rows matching `tag = network AND severity IN (ERROR, FATAL) AND is_ai_generated = 1`. */
    const val COMBINED_FILTER_MATCH_COUNT: Int = 2_858

    /** Sentinel IDs: present together with [SIZE], they mean the fixture is installed and whole. */
    const val FIRST_ID: String = "benchmark-log-000000"
    const val LAST_ID: String = "benchmark-log-099999"

    private val severities = listOf("DEBUG", "INFO", "WARN", "ERROR", "FATAL")
    private val tags = listOf("ui", "db", "cache", "network", "auth", "sync", "neural_engine")
    private val messages =
        listOf(
            "User logged in",
            "Cache miss",
            "Connection timed out",
            "Inference complete",
            "Invalid token",
        )

    fun create(): List<LogEntity> =
        List(SIZE) { index ->
            LogEntity(
                // Zero-padded so lexicographic ID order matches numeric order, the way the paged
                // select's ID tie-breaker assumes.
                id = "benchmark-log-${index.toString().padStart(6, '0')}",
                // 2025-01-01T00:00:00Z, one second apart: 100,000 distinct UTC minutes' worth of
                // rows, so minute headers appear at a realistic density while scrolling.
                timestampEpochMillis = 1_735_689_600_000L + index * 1_000L,
                severity = severities[index % severities.size],
                tag = tags[(index / severities.size) % tags.size],
                message = messages[(index / 70) % messages.size],
                latencyMs = (index * 37L) % 10_001L,
                isAiGenerated = (index / 35) % 2 == 0,
                sessionId = "benchmark-session",
            )
        }
}
