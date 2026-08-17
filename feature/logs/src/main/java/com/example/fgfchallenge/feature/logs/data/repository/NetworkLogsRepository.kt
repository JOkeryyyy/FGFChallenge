package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.di.DefaultDispatcher
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.mapper.toLogBatch
import com.example.fgfchallenge.feature.logs.data.model.LogBatch
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Network-backed [LogsRepository]. Named for its data strategy, so it stays accurate if the HTTP
 * client changes.
 *
 * It wraps [LogsApi] directly: there is one source and no planned second one, so a separate
 * `LogsRemoteDataSource` would add a layer without adding a boundary
 * (`documentation/conventions/data-layer.md` §6).
 *
 * This class is where transport failures stop. Everything it can throw becomes [LogsDataError],
 * except coroutine cancellation, which is always rethrown untouched so structured concurrency
 * keeps working.
 */
internal class NetworkLogsRepository
    @Inject
    constructor(
        private val logsApi: LogsApi,
        @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : LogsRepository {
        override suspend fun getLogs(): Result<LogBatch, LogsDataError> =
            try {
                val response = logsApi.getLogs()
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    // A non-2xx status, or a 2xx with no body: well-formed HTTP, unusable payload.
                    Result.Error(LogsDataError)
                } else {
                    // Validating and mapping ~5,000 entries is CPU work, so it moves off the
                    // caller's thread. The Retrofit call itself already suspends.
                    withContext(defaultDispatcher) { body.toLogBatch() }
                }
            } catch (cancellation: CancellationException) {
                // Must come first and stay `kotlinx.coroutines.CancellationException`: swallowing
                // it here would make a cancelled load look handled instead of cancelled. It is
                // also a `RuntimeException`, so the catch below would otherwise absorb it.
                throw cancellation
            } catch (_: IOException) {
                // Connectivity loss and timeouts: `UnknownHostException`, `ConnectException`,
                // `NoRouteToHostException`, `SocketTimeoutException`.
                Result.Error(LogsDataError)
            } catch (_: RuntimeException) {
                // Decoding failures (`SerializationException`) and anything else unexpected.
                Result.Error(LogsDataError)
            }
    }
