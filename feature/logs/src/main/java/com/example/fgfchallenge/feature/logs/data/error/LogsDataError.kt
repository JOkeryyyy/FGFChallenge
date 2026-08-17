package com.example.fgfchallenge.feature.logs.data.error

/**
 * The single failure `LogsRepository` reports. Transport exceptions, HTTP plumbing, and
 * serialization types stop at the data boundary and are collapsed into this value before
 * presentation ever sees them.
 *
 * There is deliberately one case instead of a connectivity/timeout/HTTP/serialization/schema
 * taxonomy: the application's answer to a failed load is the same retryable error state whatever
 * caused it, so classifying further would be a distinction nothing acts on. Add a case only when
 * a caller genuinely behaves differently for it.
 */
internal data object LogsDataError : Error
