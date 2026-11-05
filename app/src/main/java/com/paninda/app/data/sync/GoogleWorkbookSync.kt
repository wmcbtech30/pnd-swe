package com.paninda.app.data.sync

import android.app.PendingIntent
import android.content.Intent
import com.paninda.app.adapters.google.*
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.ShopSnapshot
import com.paninda.app.domain.model.NetworkStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WorkbookBinding(val id: String, val name: String, val subject: String, val email: String)
data class SelectedSpreadsheet(val account: GoogleAccountInfo, val file: DriveWorkbook, val inspection: WorkbookInspection)
class WorkbookApprovalRequired(val inspection: WorkbookInspection) : IllegalStateException("Existing file needs missing Paninda worksheets")
class SyncConflictException : IllegalStateException("Local and workbook changes need review")
typealias ResolveGoogleConsent = suspend (PendingIntent) -> Intent

/** Room is always the working store. Only explicit connect/sync actions require internet. */
class GoogleWorkbookSync(private val repository: ShopRepository, private val authorization: GoogleAuthorization,
    private val network: () -> NetworkStatus = { NetworkStatus.UNKNOWN }) {
    private val mutex = Mutex()

    suspend fun selectFile(resolve: ResolveGoogleConsent): SelectedSpreadsheet = mutex.withLock {
        val account = withGoogle(null, resolve) { DriveWorkbooks(it).account() }
        val id = authorization.pickFile(account.email, resolve)
        withGoogle(account.email, resolve) { http ->
            check(DriveWorkbooks(http).account().subject == account.subject) { "Google account changed" }
            val file = DriveWorkbooks(http).selected(id)
            SelectedSpreadsheet(account, file, GoogleSheetsAdapter(http, file.id).inspect())
        }
    }

    suspend fun connect(selected: SelectedSpreadsheet, allowMissingPages: Boolean,
        resolve: ResolveGoogleConsent) = mutex.withLock {
        withGoogle(selected.account.email, resolve) { http ->
            val account = DriveWorkbooks(http).account()
            check(account.subject == selected.account.subject) { "Google account changed" }
            val file = DriveWorkbooks(http).selected(selected.file.id)
            val adapter = GoogleSheetsAdapter(http, file.id)
            val inspection = adapter.inspect() // Recheck after consent; never rely on the earlier warning snapshot.
            if (inspection.requiresWarning && !allowMissingPages) throw WorkbookApprovalRequired(inspection)
            adapter.initializePages(inspection)
            WorkbookSynchronizer(repository).sync(adapter, WorkbookBinding(file.id, file.name, account.subject, account.email))
        }
    }

    suspend fun createFile(title: String, resolve: ResolveGoogleConsent) = mutex.withLock {
        require(title.isNotBlank() && title.trim().length <= 120)
        withGoogle(null, resolve) { http ->
            val account = DriveWorkbooks(http).account()
            val local = repository.snapshot()
            val expected = SnapshotFingerprint.of(local)
            val created = GoogleSheetsAdapter.create(http, title.trim())
            val adapter = GoogleSheetsAdapter(http, created.id)
            adapter.write(local)
            val verified = adapter.readSnapshot()
            if (SnapshotFingerprint.of(verified) != expected) throw SyncVerificationException()
            repository.completeSync(WorkbookBinding(created.id, created.name, account.subject, account.email),
                verified, expected, download = false)
        }
    }

    suspend fun sync(resolve: ResolveGoogleConsent) = mutex.withLock {
        val binding = checkNotNull(repository.binding()) { "Connect a workbook first" }
        withGoogle(binding.email, resolve) { http ->
            check(DriveWorkbooks(http).account().subject == binding.subject) { "Google account changed" }
            syncWith(http, binding)
        }
    }

    private suspend fun syncWith(http: GoogleHttp, binding: WorkbookBinding) {
        val adapter = GoogleSheetsAdapter(http, binding.id)
        val inspection = adapter.inspect()
        if (inspection.canRenameLegacy) adapter.initializePages(inspection)
        else if (inspection.missingPages.isNotEmpty()) throw WorkbookFormatException("Missing Paninda worksheets")
        WorkbookSynchronizer(repository).sync(adapter, binding)
    }

    suspend fun disconnect() = mutex.withLock {
        val binding = checkNotNull(repository.binding())
        // Do not report successful revocation when Google is unreachable.
        if (network() == NetworkStatus.OFFLINE) throw OfflineException()
        authorization.revoke(binding.email)
        repository.disconnect()
    }

    private suspend fun <T> withGoogle(email: String?, resolve: ResolveGoogleConsent, action: suspend (GoogleHttp) -> T): T {
        var token: String? = null
        try {
            if (network() == NetworkStatus.OFFLINE) throw OfflineException()
            token = authorization.token(email, resolve)
            return action(GoogleHttp(token))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is GoogleApiException && e.status == 401 && token != null) {
                try { authorization.clearExpired(token) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { /* Preserve the original failure and saved local session. */ }
            }
            repository.syncStatus(SyncFailures.status(e, network()))
            throw e
        }
    }
}
