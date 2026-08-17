package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Rule
import org.junit.Test

class LogMinuteHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersMinuteAndCountWithoutAnyClickAction() {
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogMinuteHeader(minute = "17:11", itemCount = 12)
            }
        }

        composeTestRule.onNodeWithText("· 12").assertIsDisplayed()

        // Verify: the wireframe specifies static, non-collapsible minute groups. This guards
        // against an interactive affordance being reintroduced on the header.
        composeTestRule.onNodeWithText("17:11").assertHasNoClickAction()
    }
}
