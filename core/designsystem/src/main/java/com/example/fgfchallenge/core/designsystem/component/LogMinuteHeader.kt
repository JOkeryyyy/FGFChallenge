package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/**
 * Reserves a collapse affordance for future use: the chevron only renders, and only reports a
 * new value, when the caller supplies [onCollapsedChange]. The header never stores collapsed
 * state itself, so a caller that omits the callback gets a plain, non-collapsible heading.
 */
@Composable
fun LogMinuteHeader(
    minute: String,
    itemCount: Int,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = false,
    onCollapsedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = minute,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "· $itemCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onCollapsedChange != null) {
            val rotation by animateFloatAsState(
                targetValue = if (isCollapsed) 180f else 0f,
                label = "minuteHeaderChevronRotation",
            )
            IconButton(onClick = { onCollapsedChange(!isCollapsed) }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription =
                        stringResource(
                            if (isCollapsed) {
                                R.string.log_minute_header_expand_action
                            } else {
                                R.string.log_minute_header_collapse_action
                            },
                        ),
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderStaticPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", itemCount = 12)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderExpandedPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", itemCount = 12, isCollapsed = false, onCollapsedChange = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderCollapsedPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", itemCount = 12, isCollapsed = true, onCollapsedChange = {})
    }
}
