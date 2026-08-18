package com.example.fgfchallenge.feature.logs.presentation.model

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.example.fgfchallenge.core.designsystem.model.AiGeneratedChoice
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetEvent
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetUi
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.Severity
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tests for the sheet's *edit* path: what one interaction does to the already-mapped sheet model, and
 * what Apply converts that back into.
 *
 * These used to be ViewModel tests, because every chip tap was an action. They are plain functions
 * now — `LogFilterSheetHost` owns the edit, so no chip tap reaches the ViewModel and none of this
 * needs a coroutine, a fake repository, or a state flow to assert.
 *
 * Two properties get their own tests and are the reason the edit path exists at all: an untouched
 * control is left as the *same instance* rather than rebuilt, and a full-width latency selection
 * converts back to no bounds, so a cleared sheet round-trips to exactly `LogFilterSelection()`.
 */
class LogFilterSheetEditTest {
    @Test
    fun `a tag chip toggles on and back off`() {
        val opened = sheetOf(LogFilterSelection())

        val selected = opened.withEvent(LogFilterSheetEvent.TagToggled("network"))
        assertThat(selected.tags.single { it.id == "network" }.selected).isTrue()

        val deselected = selected.withEvent(LogFilterSheetEvent.TagToggled("network"))
        assertThat(deselected.tags.single { it.id == "network" }.selected).isFalse()
        assertThat(deselected.toFilterSelection().tags).isEmpty()
    }

    @Test
    fun `a severity chip toggles on and back off`() {
        val opened = sheetOf(LogFilterSelection())

        val selected = opened.withEvent(LogFilterSheetEvent.SeverityToggled("ERROR"))
        assertThat(selected.toFilterSelection().severities).isEqualTo(setOf(Severity.ERROR))

        val deselected = selected.withEvent(LogFilterSheetEvent.SeverityToggled("ERROR"))
        assertThat(deselected.toFilterSelection().severities).isEmpty()
    }

    @Test
    fun `a severity chip naming no known severity is dropped rather than invented`() {
        // The sheet cannot produce such an ID, but a filter nobody selected would be worse than a
        // tap that does nothing.
        val edited = sheetOf(LogFilterSelection()).withEvent(LogFilterSheetEvent.SeverityToggled("UNKNOWN"))

        assertThat(edited.toFilterSelection().severities).isEmpty()
    }

    @Test
    fun `toggling one chip leaves every other control as the same instance`() {
        val opened = sheetOf(LogFilterSelection())

        val edited = opened.withEvent(LogFilterSheetEvent.TagToggled("network"))

        // This is the whole point of editing the mapped model instead of re-deriving it: the other
        // chips, both date fields, and the latency block are untouched, so they cannot invalidate
        // their composables and no label is reformatted.
        assertThat(edited.severities).isSameInstanceAs(opened.severities)
        assertThat(edited.start).isSameInstanceAs(opened.start)
        assertThat(edited.end).isSameInstanceAs(opened.end)
        assertThat(edited.latency).isSameInstanceAs(opened.latency)
        assertThat(edited.tags.single { it.id == "auth" }).isSameInstanceAs(opened.tags.single { it.id == "auth" })
    }

    @Test
    fun `the AI tri-state records the choice that was tapped`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.AiGeneratedSelected(AiGeneratedChoice.No))

        assertThat(edited.aiGenerated).isEqualTo(AiGeneratedChoice.No)
        assertThat(edited.toFilterSelection().aiGenerated).isEqualTo(AiGeneratedFilter.No)
    }

    @Test
    fun `the picker's UTC milliseconds become the UTC days they fall in`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.DateRangeSelected(MAY_20_UTC_MIDNIGHT, MAY_22_UTC_MIDNIGHT))
                .withEvent(LogFilterSheetEvent.StartTimeSelected(hourOfDayUtc = 17, minuteOfHourUtc = 9))
                .withEvent(LogFilterSheetEvent.EndTimeSelected(hourOfDayUtc = 17, minuteOfHourUtc = 11))

        assertThat(edited.toFilterSelection()).isEqualTo(
            LogFilterSelection(
                startDateUtc = LocalDate.of(2025, 5, 20),
                endDateUtc = LocalDate.of(2025, 5, 22),
                startTimeUtc = LocalTime.of(17, 9),
                endTimeUtc = LocalTime.of(17, 11),
            ),
        )
    }

    @Test
    fun `choosing a date keeps the time already chosen for that bound`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.StartTimeSelected(hourOfDayUtc = 6, minuteOfHourUtc = 30))
                .withEvent(LogFilterSheetEvent.DateRangeSelected(MAY_20_UTC_MIDNIGHT, null))

        val selection = edited.toFilterSelection()
        assertThat(selection.startDateUtc).isEqualTo(LocalDate.of(2025, 5, 20))
        assertThat(selection.startTimeUtc).isEqualTo(LocalTime.of(6, 30))
    }

    @Test
    fun `a cleared date range returns both bounds to unset`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.DateRangeSelected(MAY_20_UTC_MIDNIGHT, MAY_22_UTC_MIDNIGHT))
                .withEvent(LogFilterSheetEvent.DateRangeSelected(startUtcMillis = null, endUtcMillis = null))

        val selection = edited.toFilterSelection()
        assertThat(selection.startDateUtc).isNull()
        assertThat(selection.endDateUtc).isNull()
    }

    @Test
    fun `dragging the slider moves the thumbs and restates the readout`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.LatencyRangeSelected(250f..7_500f))

        assertThat(edited.latency?.selection).isEqualTo(250f..7_500f)
        assertThat(edited.latency?.selectionLabel).isEqualTo("250 – 7,500 ms")
        assertThat(edited.toFilterSelection().minimumLatencyMs).isEqualTo(250L)
        assertThat(edited.toFilterSelection().maximumLatencyMs).isEqualTo(7_500L)
    }

    @Test
    fun `slider positions snap to whole milliseconds so the readout states the value it renders`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.LatencyRangeSelected(249.6f..7_500.4f))

        assertThat(edited.latency?.selection).isEqualTo(250f..7_500f)
        assertThat(edited.latency?.selectionLabel).isEqualTo("250 – 7,500 ms")
    }

    @Test
    fun `a drag beyond the extent is held at the extent`() {
        val edited =
            sheetOf(LogFilterSelection())
                .withEvent(LogFilterSheetEvent.LatencyRangeSelected(-500f..50_000f))

        assertThat(edited.latency?.selection).isEqualTo(0f..10_000f)
    }

    @Test
    fun `a full-width latency selection converts back to no bounds at all`() {
        val untouched = sheetOf(LogFilterSelection())

        // The slider has to sit somewhere, so "unconstrained" looks like spanning the extent. Apply
        // is where that becomes the absent bounds `LogFilterSelection` uses to mean the same thing.
        assertThat(untouched.latency?.selection).isEqualTo(0f..10_000f)
        assertThat(untouched.toFilterSelection().minimumLatencyMs).isNull()
        assertThat(untouched.toFilterSelection().maximumLatencyMs).isNull()
    }

    @Test
    fun `an untouched sheet round-trips to the selection it was opened with`() {
        val applied =
            LogFilterSelection(
                tags = setOf("network"),
                severities = setOf(Severity.ERROR, Severity.FATAL),
                aiGenerated = AiGeneratedFilter.Yes,
                startDateUtc = LocalDate.of(2025, 5, 20),
                endDateUtc = LocalDate.of(2025, 5, 22),
                startTimeUtc = LocalTime.of(17, 9),
                endTimeUtc = LocalTime.of(17, 11),
                minimumLatencyMs = 250,
                maximumLatencyMs = 7_500,
            )

        // Opening the sheet and pressing Apply with no edit must not change the query, which is what
        // makes re-applying free rather than a re-query with subtly different criteria.
        assertThat(sheetOf(applied).toFilterSelection()).isEqualTo(applied)
    }

    @Test
    fun `a cleared sheet round-trips to no filters at all`() {
        val cleared = LogFilterSelection().toFilterSheetUi(OPTIONS)

        assertThat(cleared.toFilterSelection()).isEqualTo(LogFilterSelection())
    }

    @Test
    fun `Apply and Clear All are not edits and leave the model untouched`() {
        val opened = sheetOf(LogFilterSelection())

        // The host decides what these mean; `withEvent` deliberately has no opinion.
        assertThat(opened.withEvent(LogFilterSheetEvent.Applied)).isSameInstanceAs(opened)
        assertThat(opened.withEvent(LogFilterSheetEvent.Cleared)).isSameInstanceAs(opened)
    }

    @Test
    fun `a snapshot with no latency extent has no slider to edit`() {
        val noExtent = LogFilterSelection().toFilterSheetUi(LogFilterOptions(availableTags = listOf("network")))

        val edited = noExtent.withEvent(LogFilterSheetEvent.LatencyRangeSelected(250f..7_500f))

        assertThat(edited.latency).isNull()
        assertThat(edited.toFilterSelection().minimumLatencyMs).isNull()
    }

    private fun sheetOf(selection: LogFilterSelection): LogFilterSheetUi = selection.toFilterSheetUi(OPTIONS)

    private companion object {
        val OPTIONS =
            LogFilterOptions(
                availableTags = listOf("auth", "cache", "network"),
                minimumLatencyMs = 0,
                maximumLatencyMs = 10_000,
            )

        const val MAY_20_UTC_MIDNIGHT = 1_747_699_200_000L
        const val MAY_22_UTC_MIDNIGHT = 1_747_872_000_000L
    }
}
