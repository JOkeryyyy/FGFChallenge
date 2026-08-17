package com.example.fgfchallenge.core.designsystem.model

data class LogDetailsUi(
    val severityLabel: String,
    val severityTone: SeverityBadgeTone,
    val message: String,
    val tag: String,
    val timestampUtc: String,
    val latency: String,
    val aiGenerated: String,
    val logId: String,
    val sessionId: String,
)
