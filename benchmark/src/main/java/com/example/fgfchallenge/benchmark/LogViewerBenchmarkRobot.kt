package com.example.fgfchallenge.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Drives only stable accessibility/resource semantics exposed by the measured app.
 *
 * Every selector is either one of the four resource-style test tags the screen publishes, or a
 * visible label the product itself defines. Nothing here reaches into the app's internals — an
 * out-of-process benchmark has the same view of the UI a person does, which is the point.
 *
 * Every wait is an assertion. A scenario that silently proceeds without reaching its expected state
 * would still produce numbers, and those numbers would describe something other than the query the
 * scenario claims to measure, so each wait fails loudly instead.
 */
internal class LogViewerBenchmarkRobot(
    private val device: UiDevice,
) {
    /**
     * The unfiltered default. The generous timeout is for the very first benchmark launch, which
     * generates and inserts the 100,000-row fixture before the viewer can show anything.
     */
    fun awaitDefaultResult() = awaitResult(DEFAULT_RESULT, LAUNCH_TIMEOUT_MS)

    fun awaitSecondPage() = awaitResult(SECOND_PAGE_RESULT, INTERACTION_TIMEOUT_MS)

    fun awaitSearchResult() = awaitResult(SEARCH_RESULT, INTERACTION_TIMEOUT_MS)

    fun awaitCombinedFilterResult() = awaitResult(COMBINED_FILTER_RESULT, INTERACTION_TIMEOUT_MS)

    /**
     * Confirms the scroll scenario never crossed the first Paging boundary, which is what separates
     * it from the boundary scenario: steady `LazyColumn` rendering, with no append work in the
     * measured window.
     */
    fun assertDefaultResultStillLoaded() = awaitResult(DEFAULT_RESULT, SETTLE_TIMEOUT_MS)

    /**
     * One short, deliberately slow swipe inside the initial 100-row window.
     *
     * Slow because a fling would carry the list far enough to trigger the prefetch and turn this
     * into the Paging scenario. [assertDefaultResultStillLoaded] is what proves it did not.
     */
    fun shortScrollWithinInitialPage() {
        val list = requireList()
        list.swipe(Direction.UP, SHORT_SWIPE_PERCENT, SLOW_SWIPE_SPEED)
        device.waitForIdle(SETTLE_TIMEOUT_MS)
    }

    /**
     * Swipes until the first append lands, with a hard bound so a broken selector fails instead of
     * scrolling forever.
     */
    fun scrollUntilSecondPage() {
        val list = requireList()
        repeat(MAX_BOUNDARY_SWIPES) {
            if (device.hasObject(resultSelector(SECOND_PAGE_RESULT))) {
                return
            }
            list.swipe(Direction.UP, BOUNDARY_SWIPE_PERCENT)
        }
        check(device.wait(Until.hasObject(resultSelector(SECOND_PAGE_RESULT)), INTERACTION_TIMEOUT_MS)) {
            "The first paging boundary was not crossed after $MAX_BOUNDARY_SWIPES swipes"
        }
    }

    /**
     * Reveals the search field, which the app bar hides until it is asked for.
     *
     * This is setup, not measurement: expanding the field runs no query. The measured block starts
     * at the text itself.
     */
    fun openSearchField() {
        requireObject(By.desc(SEARCH_ACTION_DESCRIPTION), "search action").click()
        check(device.wait(Until.hasObject(By.res(SEARCH_TAG)), INTERACTION_TIMEOUT_MS)) {
            "The search field never appeared after tapping the search action"
        }
    }

    /**
     * Injects the whole term at once through the accessibility set-text action, so the measurement
     * covers the app's own debounce and query rather than a simulated typing cadence.
     */
    fun searchForTimedOut() {
        requireObject(By.res(SEARCH_TAG), "search field").text = SEARCH_TERM
    }

    /**
     * Builds the combined draft — `network`, `ERROR`, `FATAL`, AI `Yes` — and leaves Apply visible
     * but untapped.
     *
     * Chip taps only edit the draft the ViewModel holds; none of them issues a query, so measuring
     * them would dilute the Apply latency this scenario exists to report.
     */
    fun prepareNetworkErrorFatalAiFilterDraft() {
        requireObject(By.res(FILTER_TAG), "filter action").click()
        check(device.wait(Until.hasObject(By.text(SHEET_TITLE)), INTERACTION_TIMEOUT_MS)) {
            "The filter sheet never opened"
        }
        for (label in DRAFT_LABELS) {
            scrollSheetUntilVisible(label).click()
        }
        scrollSheetUntilVisible(APPLY_LABEL)
    }

    fun applyPreparedFilter() {
        requireObject(By.text(APPLY_LABEL), APPLY_LABEL).click()
    }

    private fun awaitResult(
        expected: String,
        timeoutMs: Long,
    ) {
        check(device.wait(Until.hasObject(resultSelector(expected)), timeoutMs)) {
            "Timed out waiting for result label: $expected"
        }
    }

    /**
     * The label is matched by tag *and* exact text, so a stale count from the previous generation
     * can never satisfy a wait for the new one.
     */
    private fun resultSelector(expected: String): BySelector = By.res(RESULT_TAG).text(expected)

    private fun requireList(): UiObject2 =
        requireObject(By.res(LIST_TAG), "log list").also { list ->
            // Keeps the gesture clear of the system's own edge handlers, which would otherwise
            // consume a swipe that starts too close to the display border.
            list.setGestureMargin(list.visibleBounds.width() / GESTURE_MARGIN_DIVISOR)
        }

    private fun requireObject(
        selector: BySelector,
        description: String,
    ): UiObject2 =
        checkNotNull(device.wait(Until.findObject(selector), INTERACTION_TIMEOUT_MS)) {
            "Could not find the $description"
        }

    /**
     * The filter sheet is taller than a phone screen, so a control may start off-screen. Scrolling
     * only ever goes downward because the labels are tapped in the order the sheet lays them out.
     */
    private fun scrollSheetUntilVisible(label: String): UiObject2 {
        repeat(MAX_SHEET_SWIPES) {
            val found = device.findObject(By.text(label))
            if (found != null) {
                return found
            }
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * SHEET_SWIPE_START_NUMERATOR / SHEET_SWIPE_DENOMINATOR,
                device.displayWidth / 2,
                device.displayHeight * SHEET_SWIPE_END_NUMERATOR / SHEET_SWIPE_DENOMINATOR,
                SHEET_SWIPE_STEPS,
            )
            device.waitForIdle(SETTLE_TIMEOUT_MS)
        }
        return checkNotNull(device.findObject(By.text(label))) {
            "\"$label\" was never reached in the filter sheet"
        }
    }

    internal companion object {
        const val TARGET_PACKAGE = "com.example.fgfchallenge"

        // The four resource-style tags LogViewerScreen publishes.
        const val SEARCH_TAG = "log_viewer_search"
        const val FILTER_TAG = "log_viewer_filter"
        const val RESULT_TAG = "log_viewer_result"
        const val LIST_TAG = "log_viewer_list"

        /**
         * The app bar's `%1$s of %2$s Logs` title, resolved for each scenario's fixed fixture
         * counts. `BenchmarkLogsFixture` pins the totals these are built from.
         */
        const val DEFAULT_RESULT = "100 of 100,000 Logs"
        const val SECOND_PAGE_RESULT = "200 of 100,000 Logs"
        const val SEARCH_RESULT = "100 of 20,020 Logs"
        const val COMBINED_FILTER_RESULT = "100 of 2,858 Logs"

        private const val SEARCH_ACTION_DESCRIPTION = "Search logs"
        private const val SEARCH_TERM = "timed out"
        private const val SHEET_TITLE = "Filters"
        private const val APPLY_LABEL = "Apply"
        private val DRAFT_LABELS = listOf("network", "ERROR", "FATAL", "Yes")

        private const val LAUNCH_TIMEOUT_MS = 120_000L
        private const val INTERACTION_TIMEOUT_MS = 30_000L
        private const val SETTLE_TIMEOUT_MS = 5_000L

        private const val SHORT_SWIPE_PERCENT = 0.3f
        private const val BOUNDARY_SWIPE_PERCENT = 0.8f

        /** Pixels per second. Low enough that the short scroll does not turn into a fling. */
        private const val SLOW_SWIPE_SPEED = 1_000

        private const val MAX_BOUNDARY_SWIPES = 30
        private const val MAX_SHEET_SWIPES = 10
        private const val GESTURE_MARGIN_DIVISOR = 5
        private const val SHEET_SWIPE_START_NUMERATOR = 3
        private const val SHEET_SWIPE_END_NUMERATOR = 1
        private const val SHEET_SWIPE_DENOMINATOR = 4
        private const val SHEET_SWIPE_STEPS = 20
    }
}
