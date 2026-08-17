package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.example.fgfchallenge.core.designsystem.theme.containerColor
import com.example.fgfchallenge.core.designsystem.theme.contentColor

/** Filled severity pill. High-recognition tone color carries meaning alongside the text label. */
@Composable
fun SeverityBadge(
    label: String,
    tone: SeverityBadgeTone,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = tone.containerColor,
                    shape = MaterialTheme.shapes.extraSmall,
                ).padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tone.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun SeverityBadgeAllTonesPreview() {
    FGFChallengeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SeverityBadge(label = "DEBUG", tone = SeverityBadgeTone.Debug)
            SeverityBadge(label = "INFO", tone = SeverityBadgeTone.Info)
            SeverityBadge(label = "WARN", tone = SeverityBadgeTone.Warn)
            SeverityBadge(label = "ERROR", tone = SeverityBadgeTone.Error)
            SeverityBadge(label = "FATAL", tone = SeverityBadgeTone.Fatal)
            SeverityBadge(label = "UNKNOWN", tone = SeverityBadgeTone.Unknown)
        }
    }
}
