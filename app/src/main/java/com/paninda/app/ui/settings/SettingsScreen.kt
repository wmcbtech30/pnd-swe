package com.paninda.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.Palette
import com.paninda.app.domain.model.ShopState
import com.paninda.app.domain.model.syncSummary
import com.paninda.app.domain.model.SyncPhase
import com.paninda.app.ui.sync.syncStatusText

@Composable
fun SettingsScreen(state: ShopState, busy: Boolean, onBack: () -> Unit, onSync: () -> Unit,
    onDisconnect: () -> Unit, onReset: () -> Unit, notificationContent: @Composable () -> Unit = {}) {
    var dialog by rememberSaveable { mutableStateOf("") }
    PageScaffold(stringResource(R.string.settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(start = 26.dp, end = 22.dp, top = 12.dp, bottom = 24.dp)) {
            SectionLabel(stringResource(R.string.settings_connection))
            Spacer(Modifier.height(8.dp))
            Text(state.workbookName.orEmpty())
            Text(state.accountEmail.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            Text(syncStatusText(if (busy && state.syncSummary.phase != SyncPhase.OFFLINE) SyncPhase.SYNCING else state.syncSummary.phase), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(if (busy) R.string.settings_sync_busy else R.string.settings_sync), Icons.Outlined.Sync, !busy, onSync)
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_conflicts))
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = if (state.syncStatus in listOf("conflict", "incompatible"))
                Palette.ProductWarning else Palette.Card)) {
                Text(stringResource(if (state.syncStatus == "conflict") R.string.settings_conflict_detected
                    else if (state.syncStatus == "incompatible") R.string.settings_contract_detected
                    else R.string.settings_conflict_guidance), Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            notificationContent()
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_manage))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.settings_disconnect), Icons.Outlined.LinkOff, !busy) { dialog = "disconnect" }
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.settings_reset), Icons.Outlined.DeleteOutline, !busy) { dialog = "reset" }
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
