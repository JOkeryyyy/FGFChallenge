package com.example.fgfchallenge.feature.logs.data.local

import assertk.assertThat
import assertk.assertions.contains
import com.example.fgfchallenge.feature.logs.data.local.LogsTestFixture.SNAPSHOT
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the traversal indices the log queries are shaped around.
 *
 * These are a performance decision, not a correctness one — every query in [LogsDaoQueryTest]
 * returns the same rows without them — which is exactly why they need a test. Dropping one would
 * leave every assertion in this module green while the paged select regained a temporary B-tree
 * sort and the filter aggregate went back to reading rows it does not need.
 *
 * The columns are asserted, not the query plan: a plan is the planner's choice and varies with
 * SQLite version and table statistics, while the index a column list describes is the contract this
 * schema actually makes.
 */
@RunWith(RobolectricTestRunner::class)
class LogsSchemaIndexTest {
    @Test
    fun `the snapshot table carries the composite ordering and filter indices`() =
        runTest {
            val database = createInMemoryLogsDatabase(StandardTestDispatcher(testScheduler))
            try {
                database.logsDao().replaceSnapshot(SNAPSHOT)
                val indices = database.indexColumns()

                // Every select orders by timestamp then ID; a timestamp-only index leaves the ID to
                // a temporary B-tree.
                assertThat(indices).contains(
                    listOf(LogEntity.COLUMN_TIMESTAMP, LogEntity.COLUMN_ID),
                )
                // Lets the filter aggregate and the paging count be answered from the index alone.
                assertThat(indices).contains(
                    listOf(
                        LogEntity.COLUMN_TAG,
                        LogEntity.COLUMN_SEVERITY,
                        LogEntity.COLUMN_IS_AI_GENERATED,
                    ),
                )
                // Severity is not the leading column of either composite, so it keeps its own.
                assertThat(indices).contains(listOf(LogEntity.COLUMN_SEVERITY))
            } finally {
                database.close()
            }
        }
}

/** Every index on the snapshot table, as its ordered column list. */
private fun LogsDatabase.indexColumns(): List<List<String>> {
    val sqlite = openHelper.readableDatabase
    val names = mutableListOf<String>()
    sqlite.query("PRAGMA index_list(`${LogEntity.TABLE_NAME}`)").use { cursor ->
        val nameColumn = cursor.getColumnIndexOrThrow("name")
        while (cursor.moveToNext()) {
            names += cursor.getString(nameColumn)
        }
    }
    return names.map { name ->
        sqlite.query("PRAGMA index_info(`$name`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(nameColumn))
                }
            }
        }
    }
}
