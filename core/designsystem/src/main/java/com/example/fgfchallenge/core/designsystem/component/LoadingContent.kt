package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.modifier.shimmerEffect
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/**
 * Skeleton placeholders only, no fabricated log values. The whole subtree is hidden from
 * TalkBack and replaced by one "Loading logs" announcement.
 */
@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading_content_description)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description }
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(modifier = Modifier.size(72.dp), shape = CircleShape)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SkeletonBlock(modifier = Modifier.width(120.dp).height(14.dp))
                SkeletonBlock(modifier = Modifier.width(160.dp).height(14.dp))
                SkeletonBlock(modifier = Modifier.width(100.dp).height(14.dp))
            }
        }
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        )
        repeat(8) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(modifier = Modifier.width(56.dp).height(20.dp))
                SkeletonBlock(modifier = Modifier.width(88.dp).height(20.dp))
                SkeletonBlock(modifier = Modifier.weight(1f).height(20.dp))
                SkeletonBlock(modifier = Modifier.width(40.dp).height(20.dp))
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(
        modifier =
            modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = shape)
                .shimmerEffect(),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun LoadingContentPreview() {
    FGFChallengeTheme {
        LoadingContent()
    }
}
