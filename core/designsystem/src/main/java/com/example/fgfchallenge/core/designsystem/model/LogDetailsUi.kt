package com.example.fgfchallenge.core.designsystem.model

import androidx.compose.runtime.Immutable

/**
 * Display-ready values for the structured log details sheet.
 *
 * Every field is already formatted by the caller — the sheet renders strings and performs no
 * parsing, arithmetic, or locale work. [timestampUtc] is the full UTC instant, unlike the row's
 * `ss.SSS` tail time. Marked [Immutable] because it is held in screen state and read on every
 * recomposition while the sheet is open.
 */
@Immutable
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
