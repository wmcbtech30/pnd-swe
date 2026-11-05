package com.paninda.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette

@Composable
fun SettingsScreen(onBack: () -> Unit, onDisconnect: () -> Unit, onReset: () -> Unit) {
    var dialog by rememberSaveable { mutableStateOf("") }
    PageScaffold(stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(start = 26.dp, end = 22.dp, top = 12.dp, bottom = 24.dp)) {
            SectionLabel(stringResource(R.string.settings_manage))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.settings_disconnect)) { dialog = "disconnect" }
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.settings_reset)) { dialog = "reset" }
        }
    }
    if (dialog.isNotEmpty()) {
        val reset = dialog == "reset"
        AlertDialog(onDismissRequest = { dialog = "" }, containerColor = Palette.White,
            title = { Text(stringResource(if (reset) R.string.settings_reset_title else R.string.settings_disconnect_title)) },
            text = { Text(stringResource(if (reset) R.string.settings_reset_body else R.string.settings_disconnect_body)) },
            confirmButton = {
                TextButton({
                    dialog = ""
                    if (reset) onReset() else onDisconnect()
                }) { Text(stringResource(if (reset) R.string.settings_reset_confirm else R.string.settings_disconnect_confirm)) }
            },
            dismissButton = { TextButton({ dialog = "" }) { Text(stringResource(R.string.common_cancel)) } })
    }
}
