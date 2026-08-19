package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable

/**
 * One entry of the flattened grouped list rendered by the single `LazyColumn`.
 *
 * Minute headers and log rows share one flat collection instead of nested lists, so the screen can
 * use exactly one lazy list — and so both can travel inside one `PagingData`, which has no notion
 * of nesting. Each item carries its own [stableKey] and a fixed [contentType]: keys survive
 * filtering, re-sorting, and page loads, while the content type lets the list reuse header and row
 * compositions separately.
 *
 * Both cases carry [utcMinuteId] because that is what the Paging separator transformation compares
 * to decide where a header belongs; it is the group's identity, not a displayed value.
 */
@Immutable
internal sealed interface LogViewerListItem {
    val stableKey: String
    val contentType: String

    /** The UTC minute this item belongs to, as `yyyy-MM-ddTHH:mmZ`. */
    val utcMinuteId: String

    /**
     * Static UTC-minute group heading.
     *
     * [minute] is the `HH:mm` the header displays, sliced out of [utcMinuteId] rather than
     * formatted from the timestamp a second time — the label and the list key are then the same
     * value by construction, and cannot drift if one formatter is ever changed.
     */
    @Immutable
    data class MinuteHeader(
        override val utcMinuteId: String,
    ) : LogViewerListItem {
        val minute: String = utcMinuteId.substringAfter(DATE_TIME_SEPARATOR).removeSuffix(UTC_SUFFIX)
        override val stableKey: String = minuteHeaderKey(utcMinuteId)
        override val contentType: String = CONTENT_TYPE

        private companion object {
            const val CONTENT_TYPE: String = "log-viewer-minute-header"
            const val DATE_TIME_SEPARATOR: Char = 'T'
            const val UTC_SUFFIX: String = "Z"
        }
    }

    /**
     * One log entry inside the preceding [MinuteHeader]'s minute.
     *
     * It carries only what the *list* renders. The details sheet's values are deliberately absent:
     * selection resolves them from the repository by log ID, so a row stays selectable after Paging
     * has discarded its page, and a loaded page never holds a second display model per row.
     */
    @Immutable
    data class LogRow(
        override val utcMinuteId: String,
        val row: LogRowUi,
    ) : LogViewerListItem {
        override val stableKey: String = logRowKey(row.id)
        override val contentType: String = CONTENT_TYPE

        private companion object {
            const val CONTENT_TYPE: String = "log-viewer-log-row"
        }
    }
}

/** Namespaced so a minute and a log can never collide on the same key in the flat list. */
internal fun minuteHeaderKey(utcMinuteId: String): String = "minute:$utcMinuteId"

internal fun logRowKey(logId: String): String = "log:$logId"
