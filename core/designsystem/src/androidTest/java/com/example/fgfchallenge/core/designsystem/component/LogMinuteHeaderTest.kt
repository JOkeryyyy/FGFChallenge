package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogMinuteHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noCallbackMeansNotClickable() {
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogMinuteHeader(minute = "17:11", itemCount = 12)
            }
        }

        composeTestRule.onNodeWithText("17:11").assertHasNoClickAction()
    }

    @Test
    fun callbackReceivesToggledCollapsedValue() {
        var lastValue: Boolean? = null
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogMinuteHeader(
                    minute = "17:11",
                    itemCount = 12,
                    isCollapsed = false,
                    onCollapsedChange = { lastValue = it },
                )
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val collapseDescription = context.getString(R.string.log_minute_header_collapse_action)

        composeTestRule.onNodeWithContentDescription(collapseDescription).performClick()

        assertEquals(true, lastValue)
    }
}
