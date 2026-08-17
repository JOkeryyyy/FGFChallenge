package com.example.fgfchallenge.feature.logs.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire representation of the supplied logs payload. These types mirror the JSON exactly — snake
 * case names, timestamps and severities still as strings — and never leave the data layer.
 *
 * Every property is required. A key that is absent or null is a structural mismatch and fails
 * decoding as `LogsDataError.Serialization`; values that decode but are semantically invalid are
 * rejected by `LogBatchMapper` as `LogsDataError.Schema`.
 */
@Serializable
internal data class LogsPayloadDto(
    @SerialName("total_count") val totalCount: Int,
    @SerialName("session_id") val sessionId: String,
    @SerialName("data") val entries: List<LogEntryDto>,
)

@Serializable
internal data class LogEntryDto(
    val id: String,
    val timestamp: String,
    val severity: String,
    val tag: String,
    val message: String,
    val metadata: LogMetadataDto,
)

@Serializable
internal data class LogMetadataDto(
    @SerialName("latency_ms") val latencyMs: Long,
    @SerialName("is_ai_generated") val isAiGenerated: Boolean,
)
