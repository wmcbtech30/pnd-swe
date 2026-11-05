package com.paninda.app.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudQueue
import com.paninda.app.domain.model.SyncPhase
import com.paninda.app.domain.model.syncSummary
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.domain.model.ShopState
import com.paninda.app.ui.components.*
import com.paninda.app.ui.theme.*

@Composable
fun SyncStatusScreen(state: ShopState, onBack: () -> Unit, busy: Boolean = false, onCheck: () -> Unit) {
    val summary = state.syncSummary
    val phase = if (busy) SyncPhase.SYNCING else summary.phase
    val icon = when (phase) {
        SyncPhase.SYNCED -> Icons.Outlined.CheckCircle
        SyncPhase.PENDING, SyncPhase.SYNCING -> Icons.Outlined.Sync
        SyncPhase.READY -> Icons.Outlined.CloudQueue
        else -> Icons.Outlined.WarningAmber
    }
    PageScaffold(stringResource(R.string.sync_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(start = 30.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
            Icon(icon, syncStatusText(phase), Modifier.size(128.dp), tint = Palette.Icon)
            Spacer(Modifier.height(12.dp))
            Text(syncStatusText(phase), fontFamily = AppFonts.Serif,
                fontStyle = FontStyle.Italic, fontSize = 40.sp, lineHeight = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.sync_explanation), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(state.workbookName.orEmpty(), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(state.accountEmail.orEmpty(), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(lastSyncText(summary),
                fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(pluralStringResource(R.plurals.sync_pending_count, state.pendingCount, state.pendingCount),
                fontFamily = AppFonts.Condensed, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.sync_tools))
            Spacer(Modifier.height(8.dp))
            MenuRow(stringResource(R.string.sync_now), Icons.Outlined.Sync, !busy, onCheck)
        }
    }
}
