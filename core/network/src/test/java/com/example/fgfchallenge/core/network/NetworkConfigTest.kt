package com.example.fgfchallenge.core.network

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Verifies the HTTP stack configuration that `:core:network` owns: debug-only BASIC logging,
 * the timeout policy, the shared base URL, and unknown-key tolerance.
 */
class NetworkConfigTest {
    @Test
    fun `debug client installs exactly one BASIC logging interceptor`() {
        val client = NetworkConfig.createOkHttpClient(debugLoggingEnabled = true)

        val loggingInterceptors = client.interceptors.filterIsInstance<HttpLoggingInterceptor>()
        assertThat(loggingInterceptors).hasSize(1)
        // BASIC never writes bodies; anything higher would dump the ~5,000-entry payload.
        assertThat(loggingInterceptors.single().level)
            .isEqualTo(HttpLoggingInterceptor.Level.BASIC)
    }

    @Test
    fun `release client installs no logging interceptor`() {
        val client = NetworkConfig.createOkHttpClient(debugLoggingEnabled = false)

        assertThat(client.interceptors.filterIsInstance<HttpLoggingInterceptor>()).isEmpty()
        assertThat(client.interceptors).isEmpty()
    }

    @Test
    fun `client applies the configured timeouts`() {
        val client = NetworkConfig.createOkHttpClient(debugLoggingEnabled = false)

        assertThat(client.connectTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(10).toInt())
        assertThat(client.readTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(30).toInt())
        assertThat(client.writeTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(30).toInt())
        assertThat(client.callTimeoutMillis).isEqualTo(TimeUnit.SECONDS.toMillis(45).toInt())
    }

    @Test
    fun `json ignores unknown keys`() {
        assertThat(NetworkConfig.createJson().configuration.ignoreUnknownKeys).isTrue()
    }

    @Test
    fun `retrofit targets the configured base url`() {
        val retrofit =
            NetworkConfig.createRetrofit(
                okHttpClient = NetworkConfig.createOkHttpClient(debugLoggingEnabled = false),
                json = NetworkConfig.createJson(),
            )

        assertThat(retrofit.baseUrl().toString()).isEqualTo(NetworkConfig.BASE_URL)
    }
}
