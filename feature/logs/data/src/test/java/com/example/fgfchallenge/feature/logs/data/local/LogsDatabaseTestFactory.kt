package com.example.fgfchallenge.feature.logs.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.coroutines.CoroutineContext

/**
 * Builds the real [LogsDatabase] in memory for host-side tests.
 *
 * These run against genuine SQLite under Robolectric rather than an emulator or a stubbed
 * database, so the SQL, the wildcard escaping, and the ordering under test are the ones that ship.
 *
 * [queryContext] is the test's own dispatcher, which keeps Room's work inside `runTest`'s
 * scheduler instead of a background thread the test would have to wait on.
 */
internal fun createInMemoryLogsDatabase(queryContext: CoroutineContext): LogsDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LogsDatabase::class.java,
        ).setQueryCoroutineContext(queryContext)
        .build()
