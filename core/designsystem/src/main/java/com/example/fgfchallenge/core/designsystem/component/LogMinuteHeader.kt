package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/**
 * UTC-minute group heading, optionally able to collapse its group.
 *
 * [onToggle] is what makes the header interactive, and `null` — the default — renders the static
 * heading with no click action, no chevron, and no state to announce. A caller that has nowhere to
 * put a collapsed/expanded flag therefore keeps exactly the previous behaviour, and this component
 * still holds no state of its own: [isCollapsed] is told to it, never decided here.
 *
 * [itemCount] is optional because a caller that streams its rows cannot always know it. A paged
 * list only ever holds part of the result, so the number of entries in a minute is unknown until
 * that whole minute happens to be loaded — and a count taken from the loaded window would change
 * as the user scrolls. Such a caller passes `null` and the header renders the minute alone; a
 * caller holding the complete group passes its size.
 */
@Composable
fun LogMinuteHeader(
    minute: String,
    modifier: Modifier = Modifier,
    itemCount: Int? = null,
    isCollapsed: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val toggleLabel = stringResource(R.string.log_minute_header_toggle_action)
    val state =
        stringResource(
            if (isCollapsed) {
                R.string.log_minute_header_state_collapsed
            } else {
                R.string.log_minute_header_state_expanded
            },
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // The click and the state both belong to the whole row rather than to the minute
                // text: the row is the touch target, and a state announced on one child would not
                // reach a user who focuses the other.
                .then(
                    if (onToggle == null) {
                        Modifier
                    } else {
                        Modifier
                            .semantics { stateDescription = state }
                            .clickable(onClickLabel = toggleLabel, onClick = onToggle)
                    },
                ).padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        if (onToggle != null) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                // The row already carries the action label and the state; a second description here
                // would make assistive technology read the same affordance twice.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .size(ChevronSize)
                        .rotate(if (isCollapsed) COLLAPSED_CHEVRON_ROTATION else 0f),
            )
        }
        Text(
            text = minute,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (itemCount != null) {
            Text(
                text = "· $itemCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Points the chevron at the collapsed group's heading instead of at the rows below it. */
private const val COLLAPSED_CHEVRON_ROTATION = -90f

private val ChevronSize = 18.dp

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderStaticPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", itemCount = 12)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderWithoutCountPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderExpandedPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", isCollapsed = false, onToggle = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderCollapsedPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", isCollapsed = true, onToggle = {})
    }
}
