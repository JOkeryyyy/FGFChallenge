package com.example.fgfchallenge.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builders for the app-wide HTTP stack: the shared Kotlinx Serialization [Json] configuration,
 * the tuned [OkHttpClient], and the [Retrofit] instance feature modules use to create their own
 * endpoint APIs.
 *
 * These are plain functions rather than inline `@Provides` bodies so that the debug/release
 * logging decision and the timeout policy can be unit-tested without a Hilt graph. The module
 * intentionally knows nothing about endpoints, DTOs, or feature error types.
 */
internal object NetworkConfig {
    /**
     * Host only. Endpoint paths stay with the feature that owns them so `:core:network` remains
     * reusable and unaware of the logs API.
     */
    const val BASE_URL: String = "https://firebasestorage.googleapis.com/"

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_WRITE_TIMEOUT_SECONDS = 30L
    private const val CALL_TIMEOUT_SECONDS = 45L

    private val jsonMediaType = "application/json".toMediaType()

    fun createJson(): Json =
        Json {
            // Forward compatibility: the payload may grow fields the prototype does not model.
            ignoreUnknownKeys = true
        }

    fun createOkHttpClient(debugLoggingEnabled: Boolean): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(READ_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .apply {
                if (debugLoggingEnabled) {
                    // BASIC records only the request line and the response status/size, never
                    // bodies, so a ~5,000-entry payload can never reach logcat.
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                }
            }.build()

    fun createRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(jsonMediaType))
            .build()
}
