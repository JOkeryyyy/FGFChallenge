package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.model.SeverityLegendItem
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.theme.containerColor
import com.example.fgfchallenge.core.designsystem.token.Spacing
import kotlin.math.roundToInt

private val RingSize = 72.dp
private val RingStrokeWidth = 8.dp
private val NarrowLegendThreshold = 380.dp

/**
 * Custom Canvas density indicator: a neutral track plus contiguous ERROR/FATAL arcs whose
 * combined sweep is the error density. The whole component collapses to one semantics node
 * carrying a complete description, since the Canvas itself exposes nothing on its own.
 */
@Composable
fun SeverityIndicator(
    totalLogCount: Int,
    errorCount: Int,
    fatalCount: Int,
    legendItems: List<SeverityLegendItem>,
    modifier: Modifier = Modifier,
) {
    val percentage = densityPercentage(totalLogCount, errorCount, fatalCount)
    val description = severityIndicatorDescription(percentage, legendItems)
    val descriptionModifier =
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < NarrowLegendThreshold) {
            Column(
                modifier = descriptionModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SeverityRing(percentage, totalLogCount, errorCount, fatalCount)
                SeverityLegendGrid(legendItems = legendItems, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = descriptionModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SeverityRing(percentage, totalLogCount, errorCount, fatalCount)
                SeverityLegendColumn(legendItems = legendItems, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SeverityRing(
    percentage: Int,
    totalLogCount: Int,
    errorCount: Int,
    fatalCount: Int,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = SeverityBadgeTone.Error.containerColor
    val fatalColor = SeverityBadgeTone.Fatal.containerColor
    val errorSweep = if (totalLogCount <= 0) 0f else 360f * errorCount / totalLogCount
    val fatalSweep = if (totalLogCount <= 0) 0f else 360f * fatalCount / totalLogCount

    Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val strokeWidthPx = RingStrokeWidth.toPx()
            val inset = strokeWidthPx / 2
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(inset, inset)
            val style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = style,
            )
            if (errorSweep > 0f) {
                drawArc(
                    color = errorColor,
                    startAngle = -90f,
                    sweepAngle = errorSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
            }
            if (fatalSweep > 0f) {
                drawArc(
                    color = fatalColor,
                    startAngle = -90f + errorSweep,
                    sweepAngle = fatalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
            }
        }
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SeverityLegendColumn(
    legendItems: List<SeverityLegendItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        legendItems.forEach { item -> SeverityLegendRow(item) }
    }
}

@Composable
private fun SeverityLegendGrid(
    legendItems: List<SeverityLegendItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        legendItems.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                pair.forEach { item -> SeverityLegendRow(item, modifier = Modifier.weight(1f)) }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SeverityLegendRow(
    item: SeverityLegendItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(color = item.tone.containerColor, shape = CircleShape),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${item.count}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun densityPercentage(
    totalLogCount: Int,
    errorCount: Int,
    fatalCount: Int,
): Int {
    if (totalLogCount <= 0) return 0
    return (((errorCount + fatalCount).toFloat() / totalLogCount) * 100).roundToInt()
}

@Composable
private fun severityIndicatorDescription(
    percentage: Int,
    legendItems: List<SeverityLegendItem>,
): String {
    val prefix = stringResource(R.string.severity_indicator_description, percentage)
    if (legendItems.isEmpty()) return prefix

    val legendItemFormat = stringResource(R.string.severity_indicator_legend_item)
    val legendText =
        legendItems.joinToString(separator = ", ") { item ->
            String.format(legendItemFormat, item.label, item.count)
        }
    return "$prefix. $legendText"
}

private val PreviewLegendItems =
    listOf(
        SeverityLegendItem("ERROR", 1_256, SeverityBadgeTone.Error),
        SeverityLegendItem("FATAL", 794, SeverityBadgeTone.Fatal),
        SeverityLegendItem("WARN", 1_143, SeverityBadgeTone.Warn),
        SeverityLegendItem("INFO", 1_207, SeverityBadgeTone.Info),
        SeverityLegendItem("DEBUG", 600, SeverityBadgeTone.Debug),
    )

private val ZeroDensityLegendItems =
    listOf(
        SeverityLegendItem("ERROR", 0, SeverityBadgeTone.Error),
        SeverityLegendItem("FATAL", 0, SeverityBadgeTone.Fatal),
        SeverityLegendItem("WARN", 1_667, SeverityBadgeTone.Warn),
        SeverityLegendItem("INFO", 1_667, SeverityBadgeTone.Info),
        SeverityLegendItem("DEBUG", 1_666, SeverityBadgeTone.Debug),
    )

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun SeverityIndicatorZeroPercentPreview() {
    FGFChallengeTheme {
        SeverityIndicator(
            totalLogCount = 5_000,
            errorCount = 0,
            fatalCount = 0,
            legendItems = ZeroDensityLegendItems,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun SeverityIndicatorFortyOnePercentPreview() {
    FGFChallengeTheme {
        SeverityIndicator(
            totalLogCount = 5_000,
            errorCount = 1_039,
            fatalCount = 1_011,
            legendItems = PreviewLegendItems,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFFF8F9FAL)
@Composable
private fun SeverityIndicatorHundredPercentPreview() {
    FGFChallengeTheme {
        SeverityIndicator(
            totalLogCount = 100,
            errorCount = 60,
            fatalCount = 40,
            legendItems =
                listOf(
                    SeverityLegendItem("ERROR", 60, SeverityBadgeTone.Error),
                    SeverityLegendItem("FATAL", 40, SeverityBadgeTone.Fatal),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
