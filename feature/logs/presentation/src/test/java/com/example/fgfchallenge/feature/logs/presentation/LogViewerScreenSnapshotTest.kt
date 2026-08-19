package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.Paparazzi
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerFixture
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for every required screen state at the 360dp reference width, in both themes.
 *
 * Together they pin what the milestone claims visually: skeletons only while the launch refresh
 * runs, fixed-width severity and tag pills with `ss.SSS` row times under inserted UTC minute headers
 * in the populated and filtered states, the compact app bar titled with the loaded and matching
 * counts together, the filter action carrying its active-filter badge once a structured filter
 * applies, the search field expanded under the bar when it is asked for, a zero-valued indicator
 * and a `0 of 0 Logs` title when a search matches nothing, a collapsed minute group showing its
 * heading with its rows withheld, and a modal error carrying Retry — and, once that modal is
 * dismissed, a notice that keeps the failure visible and retryable over the rows it left behind.
 *
 * The Paging refresh failure is here rather than beside the append states because it renders at the
 * head of the list: the notice and the rows it must not replace are both above the fold at 640dp.
 *
 * The paging append states are not here: their UI sits at the foot of the list, past the bottom of
 * a 640dp screen, so `LogViewerScreenAppendSnapshotTest` renders them on a device tall enough to
 * show them together with the rows they must not disturb.
 */
class LogViewerScreenSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = phoneDeviceConfig)

    @Test
    fun loadingLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Loading, darkTheme = false)

    @Test
    fun loadingDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Loading, darkTheme = true)

    @Test
    fun errorLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Error, darkTheme = false)

    @Test
    fun errorDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Error, darkTheme = true)

    @Test
    fun staleSnapshotLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.StaleSnapshot, darkTheme = false)

    @Test
    fun populatedContentLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.AllLogs, darkTheme = false)

    @Test
    fun populatedContentDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.AllLogs, darkTheme = true)

    /**
     * The collapsed group's heading is the state's only visible trace: its rows are gone, the
     * headings around it are unchanged, and the app bar's matching total still counts the rows the
     * group withheld.
     */
    @Test
    fun collapsedGroupLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.CollapsedGroup, darkTheme = false)

    @Test
    fun collapsedGroupDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.CollapsedGroup, darkTheme = true)

    @Test
    fun filteredContentLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Filtered, darkTheme = false)

    @Test
    fun filteredContentDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Filtered, darkTheme = true)

    @Test
    fun filteredEmptyLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.FilteredEmpty, darkTheme = false)

    @Test
    fun filteredEmptyDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.FilteredEmpty, darkTheme = true)

    @Test
    fun searchExpandedLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.SearchExpanded, darkTheme = false)

    @Test
    fun pageRefreshErrorLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.PageRefreshError, darkTheme = false)
}
