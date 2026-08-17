package com.example.fgfchallenge.core.network.di

import com.example.fgfchallenge.core.network.BuildConfig
import com.example.fgfchallenge.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt bindings for the reusable HTTP stack. Feature modules inject [Retrofit] and create their
 * own endpoint API from it; no endpoint, DTO, or feature failure type is declared here.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = NetworkConfig.createJson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = NetworkConfig.createOkHttpClient(debugLoggingEnabled = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = NetworkConfig.createRetrofit(okHttpClient = okHttpClient, json = json)
}
