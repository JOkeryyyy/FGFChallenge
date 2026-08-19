package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.error.EmptyResult
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError

/**
 * How a build variant prepares the Room snapshot before the viewer reads it: the write half of
 * `SnapshotLogsRepository`, isolated so it can be selected per source set.
 *
 * This is a variant seam, not a domain layer or a data source. It exists because there are two real
 * implementations — the shipping remote refresh, and the benchmark variant's fixed 100,000-row
 * fixture — and nothing outside `data/repository` consumes it. The read path is identical in every
 * variant, so `refresh` is the only method here.
 *
 * A failure is reported, never thrown: cancellation stays transparent, and every other
 * infrastructure failure arrives as [LogsDataError] via `guardLogsDataFailures`.
 */
internal fun interface SnapshotRefresher {
    suspend fun refresh(): EmptyResult<LogsDataError>
}
