package com.example.fgfchallenge.feature.logs.data.error

import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * The data layer's failure boundary: runs a data-access [block] and collapses every expected
 * infrastructure failure into [LogsDataError], so no exception type from either source escapes the
 * data layer.
 *
 * It lives beside the error types rather than inside the repository because more than one class now
 * reaches infrastructure: each refresh strategy writes, the repository reads, and all of them must
 * collapse failures identically. Duplicating the catch order per class is exactly how a variant
 * would eventually stop rethrowing cancellation.
 *
 * It is `inline`, so [block] may suspend even though this function does not.
 */
internal inline fun <T> guardLogsDataFailures(block: () -> Result<T, LogsDataError>): Result<T, LogsDataError> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        // Must come first and stay `kotlinx.coroutines.CancellationException`: swallowing it here
        // would make a cancelled refresh look handled instead of cancelled. It is also a
        // `RuntimeException`, so the catch below would otherwise absorb it.
        throw cancellation
    } catch (_: IOException) {
        // Connectivity loss and timeouts: `UnknownHostException`, `ConnectException`,
        // `NoRouteToHostException`, `SocketTimeoutException`.
        Result.Error(LogsDataError)
    } catch (_: RuntimeException) {
        // Decoding failures (`SerializationException`), SQLite failures (`SQLiteException` is a
        // `RuntimeException`), and anything else unexpected.
        Result.Error(LogsDataError)
    }
