package com.example.fgfchallenge.core.designsystem.model

import androidx.compose.runtime.Immutable

/**
 * Display-ready severity summary rendered by `SeverityIndicator`.
 *
 * Ring geometry and legend travel together in one value so they cannot disagree: presentation
 * derives [densityPercent], both fractions, and [legendItems] from the same result set in a single
 * calculation. The design system only draws what it is given — it never computes density itself.
 *
 * @param densityPercent combined `(ERROR + FATAL)` share of the result set, 0..100, already rounded.
 * @param errorFraction ERROR share of the result set as 0f..1f of the full ring.
 * @param fatalFraction FATAL share of the result set as 0f..1f of the full ring.
 */
@Immutable
data class SeverityDensityUi(
    val densityPercent: Int,
    val errorFraction: Float,
    val fatalFraction: Float,
    val legendItems: List<SeverityLegendItem>,
)
