package com.example.fgfchallenge.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.fgfchallenge.core.designsystem.R
import com.example.fgfchallenge.core.designsystem.theme.FGFChallengeTheme

/**
 * Shows a Material 3 [AlertDialog] and forwards [onRetry]/[onDismiss]. It does not decide what
 * happens after either callback fires — Retry, Dismiss, Back, and outside-tap all just call back
 * into the caller, which owns the resulting feature state.
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.error_dialog_retry_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.error_dialog_dismiss_action))
            }
        },
    )
}

@Preview
@Composable
private fun ErrorDialogPreview() {
    FGFChallengeTheme {
        ErrorDialog(
            title = "Unable to load logs",
            message = "We couldn't fetch logs from the server.",
            onRetry = {},
            onDismiss = {},
        )
    }
}
