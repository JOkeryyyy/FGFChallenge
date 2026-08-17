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
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * Network-backed [LogsRepository]. Named for its data strategy, so it stays accurate if the HTTP
 * client changes.
 *
 * It wraps [LogsApi] directly: there is one source and no planned second one, so a separate
 * `LogsRemoteDataSource` would add a layer without adding a boundary
 * (`documentation/conventions/data-layer.md` §6).
 *
 * This class is where transport failures stop. Everything it can throw is classified into
 * [LogsDataError], except coroutine cancellation, which is always rethrown untouched so
 * structured concurrency keeps working.
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
                when {
                    !response.isSuccessful -> {
                        Result.Error(LogsDataError.Http(response.code()))
                    }

                    else -> {
                        when (val body = response.body()) {
                            // A 2xx with no body is well-formed HTTP but an unusable payload.
                            null -> Result.Error(LogsDataError.Schema)

                            // Validating and mapping ~5,000 entries is CPU work, so it moves off
                            // the caller's thread. The Retrofit call itself already suspends.
                            else -> withContext(defaultDispatcher) { body.toLogBatch() }
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                // Must come first and stay `kotlinx.coroutines.CancellationException`: swallowing
                // it here would make a cancelled load look handled instead of cancelled.
                throw cancellation
            } catch (_: SocketTimeoutException) {
                Result.Error(LogsDataError.Timeout)
            } catch (_: UnknownHostException) {
                Result.Error(LogsDataError.Connectivity)
            } catch (_: ConnectException) {
                Result.Error(LogsDataError.Connectivity)
            } catch (_: NoRouteToHostException) {
                Result.Error(LogsDataError.Connectivity)
            } catch (_: SerializationException) {
                Result.Error(LogsDataError.Serialization)
            } catch (_: IOException) {
                Result.Error(LogsDataError.Unknown)
            } catch (_: RuntimeException) {
                Result.Error(LogsDataError.Unknown)
            }
    }
