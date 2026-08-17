package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi

/**
 * One entry of the flattened grouped list rendered by the single `LazyColumn`.
 *
 * Minute headers and log rows share one flat collection instead of nested lists, so the screen can
 * use exactly one lazy list. Each item carries its own [stableKey] and a fixed [contentType]:
 * keys survive filtering and re-sorting, while the content type lets the list reuse header and row
 * compositions separately.
 */
@Immutable
internal sealed interface LogViewerListItem {
    val stableKey: String
    val contentType: String

    /** Static UTC-minute group heading; [itemCount] is the number of rows that follow it. */
    @Immutable
    data class MinuteHeader(
        override val stableKey: String,
        val minute: String,
        val itemCount: Int,
    ) : LogViewerListItem {
        override val contentType: String = CONTENT_TYPE

        private companion object {
            const val CONTENT_TYPE: String = "log-viewer-minute-header"
        }
    }

    /**
     * One log entry inside the preceding [MinuteHeader]'s minute.
     *
     * [details] is the same entry rendered for the details sheet. Carrying it on the item makes
     * selection a lookup in the already-materialized list instead of a second data source, so
     * `details.logId` always equals `row.id`.
     */
    @Immutable
    data class LogRow(
        override val stableKey: String,
        val row: LogRowUi,
        val details: LogDetailsUi,
    ) : LogViewerListItem {
        override val contentType: String = CONTENT_TYPE

        private companion object {
            const val CONTENT_TYPE: String = "log-viewer-log-row"
        }
    }
}

/** Namespaced so a minute and a log can never collide on the same key in the flat list. */
internal fun minuteHeaderKey(utcMinuteId: String): String = "minute:$utcMinuteId"

internal fun logRowKey(logId: String): String = "log:$logId"
