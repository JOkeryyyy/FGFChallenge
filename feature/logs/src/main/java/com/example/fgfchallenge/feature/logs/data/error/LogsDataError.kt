package com.example.fgfchallenge.feature.logs.data.error

/**
 * The complete set of failures `LogsRepository` can report. Transport exceptions, HTTP plumbing,
 * and serialization types stop at the data boundary and are classified into these cases before
 * presentation ever sees them.
 */
internal sealed interface LogsDataError : Error {
    /** No usable network path to the host: DNS failure, refused connection, or no route. */
    data object Connectivity : LogsDataError

    /** The request was accepted but did not complete within the configured timeouts. */
    data object Timeout : LogsDataError

    /** A non-2xx response; [statusCode] is retained so tests and diagnostics stay specific. */
    data class Http(
        val statusCode: Int,
    ) : LogsDataError

    /** The response could not be decoded as the declared JSON schema. */
    data object Serialization : LogsDataError

    /** The response decoded, but its values violate the payload's documented semantics. */
    data object Schema : LogsDataError

    /** An unexpected I/O or runtime failure that none of the cases above describe. */
    data object Unknown : LogsDataError
}
