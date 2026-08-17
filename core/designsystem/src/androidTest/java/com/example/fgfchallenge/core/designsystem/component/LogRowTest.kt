package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LogRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun clickInvokesCallback() {
        // Setup: onClick writes to this local var so the test can observe whether it ran.
        var clicked = false
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogRow(
                    severityLabel = "ERROR",
                    severityTone = SeverityBadgeTone.Error,
                    tagLabel = "network",
                    message = "Connection timed out",
                    time = "58.123",
                    onClick = { clicked = true },
                )
            }
        }

        // Verify: performClick dispatches a real touch through Compose's gesture pipeline,
        // not a direct call to the lambda — a Paparazzi screenshot can't exercise this.
        composeTestRule.onNodeWithText("Connection timed out").performClick()

        assertTrue(clicked)
    }
}
