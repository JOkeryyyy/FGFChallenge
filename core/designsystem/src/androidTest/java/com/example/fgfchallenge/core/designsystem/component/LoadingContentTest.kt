package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import org.junit.Rule
import org.junit.Test

class LoadingContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exposesOneLoadingDescriptionAndHidesSkeletonDetail() {
        // Setup: fixed skeleton layout, no parameters to vary.
        composeTestRule.setContent {
            FGFChallengeTheme {
                LoadingContent()
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val description = context.getString(R.string.loading_content_description)

        // Verify: a node with the loading description exists and is visible.
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()

        // Verify: clearAndSetSemantics on the root merges every skeleton block into one node,
        // so exactly one node carries this description — not one per skeleton block — meaning
        // a screen reader announces it once instead of reading out each placeholder.
        composeTestRule.onAllNodesWithContentDescription(description).assertCountEquals(1)
    }
}
