package com.example.fgfchallenge.feature.logs.data.remote

import retrofit2.Response
import retrofit2.http.GET

/**
 * The feature's only endpoint. `:core:network` supplies the host; the path, its pre-encoded
 * `%2F` separator, and the public download token supplied with the challenge belong to the
 * feature that owns the endpoint.
 *
 * `Response` (rather than a bare body) is returned so the repository can classify non-2xx
 * statuses itself instead of catching `HttpException`.
 */
internal const val LOGS_ENDPOINT: String =
    "v0/b/fieldinspectiondev.firebasestorage.app/o/data%2Flogs_5k.json" +
        "?alt=media&token=15c66bf6-9716-44da-b3d1-ba9bb241baf8"

internal interface LogsApi {
    @GET(LOGS_ENDPOINT)
    suspend fun getLogs(): Response<LogsPayloadDto>
}
