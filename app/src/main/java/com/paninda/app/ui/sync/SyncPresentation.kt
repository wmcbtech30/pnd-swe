package com.paninda.app.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.paninda.app.R
import com.paninda.app.domain.model.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun lastSyncText(summary: SyncSummary): String = summary.lastSuccessfulAt?.let {
    val time = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault())
        .withZone(ShopZone).format(Instant.ofEpochMilli(it))
    stringResource(R.string.sync_checked, time)
} ?: stringResource(R.string.sync_never)

@Composable
fun syncStatusText(phase: SyncPhase): String = stringResource(when (phase) {
    SyncPhase.SYNCED -> R.string.sync_synced
    SyncPhase.PENDING -> R.string.sync_pending
    SyncPhase.CONFLICT -> R.string.sync_conflict
    SyncPhase.INCOMPATIBLE -> R.string.sync_incompatible
    SyncPhase.ACCESS -> R.string.sync_access
    SyncPhase.FAILED -> R.string.sync_failed
    SyncPhase.SYNCING -> R.string.sync_working
    SyncPhase.READY -> R.string.sync_ready
})
