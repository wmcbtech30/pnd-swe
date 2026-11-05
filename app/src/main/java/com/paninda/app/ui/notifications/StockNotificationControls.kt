package com.paninda.app.ui.notifications

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paninda.app.PanindaApplication
import com.paninda.app.R
import com.paninda.app.data.notifications.StockNotificationStatus
import com.paninda.app.data.notifications.StockNotificationPublisher
import com.paninda.app.domain.model.*
import com.paninda.app.ui.components.SectionLabel
import com.paninda.app.ui.theme.Palette

data class StockNotificationControls(val status: StockNotificationStatus, val onToggle: (Boolean) -> Unit, val onSystemSettings: () -> Unit)

@Composable
fun rememberStockNotificationControls(state: ShopState): StockNotificationControls {
    val context = LocalContext.current
    val controller = (context.applicationContext as PanindaApplication).stockNotifications
    val status by controller.status.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { controller.refresh() }
    val systemSettings = {
        val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).putExtra(Settings.EXTRA_CHANNEL_ID, StockNotificationPublisher.CHANNEL)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
        context.startActivity(intent)
    }
    val enable = {
        controller.setEnabled(true)
        val needsRuntime = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val canRequest = !status.requested || (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
        controller.markPrompted()
        if (needsRuntime && canRequest) {
            controller.markPermissionRequested()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        else if (!controller.status.value.permitted) systemSettings()
    }
    if (Build.VERSION.SDK_INT >= 33 && state.connected && status.enabled && !status.permitted && !status.prompted &&
        state.products.any { it.stockAlertLevel() != StockAlertLevel.HEALTHY }) {
        val skip = { controller.markPrompted(); controller.setEnabled(false) }
        AlertDialog(onDismissRequest = skip, containerColor = Palette.White,
            title = { Text(stringResource(R.string.alert_permission_title)) },
            text = { Text(stringResource(R.string.alert_permission_body)) },
            confirmButton = { TextButton(enable) { Text(stringResource(R.string.alert_enable)) } },
            dismissButton = { TextButton(skip) { Text(stringResource(R.string.alert_not_now)) } })
    }
    return StockNotificationControls(status, { enabled -> if (enabled) enable() else controller.setEnabled(false) }, systemSettings)
}

@Composable
fun StockNotificationSettings(controls: StockNotificationControls) {
    SectionLabel(stringResource(R.string.alert_settings_heading))
    Spacer(Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Palette.Card)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(R.string.alert_settings_toggle), Modifier.weight(1f))
                Switch(controls.status.enabled, controls.onToggle)
            }
            Text(stringResource(if (!controls.status.enabled) R.string.alert_settings_off
                else if (!controls.status.permitted) R.string.alert_settings_blocked else R.string.alert_settings_on),
                style = MaterialTheme.typography.bodySmall)
            if (controls.status.enabled && !controls.status.permitted) TextButton({ controls.onToggle(true) }) { Text(stringResource(R.string.alert_enable)) }
            TextButton(controls.onSystemSettings) { Text(stringResource(R.string.alert_android_settings)) }
        }
    }
}
