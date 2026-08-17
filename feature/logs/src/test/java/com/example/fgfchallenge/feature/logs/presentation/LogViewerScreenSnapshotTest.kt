package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for every required screen state at the 360dp reference width, in both themes.
 *
 * Together they pin what the milestone claims visually: skeletons only while loading, fixed-width
 * severity and tag pills with `ss.SSS` row times in the populated and filtered states, a retained
 * search field and zero-valued indicator when a search matches nothing, and a modal error carrying
 * Retry.
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
