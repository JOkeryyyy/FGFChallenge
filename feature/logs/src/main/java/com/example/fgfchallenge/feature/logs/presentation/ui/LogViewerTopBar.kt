package com.example.fgfchallenge.feature.logs.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.fgfchallenge.core.designsystem.component.LogSearchField
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing
import com.example.fgfchallenge.feature.logs.R
import com.example.fgfchallenge.feature.logs.presentation.model.LogSortOrder
import java.text.NumberFormat
import java.util.Locale

/**
 * The screen's one persistent control surface: what the query currently matches, and the three
 * controls that change it.
 *
 * A pinned app bar rather than a title above a row of buttons. Filter, search, and sort are the only
 * screen-level commands the viewer has, and as icons they occupy one bar height instead of three
 * stacked rows — which is height the list gets back, on a screen whose whole purpose is scanning
 * rows. Each control keeps a text label for a screen reader, since an icon carries none.
 *
 * The title is the two counts rather than a fixed product name: the numbers that describe the query
 * are worth more in the most prominent position than a name that never changes, and stating them
 * together — loaded of matching — is what keeps a paged list from reading as the whole result.
 *
 * The search *field* is the one control that is not an icon, because it takes typing. It expands
 * under the bar rather than replacing the title, so the count that says how the search is going
 * stays visible while it is being typed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogViewerTopBar(
    summary: LogViewerSummaryState,
    loadedRowCount: Int,
    activeFilterCount: Int,
    sortOrder: LogSortOrder,
    query: String,
    isSearchExpanded: Boolean,
    onAction: (LogViewerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One surface behind both rows, so the expanded field reads as part of the bar rather than as
    // the first item of the content below it.
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        text = logCountTitle(summary = summary, loadedRowCount = loadedRowCount),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag(RESULT_TEST_TAG),
                    )
                },
                navigationIcon = {
                    FilterAction(
                        activeFilterCount = activeFilterCount,
                        onClick = { onAction(LogViewerAction.FilterSheetOpened) },
                    )
                },
                actions = {
                    SearchAction(
                        isExpanded = isSearchExpanded,
                        hasQuery = query.isNotBlank(),
                        onClick = {
                            onAction(
                                if (isSearchExpanded) {
                                    LogViewerAction.SearchDismissed
                                } else {
                                    LogViewerAction.SearchOpened
                                },
                            )
                        },
                    )
                    SortAction(
                        sortOrder = sortOrder,
                        onClick = { onAction(LogViewerAction.SortOrderToggled) },
                    )
                },
            )
            if (isSearchExpanded) {
                ExpandedSearchField(
                    query = query,
                    onQueryChange = { onAction(LogViewerAction.QueryChanged(it)) },
                )
            }
        }
    }
}

/**
 * How many rows Paging holds of how many the query matches, as the bar's title.
 *
 * Both numbers or neither. The two are routinely orders of magnitude apart, so the loaded figure
 * alone would understate the result and the total alone would leave no sign that the list is paged —
 * and a loaded count paired with a total counted for *previous* criteria would be worse than either.
 * That last case is what [LogViewerSummaryState.Pending] exists to prevent, so an uncounted query
 * says so rather than showing a number for half the pair.
 */
@Composable
private fun logCountTitle(
    summary: LogViewerSummaryState,
    loadedRowCount: Int,
): String =
    when (summary) {
        LogViewerSummaryState.Pending -> {
            stringResource(R.string.log_viewer_title_counting)
        }

        is LogViewerSummaryState.Ready -> {
            stringResource(
                R.string.log_viewer_title_count,
                groupedCount(loadedRowCount),
                groupedCount(summary.summary.totalLogCount),
            )
        }
    }

/**
 * Opens the filter sheet, and states how many filter categories are currently narrowing the result.
 *
 * The count is a badge rather than only a highlight because the sheet's contents are out of sight
 * once it closes: without it, a result narrowed by an applied filter is indistinguishable from an
 * unfiltered one. Zero renders as no badge at all.
 *
 * The badge's number replaces the icon's own label rather than sitting beside it, because a screen
 * reader announcing "Filter" then "3" leaves the relationship between them to be guessed.
 */
@Composable
private fun FilterAction(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    val label =
        if (activeFilterCount > 0) {
            pluralStringResource(
                R.plurals.log_viewer_filter_action_active,
                activeFilterCount,
                activeFilterCount,
            )
        } else {
            stringResource(R.string.log_viewer_filter_action)
        }
    IconButton(onClick = onClick, modifier = Modifier.testTag(FILTER_TEST_TAG)) {
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(text = activeFilterCount.toString()) }
                }
            },
        ) {
            Icon(imageVector = Icons.Default.FilterList, contentDescription = label)
        }
    }
}

/**
 * Shows or hides the search field, and says whether a search is in effect while it is hidden.
 *
 * The indicator is the point of collapsing the field at all: the text outlives the control, so
 * without it a result narrowed by a search the user typed a minute ago would look unfiltered. It is
 * a dot rather than a count because a search is one condition, and it is announced in the label
 * rather than left as a decoration.
 */
@Composable
private fun SearchAction(
    isExpanded: Boolean,
    hasQuery: Boolean,
    onClick: () -> Unit,
) {
    val label =
        stringResource(
            when {
                isExpanded -> R.string.log_viewer_search_close_action
                hasQuery -> R.string.log_viewer_search_active_action
                else -> R.string.log_viewer_search_action
            },
        )
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (hasQuery && !isExpanded) {
                    Badge()
                }
            },
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Search,
                contentDescription = label,
            )
        }
    }
}

/**
 * Tap-to-toggle between the two orders, as one icon.
 *
 * The icon is the same in both directions because it names the control, not the direction; the
 * direction lives in the label, which states the order in effect *and* the one a tap produces —
 * without both, a toggle whose only visible state is an icon is unusable without sight.
 */
@Composable
private fun SortAction(
    sortOrder: LogSortOrder,
    onClick: () -> Unit,
) {
    val label =
        stringResource(
            when (sortOrder) {
                LogSortOrder.NewestFirst -> R.string.log_viewer_sort_action_newest_first
                LogSortOrder.OldestFirst -> R.string.log_viewer_sort_action_oldest_first
            },
        )
    IconButton(onClick = onClick) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = label)
    }
}

/**
 * The search field, revealed under the bar and focused as it appears.
 *
 * Autofocus is what makes a hidden field worth hiding: the tap that opened it is the same intent as
 * the tap that would have selected it, so requiring a second one would make the compact bar cost the
 * user an interaction. The request runs in an effect rather than during composition because the node
 * has to exist before it can take focus.
 */
@Composable
private fun ExpandedSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // Skipped for a preview or a golden: those are still renders of the screen, and taking focus
    // there opens a text-input session — which is an interaction they cannot have, and whose IME
    // machinery a host-side render has no thread to run.
    val isInspecting = LocalInspectionMode.current
    LaunchedEffect(isInspecting) {
        if (!isInspecting) {
            focusRequester.requestFocus()
        }
    }
    // Centered and width-limited like the content below, while the bar itself spans the window.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LogSearchField(
            query = query,
            onQueryChange = onQueryChange,
            enabled = true,
            modifier =
                Modifier
                    .testTag(SEARCH_TEST_TAG)
                    .widthIn(max = Dimens.contentMaxWidth)
                    .padding(horizontal = Dimens.screenHorizontalPadding)
                    .padding(bottom = Spacing.xs)
                    .focusRequester(focusRequester),
        )
    }
}

// Three of the four anchors the out-of-process Macrobenchmark suite drives; the list's own tag
// lives in `LogViewerContent.kt`. They are resource-style ids rather than accessibility labels
// because UI Automator has to find them by a value that does not change with the query, the
// locale, or the count in the title.
// See `documentation/performanceBenchmark.md`.
private const val SEARCH_TEST_TAG = "log_viewer_search"
private const val FILTER_TEST_TAG = "log_viewer_filter"
private const val RESULT_TEST_TAG = "log_viewer_result"

/**
 * Held rather than rebuilt per call: the title formats two counts on every recomposition, and the
 * bar recomposes on every paged update because it reads the loaded-row count. `getIntegerInstance`
 * clones a cached prototype, so calling it there allocated a `DecimalFormat` per number rendered.
 *
 * `NumberFormat` is not thread-safe; this one is only ever touched from composition, on the main
 * thread. The equivalent formatters in `LogEntryUiMapper` are held the same way.
 */
private val COUNT_FORMAT: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

/**
 * Groups a count before it is inserted into a resource, so the format string stays a plain `%s`.
 * The locale is fixed because the prototype ships English-only copy and the visual goldens must
 * render the same text on every machine.
 */
private fun groupedCount(count: Int): String = COUNT_FORMAT.format(count)
