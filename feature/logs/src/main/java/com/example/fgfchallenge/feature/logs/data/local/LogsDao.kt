package com.example.fgfchallenge.feature.logs.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Every read and write the log snapshot supports.
 *
 * The two query-driven reads take a [RoomRawQuery] built by [LogQuerySql] rather than declaring
 * their conditions here: the filter set is combinatorial, and a static `@Query` would either need
 * a tautology per inactive category or a different statement per combination. Declaring
 * `observedEntities` is what still lets Room invalidate the `PagingSource` and the summary `Flow`
 * when the snapshot is replaced.
 *
 * The unfiltered reads below stay `@Query`, so Room verifies their SQL at compile time.
 */
@Dao
internal interface LogsDao {
    /**
     * Rows for [LogQuerySql.pagedSelect]. Room appends the page's `LIMIT`/`OFFSET`, so only the
     * bounded working set is ever read out of the database.
     */
    @RawQuery(observedEntities = [LogEntity::class])
    fun pagedLogs(query: RoomRawQuery): PagingSource<Int, LogEntity>

    /**
     * Per-severity counts over the complete filtered result, from
     * [LogQuerySql.severityCountSelect]. Severities with no matching row are simply absent.
     */
    @RawQuery(observedEntities = [LogEntity::class])
    fun severityCounts(query: RoomRawQuery): Flow<List<SeverityCountRow>>

    @Query("SELECT DISTINCT ${LogEntity.COLUMN_TAG} FROM ${LogEntity.TABLE_NAME} ORDER BY ${LogEntity.COLUMN_TAG} ASC")
    fun availableTags(): Flow<List<String>>

    /**
     * The stored latency extent. Aggregates over the whole table without materializing it; both
     * columns are null when no snapshot is stored.
     */
    @Query(
        "SELECT MIN(${LogEntity.COLUMN_LATENCY_MS}) AS ${LatencyBoundsRow.COLUMN_MINIMUM}," +
            " MAX(${LogEntity.COLUMN_LATENCY_MS}) AS ${LatencyBoundsRow.COLUMN_MAXIMUM}" +
            " FROM ${LogEntity.TABLE_NAME}",
    )
    fun latencyBounds(): Flow<LatencyBoundsRow>

    /**
     * Details lookup by stable ID, so a selected row stays resolvable no matter which pages Paging
     * currently holds.
     */
    @Query("SELECT * FROM ${LogEntity.TABLE_NAME} WHERE ${LogEntity.COLUMN_ID} = :id")
    suspend fun logById(id: String): LogEntity?

    @Query("SELECT COUNT(*) FROM ${LogEntity.TABLE_NAME}")
    suspend fun count(): Int

    /**
     * Swaps the stored snapshot for [entities] in one transaction.
     *
     * Observers see the complete previous snapshot or the complete new one — never the emptied
     * table in between — and any failure during insertion rolls the deletion back with it.
     */
    @Transaction
    suspend fun replaceSnapshot(entities: List<LogEntity>) {
        deleteAll()
        insertAll(entities)
    }

    @Query("DELETE FROM ${LogEntity.TABLE_NAME}")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entities: List<LogEntity>)
}
