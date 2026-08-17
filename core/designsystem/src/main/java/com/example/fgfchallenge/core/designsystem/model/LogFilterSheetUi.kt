package com.example.fgfchallenge.core.designsystem.model

import androidx.compose.runtime.Immutable

/**
 * Display-ready values for the structured filter sheet, and the events it reports back.
 *
 * The sheet is stateless in the same sense as the rest of the design system: everything it renders
 * arrives in [LogFilterSheetUi] already resolved — which chips exist, which are selected, what the
 * date and time fields read, where the latency thumbs sit — and every interaction leaves as a
 * [LogFilterSheetEvent]. The component never decides that tapping a chip selects it; the caller's
 * state producer does, which is what lets one ViewModel own the draft the user is editing.
 *
 * The values are deliberately neutral: labels are formatted strings, dates are UTC epoch
 * milliseconds, times are hour/minute pairs, and latency is the slider's own `Float` domain. None
 * of `java.time`, the log entity, or the repository's models reaches this module.
 */
@Immutable
data class LogFilterSheetUi(
    /** Tag chips from the stored snapshot; empty when no snapshot has been imported yet. */
    val tags: List<LogFilterOptionUi>,
    /** The five product-defined severity chips, in the order they should render. */
    val severities: List<LogFilterOptionUi>,
    val aiGenerated: AiGeneratedChoice,
    val start: LogFilterDateTimeUi,
    val end: LogFilterDateTimeUi,
    /** `null` when the snapshot exposes no usable latency extent, which renders as unavailable. */
    val latency: LogFilterLatencyUi?,
)

/**
 * One selectable filter value. [id] is the caller's own identifier and travels back untouched in
 * the matching event, so the design system never has to interpret [label].
 */
@Immutable
data class LogFilterOptionUi(
    val id: String,
    val label: String,
    val selected: Boolean,
)

/**
 * The AI-generated control's three choices. [Any] is the inactive one — it is a real selection in
 * the segmented control, not the absence of a selection.
 */
enum class AiGeneratedChoice {
    Any,
    Yes,
    No,
}

/**
 * One end of the UTC date/time range.
 *
 * Each value appears twice by design: once as a formatted label the field displays, and once as the
 * raw value the Material pickers need to open on the current selection. Formatting stays with the
 * caller, which owns the locale and the UTC rules, while the picker still opens where the user left
 * it. A `null` label renders the field's placeholder.
 */
@Immutable
data class LogFilterDateTimeUi(
    val dateLabel: String? = null,
    val timeLabel: String? = null,
    /** UTC midnight of the selected date, in epoch milliseconds, as the date pickers report it. */
    val dateUtcMillis: Long? = null,
    val hourOfDayUtc: Int? = null,
    val minuteOfHourUtc: Int? = null,
)

/**
 * The latency range slider.
 *
 * [bounds] is the stored snapshot's extent and [selection] the draft inside it, so a selection
 * spanning the full extent is what "unconstrained" looks like here — collapsing that back into "no
 * predicate" is the caller's query policy, not the slider's.
 */
@Immutable
data class LogFilterLatencyUi(
    val bounds: ClosedFloatingPointRange<Float>,
    val selection: ClosedFloatingPointRange<Float>,
    val lowerBoundLabel: String,
    val upperBoundLabel: String,
    /** The current selection in words, e.g. `128 – 3,245 ms`; a slider with no readout is guesswork. */
    val selectionLabel: String,
)

/**
 * Everything the filter sheet reports.
 *
 * One event type rather than a dozen callback parameters: the sheet has eight distinct
 * interactions, and a single stream keeps the component's signature readable while matching how the
 * caller already dispatches user input.
 *
 * Nothing here says what the change *means*. [Applied] and [Cleared] are named for the buttons the
 * user pressed, and whether Clear All also commits is the caller's decision.
 */
sealed interface LogFilterSheetEvent {
    /** A tag chip was tapped; [id] is the [LogFilterOptionUi.id] that was passed in. */
    data class TagToggled(
        val id: String,
    ) : LogFilterSheetEvent

    data class SeverityToggled(
        val id: String,
    ) : LogFilterSheetEvent

    data class AiGeneratedSelected(
        val choice: AiGeneratedChoice,
    ) : LogFilterSheetEvent

    /**
     * The date-range picker was confirmed. Both ends are UTC epoch milliseconds at midnight, and
     * either may be `null` — confirming with nothing selected clears the range.
     */
    data class DateRangeSelected(
        val startUtcMillis: Long?,
        val endUtcMillis: Long?,
    ) : LogFilterSheetEvent

    data class StartTimeSelected(
        val hourOfDayUtc: Int,
        val minuteOfHourUtc: Int,
    ) : LogFilterSheetEvent

    data class EndTimeSelected(
        val hourOfDayUtc: Int,
        val minuteOfHourUtc: Int,
    ) : LogFilterSheetEvent

    /** Emitted continuously while a thumb is dragged, in the slider's own `Float` domain. */
    data class LatencyRangeSelected(
        val range: ClosedFloatingPointRange<Float>,
    ) : LogFilterSheetEvent

    data object Applied : LogFilterSheetEvent

    data object Cleared : LogFilterSheetEvent
}
