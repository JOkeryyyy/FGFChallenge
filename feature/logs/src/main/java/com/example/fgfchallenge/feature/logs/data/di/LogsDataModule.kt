package com.example.fgfchallenge.feature.logs.data.di

import android.content.Context
import androidx.room.Room
import com.example.fgfchallenge.feature.logs.data.local.LogsDao
import com.example.fgfchallenge.feature.logs.data.local.LogsDatabase
import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import com.example.fgfchallenge.feature.logs.data.repository.LogsRepository
import com.example.fgfchallenge.feature.logs.data.repository.SnapshotLogsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the CPU-work dispatcher used for validating and mapping the log payload.
 *
 * It is injected rather than referenced as `Dispatchers.Default` inside the repository so tests
 * can substitute a test dispatcher and stay deterministic.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DefaultDispatcher

/**
 * The logs data layer's only Hilt module: the endpoint API, the snapshot database and its DAO, the
 * repository binding, and the mapping dispatcher.
 *
 * These bindings are kept together rather than split per concern because they share one lifetime
 * and one owner; separate modules would add files without adding an architectural boundary. The
 * `@Binds` method must be abstract, so the `@Provides` bindings live in the companion object.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LogsDataModule {
    @Binds
    @Singleton
    abstract fun bindLogsRepository(repository: SnapshotLogsRepository): LogsRepository

    companion object {
        /**
         * Builds the feature's endpoint from the shared [Retrofit] instance `:core:network`
         * provides. Creating it here is what lets `:core:network` stay unaware of the logs
         * endpoint and its DTOs.
         */
        @Provides
        @Singleton
        fun provideLogsApi(retrofit: Retrofit): LogsApi = retrofit.create()

        /**
         * Scoped to the application: opening the database is expensive, and one instance is what
         * lets Room invalidate the active queries when the snapshot is replaced.
         */
        @Provides
        @Singleton
        fun provideLogsDatabase(
            @ApplicationContext context: Context,
        ): LogsDatabase =
            Room
                .databaseBuilder(context, LogsDatabase::class.java, LogsDatabase.NAME)
                // The stored snapshot is a cache of the last launch's response, so a schema change
                // recreates the table instead of migrating it. This is the other half of
                // `exportSchema = false`: without it, a version bump crashes on an existing install.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides
        fun provideLogsDao(database: LogsDatabase): LogsDao = database.logsDao()

        @Provides
        @DefaultDispatcher
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
