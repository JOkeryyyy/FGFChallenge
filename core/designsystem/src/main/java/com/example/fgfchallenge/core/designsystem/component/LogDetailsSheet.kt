package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.model.LogDetailsUi
import com.example.fgfchallenge.core.designsystem.model.SeverityBadgeTone
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme
import com.example.fgfchallenge.core.designsystem.token.Spacing

/**
 * Fixed label column so every label starts at the same x and the value column absorbs the rest of
 * the width; long values (log ID, session ID, full UTC timestamp) wrap inside their own column.
 */
private val labelColumnWidth = 116.dp

/**
 * Read-only structured log details. Close, swipe-down, and Back dismissal all come from
 * Material 3's own [ModalBottomSheet] behavior; this component only forwards [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailsSheet(
    details: LogDetailsUi,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        LogDetailsSheetContent(details = details, onDismissRequest = onDismissRequest)
    }
}

/**
 * The sheet's content, split out from [LogDetailsSheet] so it can be previewed on its own.
 * [ModalBottomSheet] hosts its content in a separate window whose entrance animation never
 * settles in a static preview render, leaving the sheet preview blank — previewing this
 * content composable directly inside a [Surface] avoids that.
 */
@Composable
private fun LogDetailsSheetContent(
    details: LogDetailsUi,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                // Long values and large font scales can push the property list past the sheet's
                // height; ModalBottomSheet nested-scrolls with a scrollable child, so drag-to-
                // dismiss still works.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.log_details_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(onClick = onDismissRequest) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.log_details_close_action),
                )
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            DetailLabel(stringResource(R.string.log_details_label_severity))
            SeverityBadge(label = details.severityLabel, tone = details.severityTone)
        }
        DetailRow(stringResource(R.string.log_details_label_message), details.message)
        DetailRow(stringResource(R.string.log_details_label_tag), details.tag)
        DetailRow(stringResource(R.string.log_details_label_timestamp), details.timestampUtc)
        DetailRow(stringResource(R.string.log_details_label_latency), details.latency)
        DetailRow(stringResource(R.string.log_details_label_ai_generated), details.aiGenerated)
        DetailRow(stringResource(R.string.log_details_label_log_id), details.logId)
        DetailRow(stringResource(R.string.log_details_label_session), details.sessionId)
    }
}

/** Top alignment keeps the label level with the first line of a value that wraps. */
@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        DetailLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(labelColumnWidth),
    )
}

@Preview
@Composable
private fun LogDetailsSheetPreview() {
    FGFChallengeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            LogDetailsSheetContent(
                details =
                    LogDetailsUi(
                        severityLabel = "ERROR",
                        severityTone = SeverityBadgeTone.Error,
                        message = "Connection timed out",
                        tag = "network",
                        timestampUtc = "2025-05-22T17:11:58.123Z",
                        latency = "3,245 ms",
                        aiGenerated = "No",
                        logId = "a1b2c3d4-e5f6-7890-abcd-1234567890ef",
                        sessionId = "sess-7f3a9b21-7cd4-4d6d-9a12-3f5e7d9a1b2c3f5e7d9a1b2c",
                    ),
                onDismissRequest = {},
            )
        }
    }
}
