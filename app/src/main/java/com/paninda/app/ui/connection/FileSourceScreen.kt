package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R

@Composable
fun FileSourceScreen(onBack: () -> Unit, onExisting: () -> Unit, onNew: () -> Unit) {
    BackHandler(onBack = onBack)
    ConnectionPage(stringResource(R.string.file_source_title), actions = {
        TextButton(onBack) { Text(stringResource(R.string.connection_start_over)) }
    }) {
        Text(stringResource(R.string.file_source_body))
        Spacer(Modifier.height(24.dp))
        Button(onExisting, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Icon(Icons.Outlined.FolderOpen, null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.file_source_existing))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onNew, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
            Icon(Icons.Outlined.Add, null)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.file_source_new))
        }
    }
}
