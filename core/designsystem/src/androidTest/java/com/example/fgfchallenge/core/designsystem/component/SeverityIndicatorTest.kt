package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Rule
import org.junit.Test

class SeverityIndicatorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exposesCompleteContentDescriptionForTheCanvasRing() {
        val legendItems =
            listOf(
                SeverityLegendItem("ERROR", 20, SeverityBadgeTone.Error),
                SeverityLegendItem("FATAL", 21, SeverityBadgeTone.Fatal),
                SeverityLegendItem("WARN", 20, SeverityBadgeTone.Warn),
                SeverityLegendItem("INFO", 20, SeverityBadgeTone.Info),
                SeverityLegendItem("DEBUG", 19, SeverityBadgeTone.Debug),
            )
        // Setup: render the ring with a known error/fatal/total combination.
        composeTestRule.setContent {
            FGFChallengeTheme {
                SeverityIndicator(
                    totalLogCount = 100,
                    errorCount = 20,
                    fatalCount = 21,
                    legendItems = legendItems,
                )
            }
        }

        // (20 + 21) / 100 = 41%, matching the density formula in ARCHITECTURE.md.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.severity_indicator_description, 41)

        // Verify: the ring is pure Canvas drawing with no text nodes, so the only way to check
        // the baked-in percentage is correct is to read it back off the semantics description.
        composeTestRule
            .onNodeWithContentDescription(expectedPrefix, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun zeroTotalLogsProducesZeroPercentDescription() {
        // Setup: all-zero edge case — guards the (error + fatal) / total division by zero.
        composeTestRule.setContent {
            FGFChallengeTheme {
                SeverityIndicator(
                    totalLogCount = 0,
                    errorCount = 0,
                    fatalCount = 0,
                    legendItems = emptyList(),
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.severity_indicator_description, 0)

        // Verify: falls back to 0% instead of crashing or reporting NaN.
        composeTestRule
            .onNodeWithContentDescription(expectedPrefix, substring = true)
            .assertIsDisplayed()
    }
}
