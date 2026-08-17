package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogBatch

/**
 * The logs data layer's only public boundary. Presentation depends on this contract and on the
 * models in `data/model`; it never sees `LogsApi`, DTOs, Retrofit, or OkHttp.
 */
internal interface LogsRepository {
    /**
     * Fetches the complete log payload once.
     *
     * One-shot and `suspend` rather than a `Flow`: the payload is a single bounded response with
     * no refresh policy, so there is no stream of values to observe. Safe to call from the main
     * thread — the implementation moves its own work off it.
     */
    suspend fun getLogs(): Result<LogBatch, LogsDataError>
}
