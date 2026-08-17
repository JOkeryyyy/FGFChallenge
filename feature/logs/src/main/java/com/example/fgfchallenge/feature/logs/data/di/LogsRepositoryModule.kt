package com.example.fgfchallenge.feature.logs.data.di

import com.example.fgfchallenge.feature.logs.data.repository.LogsRepository
import com.example.fgfchallenge.feature.logs.data.repository.NetworkLogsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the repository contract to its network implementation, so consumers only ever see
 * [LogsRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LogsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLogsRepository(repository: NetworkLogsRepository): LogsRepository
}
