package com.paninda.app.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
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
    PageScaffold(stringResource(R.string.sync_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(start = 30.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)) {
            Icon(Icons.Outlined.WarningAmber, null, Modifier.size(128.dp), tint = Palette.Icon)
            Spacer(Modifier.height(12.dp))
            Text(if (busy) stringResource(R.string.sync_working) else syncStatusText(state.syncStatus), fontFamily = AppFonts.Serif,
                fontStyle = FontStyle.Italic, fontSize = 40.sp, lineHeight = 60.sp)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.sync_explanation), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(state.workbookName.orEmpty(), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(state.accountEmail.orEmpty(), fontFamily = AppFonts.Condensed, fontSize = 20.sp)
            Text(if (state.lastCheckedAt == null) stringResource(R.string.sync_never)
                else stringResource(R.string.sync_checked, timeLabel(state.lastCheckedAt)),
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

@Composable
fun syncStatusText(status: String): String = stringResource(when (status) {
    "synced" -> R.string.sync_synced
    "pending" -> R.string.sync_pending
    "conflict" -> R.string.sync_conflict
    "incompatible" -> R.string.sync_incompatible
    "access" -> R.string.sync_access
    "failed" -> R.string.sync_failed
    else -> R.string.sync_ready
})
