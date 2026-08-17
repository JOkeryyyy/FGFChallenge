package com.example.fgfchallenge.core.designsystem.modifier

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val NANOS_PER_MILLISECOND = 1_000_000f
private const val DEFAULT_SHIMMER_DURATION_MILLIS = 1_000

/**
 * Sweeps a translucent highlight band across the modified element, on top of whatever background
 * that element already paints.
 *
 * [highlightColor] must contrast with that background — a translucent tint of the element's own
 * color composites back to the same visible color and animates invisibly. It is a parameter rather
 * than a theme lookup because a [Modifier.Node] cannot read Material's color scheme; the
 * `@Composable` call site supplies it.
 *
 * The band is not clipped to any shape. Callers drawing a non-rectangular skeleton should clip
 * first, e.g. `Modifier.clip(shape).background(color).shimmerEffect(highlight)`.
 */
fun Modifier.shimmerEffect(
    highlightColor: Color,
    enabled: Boolean = true,
    durationMillis: Int = DEFAULT_SHIMMER_DURATION_MILLIS,
): Modifier =
    if (enabled) {
        this then ShimmerElement(highlightColor = highlightColor, durationMillis = durationMillis)
    } else {
        this
    }

private data class ShimmerElement(
    private val highlightColor: Color,
    private val durationMillis: Int,
) : ModifierNodeElement<ShimmerNode>() {
    override fun create(): ShimmerNode = ShimmerNode(highlightColor, durationMillis)

    override fun update(node: ShimmerNode) {
        node.highlightColor = highlightColor
        node.durationMillis = durationMillis
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerEffect"
        properties["highlightColor"] = highlightColor
        properties["durationMillis"] = durationMillis
    }
}

private class ShimmerNode(
    var highlightColor: Color,
    var durationMillis: Int,
) : Modifier.Node(),
    DrawModifierNode {
    private var progress by mutableFloatStateOf(0f)

    override fun onAttach() {
        // withInfiniteAnimationFrameNanos (rather than a bare withFrameNanos loop) honors
        // InfiniteAnimationPolicy, so this animation does not keep Compose tests from reaching idle.
        // The node's own coroutineScope is cancelled on detach.
        coroutineScope.launch {
            var startNanos = -1L
            while (isActive) {
                withInfiniteAnimationFrameNanos { frameNanos ->
                    if (startNanos < 0L) {
                        startNanos = frameNanos
                    }
                    val elapsedMillis = (frameNanos - startNanos) / NANOS_PER_MILLISECOND
                    progress = (elapsedMillis / durationMillis) % 1f
                }
            }
        }
    }

    // Reading progress here is a draw-phase snapshot read, so each frame invalidates only the draw
    // phase — the shimmer never triggers recomposition.
    override fun ContentDrawScope.draw() {
        drawContent()
        val bandTravel = size.width * 2f
        drawRect(
            brush =
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
                    start = Offset(bandTravel * progress - size.width, 0f),
                    end = Offset(bandTravel * progress, size.height),
                ),
        )
    }
}
