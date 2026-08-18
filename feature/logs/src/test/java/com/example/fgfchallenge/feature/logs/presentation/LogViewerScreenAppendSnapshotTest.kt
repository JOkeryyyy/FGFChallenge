package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for the two Paging append states, on a screen tall enough to show them.
 *
 * They are separated from the other screen goldens for that reason alone: the append UI is the last
 * item of the list, which on a 640dp phone sits well below the fold — a golden taken there is
 * pixel-identical to the settled state and pins nothing.
 *
 * What they do pin is the requirement that reaching the end of a page, or failing to load the next
 * one, costs the user nothing: in both, every row that was already loaded is still rendered above
 * the footer, and the failure offers Retry rather than replacing the list with an error.
 *
 * Light theme only — the states are structural, and the themes they render in are already covered
 * by the settled goldens next door.
 */
class LogViewerScreenAppendSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = tallDeviceConfig)

    @Test
    fun appendProgress() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.AppendLoading, darkTheme = false)

    @Test
    fun appendRetry() = paparazzi.snapshotLogViewerScreen(LogViewerFixture.AppendError, darkTheme = false)
}
