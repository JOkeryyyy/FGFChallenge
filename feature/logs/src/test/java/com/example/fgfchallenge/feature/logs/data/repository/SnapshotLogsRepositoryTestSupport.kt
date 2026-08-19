package com.example.fgfchallenge.feature.logs.data.repository

import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.local.createInMemoryLogsDatabase
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext

/**
 * Assembles the repository under test over a real in-memory database and the supplied [logsApi].
 *
 * The database is the one that ships, so a test asserting "the previous snapshot survived" is
 * asserting real transactional behavior rather than a fake's bookkeeping. Room's query context and
 * the repository's mapping dispatcher both run on the test scheduler, so nothing escapes `runTest`.
 *
 * The [LogsDao] is handed to the test alongside the repository purely to arrange a pre-existing
 * snapshot and to count rows — the production caller only ever sees [LogsRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun runLogsRepositoryTest(
    logsApi: LogsApi,
    mappingDispatcher: CoroutineDispatcher? = null,
    block: suspend (LogsRepository, LogsDao) -> Unit,
): Unit =
    runTest {
        val queryDispatcher = StandardTestDispatcher(testScheduler)
        val database = createInMemoryLogsDatabase(queryDispatcher)
        try {
            val dao = database.logsDao()
            // Assembled the way the shipping variants are: the real remote strategy behind the
            // repository, so these tests keep exercising the production write path end to end.
            val refresher = RemoteSnapshotRefresher(logsApi, dao, mappingDispatcher ?: queryDispatcher)
            block(SnapshotLogsRepository(refresher, dao), dao)
        } finally {
            database.close()
        }
    }

/** Records that payload mapping was actually dispatched away from the calling thread. */
internal class CountingDispatcher : CoroutineDispatcher() {
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
