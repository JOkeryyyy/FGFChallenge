package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        // Verify: a header given no `onToggle` stays exactly the static heading it was. This is the
        // form `documentation/requirement.md` still describes, and every caller that has nowhere to
        // put a collapsed flag gets it by default, so it must not acquire an affordance by accident.
        composeTestRule.onNodeWithText("17:11").assertHasNoClickAction()
    }

    @Test
    fun reportsEachToggleWhenItIsCollapsible() {
        var toggles = 0
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogMinuteHeader(minute = "17:11", isCollapsed = false, onToggle = { toggles++ })
            }
        }

        composeTestRule.onNodeWithText("17:11").assertHasClickAction()
        composeTestRule.onNodeWithText("17:11").performClick()
        composeTestRule.onNodeWithText("17:11").performClick()

        // Twice, not once: the header reports taps and never decides what they mean, so a second
        // tap is a second report rather than a state it has already reached.
        assert(toggles == 2) { "expected 2 toggles, was $toggles" }
    }

    @Test
    fun stillRendersItsMinuteWhileCollapsed() {
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogMinuteHeader(minute = "17:11", isCollapsed = true, onToggle = {})
            }
        }

        // The heading of a collapsed group is the only control that can expand it again, so it has
        // to stay visible and clickable when its rows are gone.
        composeTestRule.onNodeWithText("17:11").assertIsDisplayed()
        composeTestRule.onNodeWithText("17:11").assertHasClickAction()
    }
}
