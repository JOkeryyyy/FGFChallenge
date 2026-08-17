package com.example.fgfchallenge.feature.logs.data.repository

import androidx.paging.PagingData
import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogFilterOptions
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.LogSummary
import kotlinx.coroutines.flow.Flow

/**
 * The logs data layer's only public boundary. Presentation depends on this contract and on the
 * models in `data/model`; it never sees `LogsApi`, DTOs, Retrofit, OkHttp, Room, the DAO, entities,
 * or SQL.
 *
 * The operations are split by lifecycle rather than by subject: one suspending import that mutates
 * the snapshot, three observable reads over it, and one focused lookup. Every read is served from
 * Room, so a successful refresh invalidates all of them at once and no in-memory cache can compete
 * with the database.
 */
internal interface LogsRepository {
    /**
     * Fetches the complete remote snapshot and atomically replaces the stored one.
     *
     * One-shot and `suspend` because it has no refresh policy to observe: the app runs it once per
     * launch, and again only when the user retries. Any failure — network, decoding, mapping, or
     * database — leaves the previously stored snapshot untouched and is reported as the single
     * retryable [LogsDataError]. Safe to call from the main thread.
     */
    suspend fun refreshSnapshot(): EmptyResult<LogsDataError>

    /**
     * The paged rows matching [query], newest first by default.
     *
     * Paging owns the working set: only the loaded pages are ever materialized, whatever the
     * snapshot or the match count. The stream is re-emitted when the snapshot is replaced.
     */
    fun pagedLogs(query: LogQuery): Flow<PagingData<LogEntry>>

    /**
     * Aggregates over the *complete* result of [query], built from the same predicate as
     * [pagedLogs] so the counts cannot describe a different set of rows than the list shows.
     */
    fun summary(query: LogQuery): Flow<LogSummary>

    /** Unfiltered snapshot metadata for the filter controls: available tags and latency extent. */
    fun filterOptions(): Flow<LogFilterOptions>

    /**
     * Resolves one log by its stable ID, so details stay correct regardless of which pages Paging
     * currently holds. A `null` payload is a successful lookup that matched nothing — an ID that is
     * no longer in the snapshot — as opposed to a failed read.
     */
    suspend fun logById(id: String): Result<LogEntry?, LogsDataError>
}
