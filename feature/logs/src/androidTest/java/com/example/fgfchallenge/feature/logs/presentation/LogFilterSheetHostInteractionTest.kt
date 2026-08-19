package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.ui.LogFilterSheetHost
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for `LogFilterSheetHost`: the edit it owns, and what crosses back out of it.
 *
 * These are instrumented rather than Paparazzi goldens or plain unit tests because the host's whole
 * job is the seam between real taps and a committed selection — `ModalBottomSheet` renders in its own
 * window that a single-window snapshot never captures, and the point is that a chip tap changes the
 * host's own state without anything leaving.
 *
 * The transforms themselves are covered as pure functions in `LogFilterSheetEditTest`; what is
 * asserted here is the ownership rule that motivated the host — edits stay in, only Apply comes out.
 */
class LogFilterSheetHostInteractionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val applied = mutableListOf<LogFilterSelection>()
    private var dismissals = 0

    @Test
    fun chipTapsReportNothingUntilApply() {
        setHost()

        composeTestRule.onNodeWithText("network").performClick()
        composeTestRule.onNodeWithText("ERROR").performClick()

        // The whole reason the draft moved here: a half-composed filter set never leaves the sheet,
        // so nothing recomposes the screen and nothing reaches the database.
        assertThat(applied).isEmpty()
        assertThat(dismissals).isEqualTo(0)
    }

    @Test
    fun applyReportsTheEditedSelection() {
        setHost()

        composeTestRule.onNodeWithText("network").performClick()
        composeTestRule.onNodeWithText("ERROR").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        assertThat(applied).containsExactly(
            LogFilterSelection(tags = setOf("network"), severities = setOf(Severity.ERROR)),
        )
    }

    @Test
    fun applyWithNoEditReportsTheSelectionItOpenedWith() {
        val opened = LogFilterSelection(tags = setOf("auth"), aiGenerated = AiGeneratedFilter.Yes)
        setHost(appliedFilters = opened)

        composeTestRule.onNodeWithText("Apply").performClick()

        // Pressing Apply without touching anything must round-trip exactly, or re-applying would
        // re-query with criteria the user never chose.
        assertThat(applied).containsExactly(opened)
    }

    @Test
    fun chipsToggleOffWhenTappedAgain() {
        setHost()

        composeTestRule.onNodeWithText("network").performClick()
        composeTestRule.onNodeWithText("network").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        assertThat(applied).containsExactly(LogFilterSelection())
    }

    @Test
    fun clearAllResetsTheEditWithoutCommittingIt() {
        setHost(appliedFilters = LogFilterSelection(tags = setOf("auth")))

        composeTestRule.onNodeWithText("network").performClick()
        composeTestRule.onNodeWithText("Clear All").performClick()

        // Clear All is an edit like any other: still nothing applied, and the sheet stays up.
        assertThat(applied).isEmpty()
        assertThat(dismissals).isEqualTo(0)

        composeTestRule.onNodeWithText("Apply").performClick()

        assertThat(applied).containsExactly(LogFilterSelection())
    }

    @Test
    fun theAiTriStateCommitsTheChoiceThatWasTapped() {
        setHost()

        composeTestRule.onNodeWithText("No").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        assertThat(applied).containsExactly(LogFilterSelection(aiGenerated = AiGeneratedFilter.No))
    }

    private fun setHost(appliedFilters: LogFilterSelection = LogFilterSelection()) {
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogFilterSheetHost(
                    appliedFilters = appliedFilters,
                    options = OPTIONS,
                    onApply = { applied += it },
                    onDismissRequest = { dismissals++ },
                )
            }
        }
    }

    private companion object {
        val OPTIONS =
            LogFilterOptions(
                availableTags = listOf("auth", "cache", "network"),
                minimumLatencyMs = 0,
                maximumLatencyMs = 10_000,
            )
    }
}
