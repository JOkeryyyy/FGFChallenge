package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import com.example.fgfchallenge.feature.logs.data.remote.LogsPayloadDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Classification of transport failures that MockWebServer cannot raise deterministically. Each
 * case drives the repository with a fake [LogsApi] that throws one specific exception.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLogsRepositoryFailureTest {
    @Test
    fun `unresolved host is connectivity`() = assertClassified(UnknownHostException("no dns"), LogsDataError.Connectivity)

    @Test
    fun `refused connection is connectivity`() = assertClassified(ConnectException("refused"), LogsDataError.Connectivity)

    @Test
    fun `unroutable host is connectivity`() = assertClassified(NoRouteToHostException("no route"), LogsDataError.Connectivity)

    @Test
    fun `socket timeout is timeout`() = assertClassified(SocketTimeoutException("timeout"), LogsDataError.Timeout)

    @Test
    fun `undecodable payload is serialization`() = assertClassified(SerializationException("bad json"), LogsDataError.Serialization)

    @Test
    fun `unexpected io failure is unknown`() = assertClassified(IOException("broken pipe"), LogsDataError.Unknown)

    @Test
    fun `unexpected runtime failure is unknown`() = assertClassified(IllegalStateException("boom"), LogsDataError.Unknown)

    @Test
    fun `cancellation is rethrown unchanged`() =
        runTest {
            val cancellation = CancellationException("cancelled")
            val repository = NetworkLogsRepository(ThrowingLogsApi(cancellation), UnconfinedTestDispatcher())

            val thrown =
                try {
                    repository.getLogs()
                    null
                } catch (caught: CancellationException) {
                    caught
                }

            // Same instance, not a substitute: converting cancellation into a data error would
            // break structured concurrency for the caller.
            assertThat(thrown).isSameInstanceAs(cancellation)
        }

    private fun assertClassified(
        failure: Throwable,
        expected: LogsDataError,
    ) = runTest {
        val repository = NetworkLogsRepository(ThrowingLogsApi(failure), UnconfinedTestDispatcher())

        assertThat(repository.getLogs()).isEqualTo(Result.Error(expected))
    }
}

private class ThrowingLogsApi(
    private val failure: Throwable,
) : LogsApi {
    override suspend fun getLogs(): Response<LogsPayloadDto> = throw failure
}
