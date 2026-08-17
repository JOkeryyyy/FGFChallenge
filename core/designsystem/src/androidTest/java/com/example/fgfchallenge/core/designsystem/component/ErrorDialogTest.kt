package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ErrorDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun retryButtonInvokesOnRetry() {
        var retried = false
        composeTestRule.setContent {
            FGFChallengeTheme {
                ErrorDialog(
                    title = "Unable to load logs",
                    message = "We couldn't fetch logs from the server.",
                    onRetry = { retried = true },
                    onDismiss = {},
                )
            }
        }

        val retryLabel = context.getString(R.string.error_dialog_retry_action)
        composeTestRule.onNodeWithText(retryLabel).performClick()

        assertTrue(retried)
    }

    @Test
    fun dismissButtonInvokesOnDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            FGFChallengeTheme {
                ErrorDialog(
                    title = "Unable to load logs",
                    message = "We couldn't fetch logs from the server.",
                    onRetry = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        val dismissLabel = context.getString(R.string.error_dialog_dismiss_action)
        composeTestRule.onNodeWithText(dismissLabel).performClick()

        assertTrue(dismissed)
    }
}
