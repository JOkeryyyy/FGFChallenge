package com.example.fgfchallenge.feature.logs.presentation

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme

/*
 * Shared device configurations and the single render entry point used by the log viewer's Paparazzi
 * tests, so every golden is produced by the same call with only the fixture, theme, and width
 * varying.
 */

/** 360 x 640 dp — the reference phone width every screen state is snapshotted at. */
internal val phoneDeviceConfig: DeviceConfig = DeviceConfig.NEXUS_5

/** 320 x 640 dp: the narrowest supported width, where the indicator legend stacks. */
internal val narrowDeviceConfig: DeviceConfig = DeviceConfig.NEXUS_5.copy(screenWidth = 960)

/**
 * 760 x 900 dp, the content max width, where the pane centers and the legend sits beside the ring.
 * Rendered at xhdpi rather than xxhdpi purely to keep this golden a reasonable size.
 */
internal val wideDeviceConfig: DeviceConfig =
    DeviceConfig.NEXUS_5.copy(
        screenWidth = 1_520,
        screenHeight = 1_800,
        density = Density.XHIGH,
    )

/**
 * Renders one fixture state with a discarded action callback, which is what a golden needs: these
 * pin the rendered result of a state, not what an interaction does with it.
 *
 * No fixture selects a log, so no golden covers the details sheet — `ModalBottomSheet` hosts its
 * content in a separate window that Paparazzi's single-window render never captures.
 */
internal fun Paparazzi.snapshotLogViewerScreen(
    fixture: LogViewerFixture,
    darkTheme: Boolean,
) {
    snapshot {
        FGFChallengeTheme(darkTheme = darkTheme) {
            LogViewerScreen(
                state = logViewerFixtureState(fixture),
                onAction = {},
            )
        }
    }
}
