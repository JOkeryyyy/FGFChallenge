package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerAction
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerFixture
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerFixtures
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerScreen
import com.example.fgfchallenge.feature.logs.presentation.ui.LogViewerUiState
import com.example.fgfchallenge.feature.logs.presentation.ui.logViewerFixtureItems
import com.example.fgfchallenge.feature.logs.presentation.ui.logViewerFixtureState
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented interaction tests for `LogViewerScreen`, driven by real gestures rather than direct
 * lambda calls: a tap goes through Compose's pointer pipeline, Back goes through the sheet's own
 * window, and the swipe goes through `ModalBottomSheet`'s drag/settle behavior.
 *
 * These live here rather than in Paparazzi because none of it is a rendering question — a golden
 * cannot press Back, and `ModalBottomSheet` renders in a separate window a single-window snapshot
 * never captures.
 *
 * The screen is stateless, so each test asserts the action it emitted; whether that action then
 * clears the selection is `LogViewerViewModelTest`'s subject. Both of the screen's inputs come from
 * one fixture, so a tapped row is a row the paged stream actually produced.
 *
 * One gap is deliberate: dismissing the *error dialog* with Back is not covered here. Espresso
 * cannot deliver the key to the dialog's window under this rule — it times out with
 * `RootViewWithoutFocusException`, which is why `:core:designsystem`'s `ErrorDialogTest` only
 * covers the dialog's buttons too. Back on the dialog reaches the same `onDismiss` the Dismiss
 * button does, and `dismissReportsErrorDismissed` covers that mapping. The sheet's Back path has no
 * such limitation and is exercised below.
 */
class LogViewerScreenInteractionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val actions = mutableListOf<LogViewerAction>()

    @Test
    fun rowTapSelectsThatRow() {
        setScreen(LogViewerFixture.AllLogs)

        composeTestRule.onNodeWithText("Connection timed out").performClick()

        assertThat(actions).containsExactly(LogViewerAction.LogSelected("1711-58123"))
    }

    @Test
    fun rowTapSelectsTheTappedRowAndNotItsNeighbour() {
        setScreen(LogViewerFixture.AllLogs)

        composeTestRule.onNodeWithText("Auth service unreachable").performClick()

        assertThat(actions).containsExactly(LogViewerAction.LogSelected("1711-46204"))
    }

    @Test
    fun selectedLogRendersItsDetails() {
        val details = LogViewerFixtures.firstAllLogsDetails()
        setScreen(LogViewerFixture.AllLogs, state = LogViewerFixtures.allLogsState().copy(selectedLog = details))

        composeTestRule.onNodeWithText("Log Details").assertIsDisplayed()
        composeTestRule.onNodeWithText(details.timestampUtc).assertIsDisplayed()
        composeTestRule.onNodeWithText(details.logId).assertIsDisplayed()
        composeTestRule.onNodeWithText(details.sessionId).assertIsDisplayed()
        composeTestRule.onNodeWithText(details.latency).assertIsDisplayed()
        assertThat(actions).isEmpty()
    }

    @Test
    fun closeButtonDismissesDetails() {
        setScreenWithSelectedLog()

        composeTestRule.onNodeWithContentDescription("Close").performClick()

        assertThat(actions).containsExactly(LogViewerAction.DetailsDismissed)
    }

    @Test
    fun backDismissesDetails() {
        setScreenWithSelectedLog()

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LogViewerAction.DetailsDismissed)
    }

    @Test
    fun swipeDownDismissesDetails() {
        setScreenWithSelectedLog()

        // Dragging the sheet's own content settles it to hidden, which is the gesture a user makes;
        // ModalBottomSheet then reports the same onDismissRequest the close button does.
        composeTestRule.onNodeWithText("Log Details").performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()

        assertThat(actions).containsExactly(LogViewerAction.DetailsDismissed)
    }

    @Test
    fun retryReportsRetryClicked() {
        setScreen(LogViewerFixture.Error)

        composeTestRule.onNodeWithText("Retry").performClick()

        assertThat(actions).containsExactly(LogViewerAction.RetryClicked)
    }

    @Test
    fun dismissReportsErrorDismissed() {
        setScreen(LogViewerFixture.Error)

        composeTestRule.onNodeWithText("Dismiss").performClick()

        assertThat(actions).containsExactly(LogViewerAction.ErrorDismissed)
    }

    @Test
    fun searchActionExpandsTheField() {
        setScreen(LogViewerFixture.AllLogs)

        // Collapsed, the field is not merely hidden behind something — it is not composed at all.
        composeTestRule.onNodeWithText("Search message or log ID").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Search logs").performClick()

        assertThat(actions).containsExactly(LogViewerAction.SearchOpened)
    }

    @Test
    fun theExpandedFieldIsShownFocusedAndCloseable() {
        setScreen(LogViewerFixture.SearchExpanded)

        // Autofocus is what makes hiding the field affordable: the tap that opened it is the same
        // intent as the tap that would have selected it.
        composeTestRule.onNodeWithText("Search message or log ID").assertIsFocused()
        composeTestRule.onNodeWithContentDescription("Close search").performClick()

        assertThat(actions).containsExactly(LogViewerAction.SearchDismissed)
    }

    @Test
    fun aCollapsedSearchStillReportsItsText() {
        // FilteredEmpty carries search text with the field collapsed, which is the state the
        // indicator exists for: the result is narrowed by a search that is out of sight.
        setScreen(LogViewerFixture.FilteredEmpty)

        composeTestRule.onNodeWithContentDescription("Search logs, search active").assertIsDisplayed()
        composeTestRule.onNodeWithText("kubernetes").assertDoesNotExist()
        assertThat(actions).isEmpty()
    }

    @Test
    fun theExpandedFieldKeepsTheQueryItWasOpenedOnto() {
        setScreen(
            LogViewerFixture.SearchExpanded,
            state = LogViewerFixtures.searchExpandedState().copy(query = "timeout"),
        )

        composeTestRule.onNodeWithText("timeout").assertIsDisplayed()
        // Expanding is a visibility change and nothing else, so it reports no query of its own.
        assertThat(actions).isEmpty()
    }

    @Test
    fun typingReportsTheQuery() {
        setScreen(LogViewerFixture.SearchExpanded)

        // performTextInput commits the whole string as one IME edit, so this asserts that the field
        // reports what was typed, not that it reports once per character.
        composeTestRule.onNodeWithText("Search message or log ID").performTextInput("net")

        assertThat(actions).containsExactly(LogViewerAction.QueryChanged("net"))
    }

    @Test
    fun clearingTheSearchFieldReportsAnEmptyQuery() {
        // The clear button only exists while a query is present, so the expanded field is opened
        // onto the text that puts it there.
        setScreen(
            LogViewerFixture.SearchExpanded,
            state = LogViewerFixtures.searchExpandedState().copy(query = "kubernetes"),
        )

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assertThat(actions).containsExactly(LogViewerAction.QueryChanged(""))
    }

    @Test
    fun filterActionOpensTheSheet() {
        setScreen(LogViewerFixture.AllLogs)

        composeTestRule.onNodeWithContentDescription("Filter").performClick()

        assertThat(actions).containsExactly(LogViewerAction.FilterSheetOpened)
    }

    @Test
    fun filterActionAnnouncesHowManyFiltersAreActive() {
        // `Filtered` applies one structured filter, which is what puts the badge on the icon; the
        // count is folded into the label rather than announced as a bare number beside it.
        setScreen(LogViewerFixture.Filtered)

        composeTestRule.onNodeWithContentDescription("Filter, 1 active filter").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Filter").assertDoesNotExist()
    }

    @Test
    fun sortControlReportsAToggle() {
        setScreen(LogViewerFixture.AllLogs)

        composeTestRule.onNodeWithContentDescription("Sorted newest first. Change to oldest first.").performClick()

        assertThat(actions).containsExactly(LogViewerAction.SortOrderToggled)
    }

    @Test
    fun sortControlNamesTheActiveAndNextOrder() {
        setScreen(
            LogViewerFixture.AllLogs,
            state = LogViewerFixtures.allLogsState().copy(sortOrder = LogSortOrder.OldestFirst),
        )

        // One icon in both directions, so the label is the only thing that says which order is in
        // effect — and, just as importantly, which one a tap produces.
        composeTestRule.onNodeWithContentDescription("Sorted oldest first. Change to newest first.").assertIsDisplayed()
    }

    @Test
    fun theTitleReportsLoadedRowsOfMatchingRows() {
        setScreen(LogViewerFixture.AllLogs)

        // Two numbers from two sources: the loaded figure counts the rows Paging holds — eleven,
        // excluding the three inserted minute headers — and the total is the aggregate over the
        // complete filtered result.
        composeTestRule.onNodeWithText("11 of 5,000 Logs").assertIsDisplayed()
    }

    @Test
    fun minuteHeaderTapReportsThatMinute() {
        setScreen(LogViewerFixture.AllLogs)

        composeTestRule.onNodeWithText("17:11").performClick()

        // The full minute ID, not the displayed `HH:mm`: two days of the snapshot share a clock
        // minute, and the label is only the tail of the group's identity.
        assertThat(actions).containsExactly(LogViewerAction.MinuteGroupToggled("2025-05-22T17:11Z"))
    }

    @Test
    fun aCollapsedMinuteKeepsItsHeaderAndDropsOnlyItsOwnRows() {
        setScreen(LogViewerFixture.CollapsedGroup)

        // The heading survives — it is the control that expands the group again — while its rows
        // are gone and the next group's are untouched.
        composeTestRule.onNodeWithText("17:11").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection timed out").assertDoesNotExist()
        composeTestRule.onNodeWithText("Auth service unreachable").assertDoesNotExist()
        composeTestRule.onNodeWithText("17:10").assertIsDisplayed()
        composeTestRule.onNodeWithText("DNS resolution failed").assertIsDisplayed()
    }

    @Test
    fun aCollapsedMinutesHeaderStillReportsTaps() {
        setScreen(LogViewerFixture.CollapsedGroup)

        composeTestRule.onNodeWithText("17:11").performClick()

        assertThat(actions).containsExactly(LogViewerAction.MinuteGroupToggled("2025-05-22T17:11Z"))
    }

    /**
     * The loaded figure counts rows the list presents, so collapsing a group lowers it while the
     * matching total — an aggregate over the complete filtered result — does not move. A collapsed
     * group is still a match; it is just not being shown.
     */
    @Test
    fun collapsingAGroupLowersTheLoadedCountAndNotTheMatchingTotal() {
        setScreen(LogViewerFixture.CollapsedGroup)

        composeTestRule.onNodeWithText("6 of 5,000 Logs").assertIsDisplayed()
    }

    private fun setScreenWithSelectedLog() {
        setScreen(
            LogViewerFixture.AllLogs,
            state = LogViewerFixtures.allLogsState().copy(selectedLog = LogViewerFixtures.firstAllLogsDetails()),
        )
    }

    /**
     * The fixture supplies both inputs, so the rows on screen are the ones its paged stream emits;
     * [state] is only overridden where a test needs a value no fixture carries, such as an open
     * details sheet.
     */
    private fun setScreen(
        fixture: LogViewerFixture,
        state: LogViewerUiState = logViewerFixtureState(fixture),
    ) {
        composeTestRule.setContent {
            FGFChallengeTheme {
                LogViewerScreen(
                    state = state,
                    logs = logViewerFixtureItems(fixture),
                    onAction = { actions += it },
                )
            }
        }
    }
}
