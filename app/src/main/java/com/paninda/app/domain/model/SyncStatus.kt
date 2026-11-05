package com.paninda.app.domain.model

enum class SyncPhase { READY, SYNCED, PENDING, CONFLICT, INCOMPATIBLE, ACCESS, FAILED, SYNCING, OFFLINE, UNREACHABLE }

data class SyncSummary(val phase: SyncPhase, val lastSuccessfulAt: Long?, val pendingCount: Int)

/** Shared read-only status for Home, Settings and Sync. Attempts/failures never invent a success time. */
val ShopState.syncSummary: SyncSummary
    get() = SyncSummary(if (networkStatus == NetworkStatus.OFFLINE) SyncPhase.OFFLINE else when (syncStatus) {
        "synced" -> when {
            pendingCount > 0 -> SyncPhase.PENDING
            lastCheckedAt != null -> SyncPhase.SYNCED
            else -> SyncPhase.READY
        }
        "pending" -> SyncPhase.PENDING
        "conflict" -> SyncPhase.CONFLICT
        "incompatible" -> SyncPhase.INCOMPATIBLE
        "access" -> SyncPhase.ACCESS
        "failed" -> SyncPhase.FAILED
        "unreachable" -> SyncPhase.UNREACHABLE
        "offline" -> if (networkStatus == NetworkStatus.ONLINE) {
            if (pendingCount > 0) SyncPhase.PENDING else SyncPhase.READY
        } else SyncPhase.OFFLINE
        else -> SyncPhase.READY
    }, lastCheckedAt, pendingCount)
