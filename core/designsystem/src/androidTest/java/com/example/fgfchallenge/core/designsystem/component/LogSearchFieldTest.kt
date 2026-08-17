package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LogSearchFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clearDescription: String
        get() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            return context.getString(R.string.log_search_field_clear_action)
        }

    @Test
    fun typingInvokesOnQueryChange() {
        // Setup: controlled field — query stays "", onQueryChange captures whatever it emits.
        var lastQuery: String? = null
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogSearchField(query = "", onQueryChange = { lastQuery = it }, enabled = true)
            }
        }

        // Verify: performTextInput drives Compose's real text-input path (not a direct string
        // assignment), proving the field forwards typed input instead of holding its own state.
        composeTestRule.onNode(hasSetTextAction()).performTextInput("network")

        assertEquals("network", lastQuery)
    }

    @Test
    fun clearButtonHiddenWhenQueryEmpty() {
        // Setup: empty query — the clear button should not be composed at all.
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogSearchField(query = "", onQueryChange = {}, enabled = true)
            }
        }

        // Verify: assertDoesNotExist checks the node is absent from the tree entirely, a
        // stronger claim than "not displayed".
        composeTestRule.onNodeWithContentDescription(clearDescription).assertDoesNotExist()
    }

    @Test
    fun clearButtonVisibleAndClearsQuery() {
        // Setup: pre-filled query — the clear button should now be composed and clickable.
        var lastQuery: String? = null
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogSearchField(query = "network", onQueryChange = { lastQuery = it }, enabled = true)
            }
        }

        // Verify: a real click on the clear button must emit an empty string back to the caller.
        composeTestRule.onNodeWithContentDescription(clearDescription).assertIsDisplayed().performClick()

        assertEquals("", lastQuery)
    }
}
