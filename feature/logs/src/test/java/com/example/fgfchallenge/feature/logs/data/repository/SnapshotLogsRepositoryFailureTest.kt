package com.example.fgfchallenge.feature.logs.data.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.example.fgfchallenge.feature.logs.data.error.LogsDataError
import com.example.fgfchallenge.feature.logs.data.error.Result
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture.SNAPSHOT
import com.example.fgfchallenge.feature.logs.data.remote.LogEntryDto
import com.example.fgfchallenge.feature.logs.data.remote.LogMetadataDto
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import com.example.fgfchallenge.feature.logs.data.remote.LogsPayloadDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Failures that a served response cannot produce: transport exceptions, cancellation, and a broken
 * database.
 *
 * Each case drives the repository with a fake [LogsApi] that throws one specific exception, and
 * asserts both halves of the contract — that the failure stops at the data boundary, and that the
 * snapshot stored before the attempt is still intact afterwards. Cancellation is the exception to
 * the first half: it must propagate.
 */
@RunWith(RobolectricTestRunner::class)
class SnapshotLogsRepositoryFailureTest {
    @Test
    fun `unresolved host is a data failure`() = assertContained(UnknownHostException("no dns"))

    @Test
    fun `socket timeout is a data failure`() = assertContained(SocketTimeoutException("timeout"))

    @Test
    fun `unexpected io failure is a data failure`() = assertContained(IOException("broken pipe"))

    @Test
    fun `unexpected runtime failure is a data failure`() = assertContained(IllegalStateException("boom"))

    @Test
    fun `cancellation is rethrown unchanged`() {
        val cancellation = CancellationException("cancelled")

        runLogsRepositoryTest(ThrowingLogsApi(cancellation)) { repository, dao ->
            dao.replaceSnapshot(SNAPSHOT)

            val thrown =
                try {
                    repository.refreshSnapshot()
                    null
                } catch (caught: CancellationException) {
                    caught
                }

            // Same instance, not a substitute: converting cancellation into a data error would
            // break structured concurrency for the caller.
            assertThat(thrown).isSameInstanceAs(cancellation)
            assertThat(dao.count()).isEqualTo(SNAPSHOT.size)
        }
    }

    @Test
    fun `undecodable payload is a data failure`() = assertContained(SerializationException("bad json"))

    /**
     * The one failure the endpoint cannot stage: the response arrives and maps cleanly, and the
     * write itself then fails. Two entries sharing an ID is the deterministic way to reach it —
     * the payload is structurally and semantically valid, and the primary key rejects it mid-insert.
     */
    @Test
    fun `a database write failure is contained and rolls back`() {
        runLogsRepositoryTest(DuplicateIdLogsApi) { repository, dao ->
            dao.replaceSnapshot(SNAPSHOT)

            assertThat(repository.refreshSnapshot()).isEqualTo(Result.Error(LogsDataError))
            // The deletion is part of the same transaction, so a failed insert cannot leave the
            // previous snapshot half-removed.
            assertThat(dao.count()).isEqualTo(SNAPSHOT.size)
        }
    }

    @Test
    fun `a lookup failure is contained rather than thrown`() {
        runLogsRepositoryTest(ThrowingLogsApi(IOException("unused"))) { repository, dao ->
            dao.replaceSnapshot(SNAPSHOT)

            // A missing ID is a successful lookup with no payload, not an error.
            assertThat(repository.logById("log-absent")).isEqualTo(Result.Success(null))
        }
    }

    private fun assertContained(failure: Throwable) {
        runLogsRepositoryTest(ThrowingLogsApi(failure)) { repository, dao ->
            dao.replaceSnapshot(SNAPSHOT)

            assertThat(repository.refreshSnapshot()).isEqualTo(Result.Error(LogsDataError))
            // The failed launch is retryable precisely because it changed nothing.
            assertThat(dao.count()).isEqualTo(SNAPSHOT.size)
        }
    }
}

private class ThrowingLogsApi(
    private val failure: Throwable,
) : LogsApi {
    override suspend fun getLogs(): Response<LogsPayloadDto> = throw failure
}

/**
 * Serves a payload that decodes and maps cleanly but cannot be stored: both entries claim the same
 * ID, which the primary key rejects part-way through the insert.
 */
private object DuplicateIdLogsApi : LogsApi {
    override suspend fun getLogs(): Response<LogsPayloadDto> {
        val entry =
            LogEntryDto(
                id = "log-1",
                timestamp = "2026-08-16T17:10:05Z",
                severity = "ERROR",
                tag = "network",
                message = "Connection timed out",
                metadata = LogMetadataDto(latencyMs = 2040, isAiGenerated = true),
            )
        return Response.success(
            LogsPayloadDto(totalCount = 2, sessionId = "session-666", entries = listOf(entry, entry)),
        )
    }
}
