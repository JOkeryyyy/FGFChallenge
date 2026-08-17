package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.model.LogBatch
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.data.remote.LOGS_ENDPOINT
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Exercises the real Retrofit + Kotlinx Serialization + repository path against MockWebServer:
 * the request that is issued, payload mapping, and which responses are rejected.
 *
 * Every rejection produces the same [LogsDataError], so each failing case is here to pin down the
 * response the repository refuses, not the value it returns. Failures that only the transport can
 * raise are covered by [NetworkLogsRepositoryFailureTest] with a fake `LogsApi`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLogsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var logsApi: LogsApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        logsApi =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(JSON.asConverterFactory("application/json".toMediaType()))
                .build()
                .create()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `getLogs issues a single GET to the supplied firebase route`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            repository().getLogs()

            assertThat(server.requestCount).isEqualTo(1)
            val recorded = server.takeRequest()
            assertThat(recorded.method).isEqualTo("GET")
            assertThat(recorded.target).isEqualTo("/$LOGS_ENDPOINT")
        }

    @Test
    fun `getLogs maps a valid payload to the application model`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            val result = repository().getLogs()

            assertThat(result).isEqualTo(Result.Success(EXPECTED_BATCH))
        }

    @Test
    fun `getLogs ignores unknown json keys`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = PAYLOAD_WITH_UNKNOWN_KEYS))

            val result = repository().getLogs()

            assertThat(result).isEqualTo(Result.Success(EXPECTED_BATCH))
        }

    @Test
    fun `getLogs succeeds with the actual entries when total_count disagrees`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"total_count\": 2", "\"total_count\": 5000")))

            val result = repository().getLogs()

            val batch = (result as Result.Success).data
            assertThat(batch.reportedTotalCount).isEqualTo(5000)
            assertThat(batch.entries.size).isEqualTo(2)
        }

    @Test
    fun `getLogs maps an unrecognized severity to UNKNOWN`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"ERROR\"", "\"TRACE\"")))

            val result = repository().getLogs()

            assertThat(
                (result as Result.Success)
                    .data.entries
                    .first()
                    .severity,
            ).isEqualTo(Severity.UNKNOWN)
        }

    @Test
    fun `getLogs fails on an unsuccessful response`() =
        runTest {
            server.enqueue(MockResponse(code = 404, body = "not found"))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails when a successful response has no body`() =
        runTest {
            // Retrofit reports 204/205 as a successful response with a null body.
            server.enqueue(MockResponse(code = 204, body = ""))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on malformed json`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = "{ \"total_count\": "))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails when a required key is absent`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"tag\": \"network\",", "")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on an invalid timestamp`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("2026-08-16T17:10:05Z", "17:10:05")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on a blank required value`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"network\"", "\"   \"")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on a blank session id`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"session-666\"", "\"\"")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on a negative latency`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"latency_ms\": 2040", "\"latency_ms\": -1")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs fails on a negative total count`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"total_count\": 2", "\"total_count\": -1")))

            assertThat(repository().getLogs()).isEqualTo(FAILURE)
        }

    @Test
    fun `getLogs maps the payload on the injected dispatcher`() =
        runTest {
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))
            val mappingDispatcher = CountingDispatcher()

            NetworkLogsRepository(logsApi, mappingDispatcher).getLogs()

            assertThat(mappingDispatcher.dispatchCount).isGreaterThan(0)
        }

    private fun repository(dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()): LogsRepository =
        NetworkLogsRepository(logsApi, dispatcher)
}

/** Records that mapping was actually dispatched away from the calling thread. */
private class CountingDispatcher : CoroutineDispatcher() {
    var dispatchCount = 0
        private set

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        dispatchCount++
        block.run()
    }
}

/** Mirrors the `:core:network` configuration so the tests exercise the same decoding behavior. */
private val JSON = Json { ignoreUnknownKeys = true }

private val FAILURE = Result.Error(LogsDataError)

private val EXPECTED_BATCH =
    LogBatch(
        reportedTotalCount = 2,
        sessionId = "session-666",
        entries =
            listOf(
                LogEntry(
                    id = "log-1",
                    timestamp = Instant.parse("2026-08-16T17:10:05Z"),
                    severity = Severity.ERROR,
                    tag = "network",
                    message = "Connection timed out",
                    latencyMs = 2040,
                    isAiGenerated = true,
                    sessionId = "session-666",
                ),
                LogEntry(
                    id = "log-2",
                    timestamp = Instant.parse("2026-08-16T17:11:06.250Z"),
                    severity = Severity.DEBUG,
                    tag = "neural_engine",
                    message = "Inference complete",
                    latencyMs = 12,
                    isAiGenerated = false,
                    sessionId = "session-666",
                ),
            ),
    )

private val VALID_PAYLOAD =
    """
    {
      "total_count": 2,
      "session_id": "session-666",
      "data": [
        {
          "id": "log-1",
          "timestamp": "2026-08-16T17:10:05Z",
          "severity": "ERROR",
          "tag": "network",
          "message": "Connection timed out",
          "metadata": { "latency_ms": 2040, "is_ai_generated": true }
        },
        {
          "id": "log-2",
          "timestamp": "2026-08-16T17:11:06.250Z",
          "severity": "DEBUG",
          "tag": "neural_engine",
          "message": "Inference complete",
          "metadata": { "latency_ms": 12, "is_ai_generated": false }
        }
      ]
    }
    """.trimIndent()

private val PAYLOAD_WITH_UNKNOWN_KEYS =
    VALID_PAYLOAD
        .replace("\"total_count\": 2,", "\"schema_version\": 3,\n\"total_count\": 2,")
        .replace("\"id\": \"log-1\",", "\"id\": \"log-1\",\n\"trace_id\": \"abc\",")
        .replace("\"is_ai_generated\": true", "\"is_ai_generated\": true, \"model\": \"v2\"")
