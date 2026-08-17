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
 * Static UTC-minute group heading with its entry count. Deliberately non-interactive: the
 * wireframe specifies static, non-collapsible minute groups, so this carries no collapse
 * affordance and no state of its own.
 */
@Composable
fun LogMinuteHeader(
    minute: String,
    itemCount: Int,
    modifier: Modifier = Modifier,
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
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogMinuteHeaderStaticPreview() {
    FGFChallengeTheme {
        LogMinuteHeader(minute = "17:11", itemCount = 12)
    }
}
