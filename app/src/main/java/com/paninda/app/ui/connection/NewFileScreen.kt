package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R

@Composable
fun NewFileScreen(busy: Boolean, onBack: () -> Unit, onCreate: (String) -> Unit) {
    val defaultName = stringResource(R.string.new_file_default_name)
    var name by rememberSaveable { mutableStateOf(defaultName) }
    BackHandler { if (!busy) onBack() }
    ConnectionPage(stringResource(R.string.new_file_title), actions = {
        TextButton(onBack, enabled = !busy) { Text(stringResource(R.string.common_cancel)) }
        Button({ onCreate(name.trim()) }, enabled = !busy && name.isNotBlank() && name.trim().length <= 120,
            modifier = Modifier.heightIn(min = 54.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.new_file_create))
        }
    }) {
        Text(stringResource(R.string.new_file_body))
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it.take(120) }, enabled = !busy, singleLine = true,
            label = { Text(stringResource(R.string.new_file_name)) }, modifier = Modifier.fillMaxWidth())
    }
}
