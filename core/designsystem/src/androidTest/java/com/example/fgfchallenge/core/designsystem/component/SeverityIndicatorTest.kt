package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityDensityUi
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Rule
import org.junit.Test

class SeverityIndicatorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exposesSuppliedDensityAndLegendInOneContentDescription() {
        val legendItems =
            listOf(
                SeverityLegendItem("ERROR", 20, SeverityBadgeTone.Error),
                SeverityLegendItem("FATAL", 21, SeverityBadgeTone.Fatal),
                SeverityLegendItem("WARN", 20, SeverityBadgeTone.Warn),
                SeverityLegendItem("INFO", 20, SeverityBadgeTone.Info),
                SeverityLegendItem("DEBUG", 19, SeverityBadgeTone.Debug),
            )
        // Setup: presentation has already reduced 20 ERROR + 21 FATAL of 100 to 41% and the
        // matching ring fractions, so the component only has to render them.
        composeTestRule.setContent {
            FGFChallengeTheme {
                SeverityIndicator(
                    density =
                        SeverityDensityUi(
                            densityPercent = 41,
                            errorFraction = 0.20f,
                            fatalFraction = 0.21f,
                            legendItems = legendItems,
                        ),
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.severity_indicator_description, 41)

        // Verify: the ring is pure Canvas drawing with no text nodes, so the only way to check
        // the rendered percentage is to read it back off the semantics description.
        composeTestRule
            .onNodeWithContentDescription(expectedPrefix, substring = true)
            .assertIsDisplayed()

        // Verify: the legend is described by the same node, so a screen reader gets the ring and
        // the counts as one announcement rather than a bare percentage.
        composeTestRule
            .onNodeWithContentDescription("ERROR 20", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun zeroDensityProducesZeroPercentDescription() {
        // Setup: the empty-result case. The divide-by-zero guard now lives in presentation, so
        // what this covers is the component rendering a 0% ring with no legend at all.
        composeTestRule.setContent {
            FGFChallengeTheme {
                SeverityIndicator(
                    density =
                        SeverityDensityUi(
                            densityPercent = 0,
                            errorFraction = 0f,
                            fatalFraction = 0f,
                            legendItems = emptyList(),
                        ),
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedPrefix = context.getString(R.string.severity_indicator_description, 0)

        composeTestRule
            .onNodeWithContentDescription(expectedPrefix, substring = true)
            .assertIsDisplayed()
    }
}
