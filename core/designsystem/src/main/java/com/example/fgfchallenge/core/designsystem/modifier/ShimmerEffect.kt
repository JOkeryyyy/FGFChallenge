package com.example.fgfchallenge.core.designsystem.modifier

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * Draws a moving highlight gradient over the modified element's own background, without
 * triggering recomposition on each animation frame (the animated value is only read inside
 * [drawWithCache]'s draw-phase lambda).
 */
fun Modifier.shimmerEffect(
    enabled: Boolean = true,
    durationMillis: Int = 1_000,
): Modifier =
    if (!enabled) {
        this
    } else {
        composed {
            val transition = rememberInfiniteTransition(label = "shimmerEffect")
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "shimmerProgress",
            )
            val baseColor = MaterialTheme.colorScheme.surfaceVariant

            drawWithCache {
                val brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                baseColor.copy(alpha = 0.3f),
                                baseColor.copy(alpha = 0.9f),
                                baseColor.copy(alpha = 0.3f),
                            ),
                        start = Offset(size.width * (progress * 2f - 1f), 0f),
                        end = Offset(size.width * progress * 2f, size.height),
                    )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = brush)
                }
            }
        }
    }
