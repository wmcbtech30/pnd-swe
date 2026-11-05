package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.ui.theme.*

/** Figma 28:404, reused for a conditional missing-worksheets warning and connection errors. */
@Composable
fun ConnectionNoticeScreen(failed: Boolean, busy: Boolean = false,
    onStartOver: () -> Unit, onProceed: () -> Unit, bodyOverride: Int? = null, fileName: String? = null,
    missingPages: List<String> = emptyList()) {
    BackHandler { if (!busy) onStartOver() }
    ConnectionPage(stringResource(if (failed) R.string.connection_failed_title else R.string.connection_warning_title), actions = {
        TextButton(onStartOver, enabled = !busy, modifier = Modifier.heightIn(min = 54.dp)) {
            Text(stringResource(R.string.connection_start_over))
        }
        Button(onProceed, enabled = !busy, shape = CircleShape, modifier = Modifier.widthIn(min = 139.dp).heightIn(min = 54.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Palette.White)
            else Text(stringResource(if (failed) R.string.connection_try_again else R.string.connection_proceed))
        }
    }) {
        if (fileName != null) {
            Text(fileName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
        }
        Text(stringResource(bodyOverride ?: if (failed) R.string.connection_failed_body else R.string.connection_missing_pages_warning),
            fontFamily = AppFonts.Sans, fontSize = 20.sp, lineHeight = 24.sp)
        if (!failed && missingPages.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(missingPages.joinToString("\n"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
