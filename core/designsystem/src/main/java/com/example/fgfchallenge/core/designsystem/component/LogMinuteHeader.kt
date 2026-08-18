package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/**
 * Static UTC-minute group heading. Deliberately non-interactive: the wireframe specifies static,
 * non-collapsible minute groups, so this carries no collapse affordance and no state of its own.
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
