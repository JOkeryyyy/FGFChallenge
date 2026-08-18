package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for every required screen state at the 360dp reference width, in both themes.
 *
 * Together they pin what the milestone claims visually: skeletons only while the launch refresh
 * runs, fixed-width severity and tag pills with `ss.SSS` row times under inserted UTC minute headers
 * in the populated and filtered states, a result line naming the matching and loaded counts
 * separately, a retained search field and zero-valued indicator when a search matches nothing, and a
 * modal error carrying Retry — and, once that modal is dismissed, a notice that keeps the failure
 * visible and retryable over the rows it left behind.
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

    @Test
    fun filteredContentLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Filtered, darkTheme = false)

    @Test
    fun filteredContentDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.Filtered, darkTheme = true)

    @Test
    fun filteredEmptyLight() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.FilteredEmpty, darkTheme = false)

    @Test
    fun filteredEmptyDark() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.FilteredEmpty, darkTheme = true)
}
