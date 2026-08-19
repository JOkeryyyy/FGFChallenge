package com.example.fgfchallenge.feature.logs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The feature-owned store for the current log snapshot.
 *
 * It lives in `:feature:logs:data` rather than a shared `:core:database` module because no other
 * feature reads logs; a generic module would be an abstraction with one consumer.
 *
 * Schema export and migrations are deliberately absent. Every launch replaces the whole snapshot
 * from the network, so the database holds no state worth carrying across a schema change — a
 * version bump can recreate it. That changes the moment anything user-owned is stored here.
 */
@Database(
    entities = [LogEntity::class],
    // 2: the traversal indices changed shape. Nothing user-owned is stored here, so the
    // destructive fallback recreates the table and the next launch refresh refills it.
    version = 2,
    exportSchema = false,
)
internal abstract class LogsDatabase : RoomDatabase() {
    abstract fun logsDao(): LogsDao

    internal companion object {
        const val NAME = "logs.db"
    }
}
