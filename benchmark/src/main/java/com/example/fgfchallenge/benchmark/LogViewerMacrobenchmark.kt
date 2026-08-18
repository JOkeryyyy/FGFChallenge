package com.example.fgfchallenge.benchmark

import android.os.Trace
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The four measured log-viewer scenarios, run against the release-like `benchmark` app variant on
 * one documented physical device.
 *
 * Each scenario records frame timing plus one named end-to-end interaction latency, over the
 * deterministic 100,000-row Room fixture that variant installs. The protocol — device preparation,
 * commands, artifacts, and how to read the output — is `documentation/performanceBenchmark.md`.
 *
 * These results are observational. No number here is a threshold, and nothing in this file gates
 * CI, acceptance, or delivery.
 */
@RunWith(AndroidJUnit4::class)
class LogViewerMacrobenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Steady `LazyColumn` rendering inside the already-loaded first page: no append, no query, just
     * the cost of drawing rows the app already holds.
     */
    @Test
    fun scrollInitialWindow() =
        measureScenario("scrollInteraction") { robot ->
            robot.shortScrollWithinInitialPage()
            robot.assertDefaultResultStillLoaded()
        }

    /** The first boundary-directed gesture through the Room/Paging append and a stable 200 loaded. */
    @Test
    fun crossFirstPagingBoundary() =
        measureScenario("pagingBoundaryInteraction") { robot ->
            robot.scrollUntilSecondPage()
            robot.awaitSecondPage()
        }

    /**
     * Text injection through the deliberate 300 ms debounce, the `%timed out%` message/ID scan over
     * 100,000 rows, the full-result aggregate, the first page, and the rendered result.
     */
    @Test
    fun searchTimedOut() =
        measureScenario(
            traceName = "searchInteraction",
            // Revealing the field runs no query, so it stays outside the measured block.
            prepare = { robot -> robot.openSearchField() },
        ) { robot ->
            robot.searchForTimedOut()
            robot.awaitSearchResult()
        }

    /**
     * Apply through `tag = network AND severity IN (ERROR, FATAL) AND is_ai_generated = 1`, its
     * aggregate, first page, and rendered result. The draft chip taps issue no query, so they are
     * prepared rather than measured.
     */
    @Test
    fun applyCombinedFilter() =
        measureScenario(
            traceName = "combinedFilterInteraction",
            prepare = { robot -> robot.prepareNetworkErrorFatalAiFilterDraft() },
        ) { robot ->
            robot.applyPreparedFilter()
            robot.awaitCombinedFilterResult()
        }

    /**
     * The one iteration shape every scenario uses.
     *
     * `CompilationMode.Ignore()` because below API 34 the other modes can reinstall the app and
     * reset its compilation state, which would also take the seeded 100,000-row database with it.
     * The setup block therefore resets process and UI state — home, kill, relaunch, wait for the
     * default result — without ever calling `clearAppData()`, reinstalling, or reseeding Room.
     */
    @OptIn(ExperimentalMetricApi::class, ExperimentalMacrobenchmarkApi::class)
    private fun measureScenario(
        traceName: String,
        prepare: MacrobenchmarkScope.(LogViewerBenchmarkRobot) -> Unit = {},
        interaction: MacrobenchmarkScope.(LogViewerBenchmarkRobot) -> Unit,
    ) {
        benchmarkRule.measureRepeated(
            packageName = LogViewerBenchmarkRobot.TARGET_PACKAGE,
            // frameOverrunMs is deliberately absent: it needs API 31+, and the baseline device is
            // API 29. PowerMetric is absent too — system power rails are Pixel 6-class and later.
            metrics = listOf(FrameTimingMetric(), interactionMetric(traceName)),
            compilationMode = CompilationMode.Ignore(),
            iterations = ITERATIONS,
            startupMode = null,
            setupBlock = {
                pressHome()
                killProcess()
                startActivityAndWait()
                val robot = LogViewerBenchmarkRobot(device)
                robot.awaitDefaultResult()
                prepare(robot)
            },
        ) {
            val robot = LogViewerBenchmarkRobot(device)
            traceInteraction(traceName) { interaction(robot) }
        }
    }

    /**
     * The named section is emitted by the *test* process, not the app, so the metric must not be
     * restricted to the target package.
     */
    @OptIn(ExperimentalMetricApi::class)
    private fun interactionMetric(traceName: String): TraceSectionMetric =
        TraceSectionMetric(
            sectionName = traceName,
            mode = TraceSectionMetric.Mode.First,
            label = traceName,
            targetPackageOnly = false,
        )

    /**
     * Wraps one interaction in a trace section that spans the gesture and every wait it implies, so
     * the recorded duration is the user-visible latency rather than the gesture alone.
     */
    private inline fun traceInteraction(
        traceName: String,
        block: () -> Unit,
    ) {
        Trace.beginSection(traceName)
        try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    private companion object {
        const val ITERATIONS = 10
    }
}
