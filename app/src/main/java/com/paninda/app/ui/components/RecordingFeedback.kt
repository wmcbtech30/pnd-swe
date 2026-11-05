package com.paninda.app.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paninda.app.R
import com.paninda.app.domain.model.RecordResult
import com.paninda.app.ui.theme.Palette

internal fun recordingError(result: RecordResult): Int = when (result) {
    RecordResult.Invalid -> R.string.recording_invalid
    RecordResult.Unavailable -> R.string.recording_unavailable
    RecordResult.InsufficientStock -> R.string.recording_stock
    RecordResult.StaleCount -> R.string.recording_stale
    else -> R.string.common_action_failed
}

@Composable
internal fun DiscardRecordingDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Palette.White,
        title = { Text(stringResource(R.string.product_editor_unsaved_title)) },
        text = { Text(stringResource(R.string.product_editor_unsaved_body)) },
        confirmButton = { TextButton(onDiscard) { Text(stringResource(R.string.product_editor_discard)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.product_editor_keep)) } })
}
