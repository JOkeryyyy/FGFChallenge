package com.example.fgfchallenge.feature.logs.data.error

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

/** Covers the feature's typed-result helpers: they must transform data and pass failures through. */
class ResultTest {
    private val success: Result<Int, LogsDataError> = Result.Success(2)
    private val failure: Result<Int, LogsDataError> = Result.Error(LogsDataError)

    @Test
    fun `error type is bounded directly to LogsDataError`() {
        val errorTypeParameter = Result::class.java.typeParameters.single { it.name == "E" }

        assertThat(errorTypeParameter.bounds.toList())
            .isEqualTo(listOf(LogsDataError::class.java))
    }

    @Test
    fun `map transforms success data`() {
        assertThat(success.map { it * 3 }).isEqualTo(Result.Success(6))
    }

    @Test
    fun `map leaves a failure untouched`() {
        assertThat(failure.map { it * 3 }).isEqualTo(Result.Error(LogsDataError))
    }

    @Test
    fun `onSuccess runs only for success`() {
        var seen: Int? = null
        success.onSuccess { seen = it }
        assertThat(seen).isEqualTo(2)

        var unseen: Int? = null
        failure.onSuccess { unseen = it }
        assertThat(unseen).isNull()
    }

    @Test
    fun `onFailure runs only for failure`() {
        var seen: LogsDataError? = null
        failure.onFailure { seen = it }
        assertThat(seen).isEqualTo(LogsDataError)

        var unseen: LogsDataError? = null
        success.onFailure { unseen = it }
        assertThat(unseen).isNull()
    }

    @Test
    fun `asEmptyResult discards data but keeps the outcome`() {
        assertThat(success.asEmptyResult()).isEqualTo(Result.Success(Unit))
        assertThat(failure.asEmptyResult()).isEqualTo(Result.Error(LogsDataError))
    }
}
