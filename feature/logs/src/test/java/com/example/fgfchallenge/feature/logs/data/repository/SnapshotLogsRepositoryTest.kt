package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture
import com.example.fgfchallenge.feature.logs.data.local.matchingIds
import com.example.fgfchallenge.feature.logs.data.model.LogEntry
import com.example.fgfchallenge.feature.logs.data.model.LogQuery
import com.example.fgfchallenge.feature.logs.data.model.Severity
import com.example.fgfchallenge.feature.logs.data.remote.LOGS_ENDPOINT
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.time.Instant

/**
 * The launch refresh, end to end: the real Retrofit + Kotlinx Serialization + Room path against
 * MockWebServer and a genuine in-memory database.
 *
 * Two things are asserted for every response — the result the repository returns, and what the
 * stored snapshot looks like afterwards. Every rejection produces the same [LogsDataError], so each
 * failing case is here to pin down which response is refused and that refusing it leaves the
 * previous snapshot intact, not to distinguish error values. Failures only the transport or the
 * database can raise are covered by [SnapshotLogsRepositoryFailureTest].
 */
@RunWith(RobolectricTestRunner::class)
class SnapshotLogsRepositoryTest {
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
    fun `refreshSnapshot issues a single GET to the supplied firebase route`() =
        withRepository { repository, _ ->
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            repository.refreshSnapshot()

            assertThat(server.requestCount).isEqualTo(1)
            val recorded = server.takeRequest()
            assertThat(recorded.method).isEqualTo("GET")
            assertThat(recorded.target).isEqualTo("/$LOGS_ENDPOINT")
        }

    @Test
    fun `refreshSnapshot stores the mapped payload`() =
        withRepository { repository, dao ->
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            val result = repository.refreshSnapshot()

            assertThat(result).isEqualTo(Result.Success(Unit))
            assertThat(dao.count()).isEqualTo(2)
            assertThat(repository.logById("log-1")).isEqualTo(Result.Success(EXPECTED_FIRST_ENTRY))
        }

    @Test
    fun `refreshSnapshot replaces rather than accumulates`() =
        withRepository { repository, dao ->
            dao.replaceSnapshot(LogsTestFixture.SNAPSHOT)
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            repository.refreshSnapshot()

            // A second launch swaps the whole snapshot: nothing from the previous one survives.
            assertThat(dao.count()).isEqualTo(2)
            assertThat(dao.matchingIds(LogQuery())).isEqualTo(listOf("log-2", "log-1"))
        }

    @Test
    fun `refreshSnapshot ignores unknown json keys`() =
        withRepository { repository, dao ->
            server.enqueue(MockResponse(code = 200, body = PAYLOAD_WITH_UNKNOWN_KEYS))

            assertThat(repository.refreshSnapshot()).isEqualTo(Result.Success(Unit))
            assertThat(dao.count()).isEqualTo(2)
        }

    @Test
    fun `refreshSnapshot stores the actual entries when total_count disagrees`() =
        withRepository { repository, dao ->
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"total_count\": 2", "\"total_count\": 5000")))

            assertThat(repository.refreshSnapshot()).isEqualTo(Result.Success(Unit))
            assertThat(dao.count()).isEqualTo(2)
        }

    @Test
    fun `refreshSnapshot stores an unrecognized severity as UNKNOWN`() =
        withRepository { repository, _ ->
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD.replace("\"ERROR\"", "\"TRACE\"")))

            repository.refreshSnapshot()

            val stored = (repository.logById("log-1") as Result.Success).data
            assertThat(stored).isNotNull()
            assertThat(stored?.severity).isEqualTo(Severity.UNKNOWN)
        }

    @Test
    fun `refreshSnapshot fails on an unsuccessful response`() = assertRefreshRejects(MockResponse(code = 404, body = "not found"))

    @Test
    fun `refreshSnapshot fails when a successful response has no body`() =
        // Retrofit reports 204/205 as a successful response with a null body.
        assertRefreshRejects(MockResponse(code = 204, body = ""))

    @Test
    fun `refreshSnapshot fails on malformed json`() = assertRefreshRejects(body = "{ \"total_count\": ")

    @Test
    fun `refreshSnapshot fails when a required key is absent`() = assertRefreshRejects(payloadWithout("\"tag\": \"network\","))

    @Test
    fun `refreshSnapshot fails on an invalid timestamp`() = assertRefreshRejects(payloadWith("2026-08-16T17:10:05Z", "17:10:05"))

    @Test
    fun `refreshSnapshot fails on a blank required value`() = assertRefreshRejects(payloadWith("\"network\"", "\"   \""))

    @Test
    fun `refreshSnapshot fails on a blank session id`() = assertRefreshRejects(payloadWith("\"session-666\"", "\"\""))

    @Test
    fun `refreshSnapshot fails on a negative latency`() = assertRefreshRejects(payloadWith("\"latency_ms\": 2040", "\"latency_ms\": -1"))

    @Test
    fun `refreshSnapshot fails on a negative total count`() = assertRefreshRejects(payloadWith("\"total_count\": 2", "\"total_count\": -1"))

    @Test
    fun `refreshSnapshot maps the payload on the injected dispatcher`() {
        val mappingDispatcher = CountingDispatcher()

        runLogsRepositoryTest(logsApi, mappingDispatcher) { repository, _ ->
            server.enqueue(MockResponse(code = 200, body = VALID_PAYLOAD))

            repository.refreshSnapshot()
        }

        assertThat(mappingDispatcher.dispatchCount).isGreaterThan(0)
    }

    /**
     * Asserts that [response] is refused *and* that the snapshot stored before the attempt is still
     * the complete previous one — the invariant that makes a failed launch retryable rather than
     * destructive.
     */
    private fun assertRefreshRejects(response: MockResponse) =
        withRepository { repository, dao ->
            dao.replaceSnapshot(LogsTestFixture.SNAPSHOT)
            server.enqueue(response)

            assertThat(repository.refreshSnapshot()).isEqualTo(Result.Error(LogsDataError))
            assertThat(dao.count()).isEqualTo(LogsTestFixture.SNAPSHOT.size)
        }

    private fun assertRefreshRejects(body: String) = assertRefreshRejects(MockResponse(code = 200, body = body))

    private fun withRepository(block: suspend (LogsRepository, LogsDao) -> Unit) = runLogsRepositoryTest(logsApi, block = block)
}

private fun payloadWith(
    original: String,
    replacement: String,
): String = VALID_PAYLOAD.replace(original, replacement)

private fun payloadWithout(fragment: String): String = VALID_PAYLOAD.replace(fragment, "")

/** Mirrors the `:core:network` configuration so the tests exercise the same decoding behavior. */
private val JSON = Json { ignoreUnknownKeys = true }

private val EXPECTED_FIRST_ENTRY =
    LogEntry(
        id = "log-1",
        timestamp = Instant.parse("2026-08-16T17:10:05Z"),
        severity = Severity.ERROR,
        tag = "network",
        message = "Connection timed out",
        latencyMs = 2040,
        isAiGenerated = true,
        sessionId = "session-666",
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
