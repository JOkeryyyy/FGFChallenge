package com.example.fgfchallenge

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the app launches into the log viewer showing its populated fixture: the wireframe title,
 * the result count, the first minute group, and a log row from it.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndShowsLogViewerTitle() {
        composeRule
            .onNodeWithText("Semantic Logs")
            .assertIsDisplayed()
    }

    @Test
    fun appLaunchesWithPopulatedFixtureContent() {
        composeRule.onNodeWithText("5,000 results").assertIsDisplayed()
        composeRule.onNodeWithText("17:11").assertIsDisplayed()
        composeRule.onNodeWithText("Connection timed out").assertIsDisplayed()
    }
}
