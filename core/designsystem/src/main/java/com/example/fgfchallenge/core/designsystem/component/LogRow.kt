package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Dimens
import com.example.fgfchallenge.core.designsystem.token.Spacing

private val SeverityColumnWidth = 56.dp
private val TagColumnWidth = 88.dp
private val TimeColumnWidth = 52.dp

/**
 * One log entry: fixed-width severity and tag pills keep those columns aligned across rows,
 * the message is the only element that compresses, and the tail time column uses tabular
 * figures so digits line up vertically.
 */
@Composable
fun LogRow(
    severityLabel: String,
    severityTone: SeverityBadgeTone,
    tagLabel: String,
    message: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.minTouchTarget)
                    .clickable(onClick = onClick)
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SeverityBadge(
                label = severityLabel,
                tone = severityTone,
                modifier = Modifier.width(SeverityColumnWidth),
            )
            TagBadge(
                label = tagLabel,
                modifier = Modifier.width(TagColumnWidth),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = time,
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(TimeColumnWidth),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LogRowPreview() {
    FGFChallengeTheme {
        Column {
            LogRow(
                severityLabel = "ERROR",
                severityTone = SeverityBadgeTone.Error,
                tagLabel = "network",
                message = "Connection timed out",
                time = "58.123",
                onClick = {},
            )
            LogRow(
                severityLabel = "DEBUG",
                severityTone = SeverityBadgeTone.Debug,
                tagLabel = "cache",
                message = "Cache lookup key=1234",
                time = "11.098",
                onClick = {},
            )
        }
    }
}
