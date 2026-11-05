package com.paninda.app.data.sync

import com.paninda.app.adapters.*
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.ShopSnapshot

class SyncVerificationException : IllegalStateException("Upload could not be verified")

/** Provider-neutral conflict policy, independent of OAuth and HTTP. */
class WorkbookSynchronizer(private val repository: ShopRepository) {
    suspend fun sync(adapter: WorkbookAdapter, binding: WorkbookBinding) {
        suspend fun read() = when (val result = adapter.read()) {
            is AdapterResult.Available -> result.value
            AdapterResult.Unavailable -> error("Workbook adapter is unavailable")
        }
        val local = repository.snapshot()
        val localHash = SnapshotFingerprint.of(local)
        val remote = read()
        val remoteHash = SnapshotFingerprint.of(remote)
        when (SyncPolicy.decide(repository.baseFor(binding), localHash, remoteHash, local == ShopSnapshot(), remote == ShopSnapshot())) {
            SyncDecision.CONFLICT -> throw SyncConflictException()
            SyncDecision.DOWNLOAD -> repository.completeSync(binding, remote, localHash, download = true)
            SyncDecision.UNCHANGED -> repository.completeSync(binding, remote, localHash, download = false)
            SyncDecision.UPLOAD -> {
                // Sheets has no conditional-write/CAS API. A second read narrows, but cannot eliminate,
                // the concurrent-editor window. Multi-device writing requires a coordinator.
                if (SnapshotFingerprint.of(read()) != remoteHash) throw SyncConflictException()
                check(adapter.write(local) is AdapterResult.Available)
                val verified = read()
                if (SnapshotFingerprint.of(verified) != localHash) throw SyncVerificationException()
                repository.completeSync(binding, verified, localHash, download = false)
            }
        }
    }
}
