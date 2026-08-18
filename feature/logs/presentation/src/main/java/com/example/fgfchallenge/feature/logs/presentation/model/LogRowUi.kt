package com.example.fgfchallenge.feature.logs.presentation.model

import androidx.compose.runtime.Immutable
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone

/**
 * Display-ready values for one log row.
 *
 * Every field is already formatted for rendering: [severityLabel] and [tagLabel] are the pill
 * texts, and [time] is the `ss.SSS` tail time (the minute lives in the group header). [id] is the
 * log's identity, used for the row's stable list key and, from Roadmap #4 on, for selection.
 */
@Immutable
internal data class LogRowUi(
    val id: String,
    val severityLabel: String,
    val severityTone: SeverityBadgeTone,
    val tagLabel: String,
    val message: String,
    val time: String,
)
