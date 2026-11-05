package com.paninda.app.data.sync

enum class SyncDecision { UPLOAD, DOWNLOAD, UNCHANGED, CONFLICT }
object SyncPolicy {
    fun decide(base: String?, local: String, remote: String, localEmpty: Boolean): SyncDecision = when {
        local == remote -> SyncDecision.UNCHANGED
        base == null -> if (localEmpty) SyncDecision.DOWNLOAD else SyncDecision.CONFLICT
        local == base -> SyncDecision.DOWNLOAD
        remote == base -> SyncDecision.UPLOAD
        else -> SyncDecision.CONFLICT
    }
}
