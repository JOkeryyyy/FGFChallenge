package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for the populated state at the two widths that change its layout: the narrowest
 * supported width, where the severity legend stacks under the ring, and the content max width,
 * where the pane centers and the legend sits beside it.
 */
class LogViewerScreenWidthSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = narrowDeviceConfig)

    @Test
    fun populatedContentAtNarrowWidth() {
        paparazzi.unsafeUpdateConfig(deviceConfig = narrowDeviceConfig)

        paparazzi.snapshotLogViewerScreen(LogViewerFixture.AllLogs, darkTheme = false)
    }

    @Test
    fun populatedContentAtContentMaxWidth() {
        paparazzi.unsafeUpdateConfig(deviceConfig = wideDeviceConfig)

        paparazzi.snapshotLogViewerScreen(LogViewerFixture.AllLogs, darkTheme = false)
    }
}
