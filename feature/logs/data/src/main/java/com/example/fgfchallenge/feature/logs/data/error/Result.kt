@file:Suppress("FINAL_UPPER_BOUND")

package com.example.fgfchallenge.feature.logs.data.error

/**
 * The feature's single typed-result convention: a [Result] that carries either data or the
 * feature's [LogsDataError], plus the small set of chaining helpers callers need.
 *
 * This deliberately shadows `kotlin.Result`, which cannot express a typed failure. Every layer in
 * this feature uses this type rather than throwing for expected failures; see
 * `documentation/conventions/data-layer.md` §11.
 * The generic error slot is retained as part of the approved cross-module signature even though
 * this prototype intentionally has one final error value.
 *
 * It stays in `:feature:logs:data` rather than moving to `:core:network`: a result convention is
 * not network infrastructure. It moves to a neutral shared module only if a second feature
 * genuinely needs it.
 */
sealed interface Result<out D, out E : LogsDataError> {
    data class Success<out D>(
        val data: D,
    ) : Result<D, Nothing>

    data class Error<out E : LogsDataError>(
        val error: E,
    ) : Result<Nothing, E>
}

/** A [Result] whose success case carries no payload. */
typealias EmptyResult<E> = Result<Unit, E>

internal inline fun <T, E : LogsDataError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }

internal inline fun <T, E : LogsDataError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            this
        }

        is Result.Success -> {
            action(data)
            this
        }
    }

internal inline fun <T, E : LogsDataError> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            action(error)
            this
        }

        is Result.Success -> {
            this
        }
    }

internal fun <T, E : LogsDataError> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
