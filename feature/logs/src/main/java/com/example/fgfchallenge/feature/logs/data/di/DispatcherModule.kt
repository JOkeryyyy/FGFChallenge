package com.example.fgfchallenge.feature.logs.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Supplies the CPU-work dispatcher used for validating and mapping the log payload.
 *
 * It is injected rather than referenced as `Dispatchers.Default` inside the repository so tests
 * can substitute a test dispatcher and stay deterministic.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DispatcherModule {
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
