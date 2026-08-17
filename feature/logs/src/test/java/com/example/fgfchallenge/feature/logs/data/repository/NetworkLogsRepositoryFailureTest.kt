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
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Transport failures that MockWebServer cannot raise deterministically. Each case drives the
 * repository with a fake [LogsApi] that throws one specific exception, and asserts that it stops
 * at the data boundary — except cancellation, which must not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLogsRepositoryFailureTest {
    @Test
    fun `unresolved host is a data failure`() = assertContained(UnknownHostException("no dns"))

    @Test
    fun `socket timeout is a data failure`() = assertContained(SocketTimeoutException("timeout"))

    @Test
    fun `unexpected io failure is a data failure`() = assertContained(IOException("broken pipe"))

    @Test
    fun `undecodable payload is a data failure`() = assertContained(SerializationException("bad json"))

    @Test
    fun `unexpected runtime failure is a data failure`() = assertContained(IllegalStateException("boom"))

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

    private fun assertContained(failure: Throwable) =
        runTest {
            val repository = NetworkLogsRepository(ThrowingLogsApi(failure), UnconfinedTestDispatcher())

            assertThat(repository.getLogs()).isEqualTo(Result.Error(LogsDataError))
        }
}

private class ThrowingLogsApi(
    private val failure: Throwable,
) : LogsApi {
    override suspend fun getLogs(): Response<LogsPayloadDto> = throw failure
}
