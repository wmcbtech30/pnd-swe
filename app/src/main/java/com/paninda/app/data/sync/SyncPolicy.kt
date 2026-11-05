package com.paninda.app.data.sync

enum class SyncDecision { UPLOAD, DOWNLOAD, UNCHANGED, CONFLICT }
object SyncPolicy {
    fun decide(base: String?, local: String, remote: String, localEmpty: Boolean, remoteEmpty: Boolean = false): SyncDecision = when {
        local == remote -> SyncDecision.UNCHANGED
        base == null -> when {
            localEmpty -> SyncDecision.DOWNLOAD
            remoteEmpty -> SyncDecision.UPLOAD
            else -> SyncDecision.CONFLICT
        }
        local == base -> SyncDecision.DOWNLOAD
        remote == base -> SyncDecision.UPLOAD
        else -> SyncDecision.CONFLICT
    }
}
