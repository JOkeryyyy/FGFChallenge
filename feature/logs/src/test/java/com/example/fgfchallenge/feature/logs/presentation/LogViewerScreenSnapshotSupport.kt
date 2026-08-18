package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
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
 * 360 x 1400 dp: the reference width on a screen tall enough that the whole fixture list and the
 * item below it are on screen at once, which is what the append states need to be visible at all.
 * Rendered at xhdpi rather than xxhdpi to keep the golden a reasonable size.
 */
internal val tallDeviceConfig: DeviceConfig =
    DeviceConfig.NEXUS_5.copy(
        screenWidth = 720,
        screenHeight = 2_800,
        density = Density.XHIGH,
    )

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
 * Renders one fixture with a discarded action callback, which is what a golden needs: these pin the
 * rendered result of a state, not what an interaction does with it.
 *
 * Both of the screen's inputs come from the same fixture, and the paged half is collected through
 * the real `collectAsLazyPagingItems`, so a golden exercises the actual Paging path — keys, content
 * types, inserted minute headers, and append load states included — rather than a list dressed up
 * to look like one.
 *
 * No fixture selects a log or opens a filter draft, so no golden covers either sheet —
 * `ModalBottomSheet` hosts its content in a separate window that Paparazzi's single-window render
 * never captures. What the goldens do pin from the filter work is its entry point: the Filter
 * control and its active-filter badge render in the screen's own window.
 *
 * The render declares itself an inspection, which a golden is. It matters for exactly one state:
 * the expanded search field takes focus as it appears, and focusing a field opens a text-input
 * session whose IME `HandlerThread` layoutlib cannot run on the host JVM. So these goldens pin where
 * the field sits and what it shows, and `LogViewerScreenInteractionTest` — which drives a real
 * window — pins that it arrives focused. Nothing else on this screen reads inspection mode.
 */
internal fun Paparazzi.snapshotLogViewerScreen(
    fixture: LogViewerFixture,
    darkTheme: Boolean,
) {
    snapshot {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            FGFChallengeTheme(darkTheme = darkTheme) {
                LogViewerScreen(
                    state = logViewerFixtureState(fixture),
                    logs = logViewerFixtureItems(fixture),
                    onAction = {},
                )
            }
        }
    }
}
