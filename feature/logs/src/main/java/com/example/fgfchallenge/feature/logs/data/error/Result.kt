package com.example.fgfchallenge.feature.logs.data.error

/**
 * The feature's single typed-result convention: a [Result] that carries either data or a typed
 * [Error], plus the small set of chaining helpers callers need.
 *
 * This deliberately shadows `kotlin.Result`, which cannot express a typed failure. Every layer in
 * this feature uses this type rather than throwing for expected failures; see
 * `documentation/conventions/data-layer.md` §11.
 */
internal interface Error

internal sealed interface Result<out D, out E : Error> {
    data class Success<out D>(
        val data: D,
    ) : Result<D, Nothing>

    data class Error<out E : com.example.fgfchallenge.feature.logs.data.error.Error>(
        val error: E,
    ) : Result<Nothing, E>
}

/** A [Result] whose success case carries no payload. */
internal typealias EmptyResult<E> = Result<Unit, E>

internal inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }

internal inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            this
        }

        is Result.Success -> {
            action(data)
            this
        }
    }

internal inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            action(error)
            this
        }

        is Result.Success -> {
            this
        }
    }

internal fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
