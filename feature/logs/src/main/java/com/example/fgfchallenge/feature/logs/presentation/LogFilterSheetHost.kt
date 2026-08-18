package com.example.fgfchallenge.feature.logs.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.fgfchallenge.core.designsystem.component.LogFilterSheet
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetEvent
import com.example.fgfchallenge.core.designsystem.model.LogFilterSheetUi
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.presentation.model.AiGeneratedFilter
import com.example.fgfchallenge.feature.logs.presentation.model.LogFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.severityFilterFor
import com.example.fgfchallenge.feature.logs.presentation.model.toFilterSelection
import com.example.fgfchallenge.feature.logs.presentation.model.toFilterSheetUi
import com.example.fgfchallenge.feature.logs.presentation.model.withEvent
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToLong

/**
 * Owns the filter sheet's uncommitted edit, and is the only thing an edit recomposes.
 *
 * The draft used to live in `LogViewerUiState`, which meant every chip tap — and every frame of a
 * latency drag — published a new screen state and invalidated `LogViewerScreen`, its `Scaffold`, and
 * the row `LazyColumn` for a value none of them render. Nothing outside the sheet reads the draft,
 * so it belongs here instead: this host is a sibling of the screen under `LogsFeature`, and an edit
 * now leaves the screen's state instance untouched, so the screen skips.
 *
 * It sits in `feature/logs` rather than inside [LogFilterSheet] because the design system's
 * components stay stateless and free of feature types — the sheet speaks chips, epoch milliseconds,
 * and `Float` positions, while the query speaks `Severity` and `java.time`. This is where the two
 * meet.
 *
 * The edit state is the *sheet's* model rather than a [LogFilterSelection], which is what keeps an
 * edit cheap: every option already carries its own `selected` flag, so a tap rewrites one chip
 * instead of re-deriving all of them and reformatting every label. The expensive mapping runs when
 * the sheet opens, and once more on Apply, on the way back out.
 *
 * Only [onApply] commits. Dismissal reports itself and nothing else, so a swipe-down never becomes
 * an implicit Apply — the applied filters, and the rows the user is reading, are left alone.
 */
@Composable
internal fun LogFilterSheetHost(
    appliedFilters: LogFilterSelection,
    options: LogFilterOptions,
    onApply: (LogFilterSelection) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var draft by rememberSaveable(stateSaver = logFilterSheetUiSaver) {
        mutableStateOf(appliedFilters.toFilterSheetUi(options))
    }

    // The controls are built from a live Room stream, so the snapshot metadata can arrive — or be
    // replaced — after the sheet is already open. Re-deriving from the *current* edit keeps the
    // user's selections and still shows the tags that now exist.
    //
    // The comparison is what keeps this from doing anything on the common path: `LaunchedEffect`
    // runs on first composition too, and without it every sheet opening would immediately re-map the
    // value it had just mapped.
    var mappedAgainst by remember { mutableStateOf(options) }
    LaunchedEffect(options) {
        if (options != mappedAgainst) {
            draft = draft.toFilterSelection().toFilterSheetUi(options)
            mappedAgainst = options
        }
    }

    LogFilterSheet(
        filters = draft,
        onEvent = { event ->
            when (event) {
                LogFilterSheetEvent.Applied -> onApply(draft.toFilterSelection())

                // Clear All resets the edit and leaves the sheet open: it is an edit like any other,
                // and nothing queries the database until Apply.
                LogFilterSheetEvent.Cleared -> draft = LogFilterSelection().toFilterSheetUi(options)

                else -> draft = draft.withEvent(event)
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

/**
 * Persists the edit across configuration change and process death, which the ViewModel-held draft
 * only managed for the former.
 *
 * It stores the selection *and the snapshot metadata it was rendered against*, then restores by
 * re-running the forward mapping — the same work opening the sheet already does. Storing the
 * rendered sheet instead would be storing a cache of derived labels, and capturing the live
 * [LogFilterOptions] in the saver would let a restore run against metadata that has since changed.
 *
 * `null` is a meaningful value in most slots — an open side of the date range, an unset time, a
 * snapshot with no latency extent — so the encoding is positional and every slot is always written.
 * Every element is `Serializable`, which is what a saved-instance-state `Bundle` requires.
 */
private val logFilterSheetUiSaver: Saver<LogFilterSheetUi, Any> =
    listSaver<LogFilterSheetUi, Any?>(
        save = { sheet ->
            val selection = sheet.toFilterSelection()
            val latency = sheet.latency
            listOf(
                ArrayList(selection.tags),
                ArrayList(selection.severities.map { it.name }),
                selection.aiGenerated.name,
                selection.startDateUtc?.toEpochDay(),
                selection.endDateUtc?.toEpochDay(),
                selection.startTimeUtc?.toSecondOfDay(),
                selection.endTimeUtc?.toSecondOfDay(),
                selection.minimumLatencyMs,
                selection.maximumLatencyMs,
                ArrayList(sheet.tags.map { it.id }),
                latency?.bounds?.start?.roundToLong(),
                latency?.bounds?.endInclusive?.roundToLong(),
            )
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            val selection =
                LogFilterSelection(
                    tags = (saved[0] as List<String>).toSet(),
                    severities = (saved[1] as List<String>).mapNotNullTo(mutableSetOf(), ::severityFilterFor),
                    aiGenerated = AiGeneratedFilter.valueOf(saved[2] as String),
                    startDateUtc = (saved[3] as Long?)?.let(LocalDate::ofEpochDay),
                    endDateUtc = (saved[4] as Long?)?.let(LocalDate::ofEpochDay),
                    startTimeUtc = (saved[5] as Int?)?.let { LocalTime.ofSecondOfDay(it.toLong()) },
                    endTimeUtc = (saved[6] as Int?)?.let { LocalTime.ofSecondOfDay(it.toLong()) },
                    minimumLatencyMs = saved[7] as Long?,
                    maximumLatencyMs = saved[8] as Long?,
                )

            @Suppress("UNCHECKED_CAST")
            val restoredOptions =
                LogFilterOptions(
                    availableTags = saved[9] as List<String>,
                    minimumLatencyMs = saved[10] as Long?,
                    maximumLatencyMs = saved[11] as Long?,
                )
            selection.toFilterSheetUi(restoredOptions)
        },
    )
