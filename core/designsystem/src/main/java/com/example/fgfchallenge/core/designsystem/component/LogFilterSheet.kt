package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.model.AiGeneratedChoice
import com.example.fgfchallenge.core.designsystem.model.LogFilterDateTimeUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterLatencyUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterOptionUi
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetEvent
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetUi
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/** The date-range picker's month list is unbounded; without a cap it fills the whole dialog window. */
private val dateRangePickerMaxHeight = 500.dp

/**
 * The structured filter sheet: tag and severity chips, the AI-generated tri-state, the UTC
 * date/time range, and the inclusive latency range, above Clear All and Apply.
 *
 * It owns none of the filter state. [filters] is the caller's draft, rendered as given, and every
 * interaction leaves as a [LogFilterSheetEvent] — including each drag of the latency slider, so the
 * value the sheet shows is always the value the caller holds rather than a second copy that could
 * drift from it. Which events commit anything is likewise the caller's decision: this component
 * does not know that Apply ends the edit.
 *
 * The sheet has exactly one settled state: expanded. Five filter categories plus the action row do
 * not fit in a partially expanded sheet, so opening at half height would show a fragment of the
 * controls and make a second drag a precondition for using them. `skipPartiallyExpanded` removes
 * that intermediate state entirely — the sheet opens expanded, and the only other place it can
 * settle is dismissed.
 *
 * Close, swipe-down, and Back dismissal all come from Material 3's own [ModalBottomSheet] behavior
 * and reach [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFilterSheet(
    filters: LogFilterSheetUi,
    onEvent: (LogFilterSheetEvent) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LogFilterSheetContent(filters = filters, onEvent = onEvent)
    }
}

/**
 * The sheet's content, split out from [LogFilterSheet] for the same reason `LogDetailsSheet`'s is:
 * [ModalBottomSheet] hosts its content in a separate window whose entrance animation never settles
 * in a static preview render, leaving the sheet itself blank.
 */
@Composable
private fun LogFilterSheetContent(
    filters: LogFilterSheetUi,
    onEvent: (LogFilterSheetEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The only state this component keeps: which picker dialog is open. It is transient view state
    // with no meaning outside the sheet — a configuration change may reasonably close a half-open
    // picker — as opposed to the draft values, which every event hands straight to the caller.
    var openPicker by remember { mutableStateOf(FilterPicker.None) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                // Five sections plus the action row exceed the sheet's height on a phone, and more
                // so at a large font scale. ModalBottomSheet nested-scrolls with a scrollable
                // child, so drag-to-dismiss still works.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.log_filter_sheet_title),
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider()

        FilterSection(title = stringResource(R.string.log_filter_section_tags)) {
            if (filters.tags.isEmpty()) {
                UnavailableText(stringResource(R.string.log_filter_tags_unavailable))
            } else {
                FilterChipRow(
                    options = filters.tags,
                    onToggle = { id -> onEvent(LogFilterSheetEvent.TagToggled(id)) },
                )
            }
        }

        FilterSection(title = stringResource(R.string.log_filter_section_severity)) {
            FilterChipRow(
                options = filters.severities,
                onToggle = { id -> onEvent(LogFilterSheetEvent.SeverityToggled(id)) },
            )
        }

        FilterSection(title = stringResource(R.string.log_filter_section_ai_generated)) {
            AiGeneratedControl(
                selected = filters.aiGenerated,
                onSelect = { choice -> onEvent(LogFilterSheetEvent.AiGeneratedSelected(choice)) },
            )
        }

        FilterSection(title = stringResource(R.string.log_filter_section_date_time)) {
            DateTimeRow(
                boundLabel = stringResource(R.string.log_filter_start_label),
                value = filters.start,
                timeAction = stringResource(R.string.log_filter_select_start_time_action),
                onDateClick = { openPicker = FilterPicker.DateRange },
                onTimeClick = { openPicker = FilterPicker.StartTime },
            )
            DateTimeRow(
                boundLabel = stringResource(R.string.log_filter_end_label),
                value = filters.end,
                timeAction = stringResource(R.string.log_filter_select_end_time_action),
                onDateClick = { openPicker = FilterPicker.DateRange },
                onTimeClick = { openPicker = FilterPicker.EndTime },
            )
        }

        FilterSection(title = stringResource(R.string.log_filter_section_latency)) {
            val latency = filters.latency
            if (latency == null) {
                UnavailableText(stringResource(R.string.log_filter_latency_unavailable))
            } else {
                LatencyControl(
                    latency = latency,
                    onRangeChange = { range -> onEvent(LogFilterSheetEvent.LatencyRangeSelected(range)) },
                )
            }
        }

        HorizontalDivider()
        FilterActions(
            onClearAll = { onEvent(LogFilterSheetEvent.Cleared) },
            onApply = { onEvent(LogFilterSheetEvent.Applied) },
        )
    }

    when (openPicker) {
        FilterPicker.None -> {
            // No picker open: the sheet's own controls are the whole surface.
        }

        FilterPicker.DateRange -> {
            DateRangePickerDialog(
                start = filters.start.dateUtcMillis,
                end = filters.end.dateUtcMillis,
                onConfirm = { startMillis, endMillis ->
                    openPicker = FilterPicker.None
                    onEvent(LogFilterSheetEvent.DateRangeSelected(startMillis, endMillis))
                },
                onDismiss = { openPicker = FilterPicker.None },
            )
        }

        FilterPicker.StartTime -> {
            FilterTimePickerDialog(
                title = stringResource(R.string.log_filter_select_start_time_action),
                value = filters.start,
                onConfirm = { hour, minute ->
                    openPicker = FilterPicker.None
                    onEvent(LogFilterSheetEvent.StartTimeSelected(hour, minute))
                },
                onDismiss = { openPicker = FilterPicker.None },
            )
        }

        FilterPicker.EndTime -> {
            FilterTimePickerDialog(
                title = stringResource(R.string.log_filter_select_end_time_action),
                value = filters.end,
                onConfirm = { hour, minute ->
                    openPicker = FilterPicker.None
                    onEvent(LogFilterSheetEvent.EndTimeSelected(hour, minute))
                },
                onDismiss = { openPicker = FilterPicker.None },
            )
        }
    }
}

/** Which modal picker the sheet currently has open. */
private enum class FilterPicker {
    None,
    DateRange,
    StartTime,
    EndTime,
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Multi-select chips: within a category the selections are alternatives, so several may be on. */
@Composable
private fun FilterChipRow(
    options: List<LogFilterOptionUi>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.selected,
                onClick = { onToggle(option.id) },
                label = {
                    Text(text = option.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

/** Single-select: the three choices are exclusive, and `Any` is one of them rather than "none". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiGeneratedControl(
    selected: AiGeneratedChoice,
    onSelect: (AiGeneratedChoice) -> Unit,
) {
    val choices = AiGeneratedChoice.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = choice == selected,
                onClick = { onSelect(choice) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
            ) {
                Text(text = stringResource(choice.labelRes()))
            }
        }
    }
}

private fun AiGeneratedChoice.labelRes(): Int =
    when (this) {
        AiGeneratedChoice.Any -> R.string.log_filter_ai_any
        AiGeneratedChoice.Yes -> R.string.log_filter_ai_yes
        AiGeneratedChoice.No -> R.string.log_filter_ai_no
    }

/**
 * One end of the range: its date opens the shared range picker, its time opens that end's clock.
 * Both read as placeholders until chosen, because an unset bound leaves that side of the range open
 * rather than defaulting to today.
 */
@Composable
private fun DateTimeRow(
    boundLabel: String,
    value: LogFilterDateTimeUi,
    timeAction: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            text = boundLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onDateClick, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = stringResource(R.string.log_filter_select_date_range_action),
                    modifier = Modifier.padding(end = Spacing.xxs),
                )
                Text(
                    text = value.dateLabel ?: stringResource(R.string.log_filter_date_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = onTimeClick,
                modifier = Modifier.semantics { contentDescription = timeAction },
            ) {
                Text(
                    text = value.timeLabel ?: stringResource(R.string.log_filter_time_placeholder),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * The extent's ends bracket the slider and the current selection is spelled out above it, so the
 * chosen bounds are readable rather than inferred from two thumb positions.
 */
@Composable
private fun LatencyControl(
    latency: LogFilterLatencyUi,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Text(
            text = latency.selectionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RangeSlider(
            value = latency.selection,
            onValueChange = onRangeChange,
            valueRange = latency.bounds,
            // The thumbs already carry the selection, which the label above states in full; the
            // raw bound values would otherwise be read out twice.
            modifier = Modifier.clearAndSetSemantics { contentDescription = latency.selectionLabel },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BoundLabel(latency.lowerBoundLabel)
            BoundLabel(latency.upperBoundLabel)
        }
    }
}

@Composable
private fun BoundLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Apply is the filled action because it is the one that changes what the list shows. */
@Composable
private fun FilterActions(
    onClearAll: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(onClick = onClearAll, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.log_filter_clear_all_action))
        }
        Button(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.log_filter_apply_action))
        }
    }
}

/**
 * Both ends are picked together, which is why either field opens this one dialog: a range picked as
 * two independent dates can be reversed, and the Material range picker cannot produce that.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    start: Long?,
    end: Long?,
    onConfirm: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = start,
            initialSelectedEndDateMillis = end,
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.selectedStartDateMillis, state.selectedEndDateMillis) },
            ) {
                Text(stringResource(R.string.log_filter_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.log_filter_dialog_cancel))
            }
        },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.heightIn(max = dateRangePickerMaxHeight),
            showModeToggle = false,
        )
    }
}

/** 24-hour, because every value the viewer shows is UTC and `HH:mm` is what the headers use. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterTimePickerDialog(
    title: String,
    value: LogFilterDateTimeUi,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = value.hourOfDayUtc ?: 0,
            initialMinute = value.minuteOfHourUtc ?: 0,
            is24Hour = true,
        )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.labelMedium) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.log_filter_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.log_filter_dialog_cancel))
            }
        },
    ) {
        TimePicker(state = state)
    }
}

@Composable
private fun UnavailableText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private val previewFilters =
    LogFilterSheetUi(
        tags =
            listOf(
                LogFilterOptionUi(id = "network", label = "network", selected = true),
                LogFilterOptionUi(id = "auth", label = "auth", selected = true),
                LogFilterOptionUi(id = "cache", label = "cache", selected = false),
                LogFilterOptionUi(id = "database", label = "database", selected = false),
            ),
        severities =
            listOf("DEBUG", "INFO", "WARN", "ERROR", "FATAL").map { label ->
                LogFilterOptionUi(id = label, label = label, selected = label == "ERROR")
            },
        aiGenerated = AiGeneratedChoice.Any,
        start =
            LogFilterDateTimeUi(
                dateLabel = "2025-05-22",
                timeLabel = "17:09",
                dateUtcMillis = 1_747_872_000_000,
                hourOfDayUtc = 17,
                minuteOfHourUtc = 9,
            ),
        end = LogFilterDateTimeUi(),
        latency =
            LogFilterLatencyUi(
                bounds = 0f..10_000f,
                selection = 250f..7_500f,
                lowerBoundLabel = "0",
                upperBoundLabel = "10,000",
                selectionLabel = "250 – 7,500 ms",
            ),
    )

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL, heightDp = 900)
@Composable
private fun LogFilterSheetPreview() {
    FGFChallengeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            LogFilterSheetContent(filters = previewFilters, onEvent = {})
        }
    }
}

@Preview(name = "320 narrow", widthDp = 320, heightDp = 900, showBackground = true)
@Composable
private fun LogFilterSheetNarrowPreview() {
    FGFChallengeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            LogFilterSheetContent(filters = previewFilters, onEvent = {})
        }
    }
}

@Preview(name = "empty snapshot", showBackground = true, heightDp = 900)
@Composable
private fun LogFilterSheetEmptyPreview() {
    FGFChallengeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            LogFilterSheetContent(
                filters = previewFilters.copy(tags = emptyList(), latency = null),
                onEvent = {},
            )
        }
    }
}
