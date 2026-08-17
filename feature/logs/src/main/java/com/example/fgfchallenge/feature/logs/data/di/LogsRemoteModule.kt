package com.example.fgfchallenge.feature.logs.data.di

import com.example.fgfchallenge.feature.logs.data.remote.LogsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/**
 * Creates the feature's endpoint API from the shared [Retrofit] instance `:core:network`
 * provides. Keeping this binding here is what lets `:core:network` stay unaware of the logs
 * endpoint and its DTOs.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object LogsRemoteModule {
    @Provides
    @Singleton
    fun provideLogsApi(retrofit: Retrofit): LogsApi = retrofit.create()
}
