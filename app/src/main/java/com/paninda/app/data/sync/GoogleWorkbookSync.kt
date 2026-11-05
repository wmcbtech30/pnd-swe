package com.paninda.app.data.sync

import android.app.PendingIntent
import android.content.Intent
import com.paninda.app.adapters.google.*
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.ShopSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WorkbookBinding(val id: String, val name: String, val subject: String, val email: String)
class SyncConflictException : IllegalStateException("Local and workbook changes need review")
typealias ResolveGoogleConsent = suspend (PendingIntent) -> Intent

/** Room is always the working store. Only explicit connect/sync actions require internet. */
class GoogleWorkbookSync(private val repository: ShopRepository, private val authorization: GoogleAuthorization) {
    private val mutex = Mutex()

    suspend fun discover(resolve: ResolveGoogleConsent): Pair<GoogleAccountInfo, List<DriveWorkbook>> = mutex.withLock {
        withGoogle(null, resolve) { http -> DriveWorkbooks(http).let { it.account() to it.list() } }
    }

    suspend fun connect(account: GoogleAccountInfo, workbook: DriveWorkbook?, title: String,
        resolve: ResolveGoogleConsent) = mutex.withLock {
        withGoogle(account.email, resolve) { http ->
            val actual = DriveWorkbooks(http).account()
            check(actual.subject == account.subject) { "Google account changed; start again" }
            if (workbook == null) {
                val local = repository.snapshot()
                val expected = SnapshotFingerprint.of(local)
                val created = GoogleSheetsAdapter.create(http, title)
                // Mark after data is fully written: incomplete creation is never advertised as reconnectable.
                val adapter = GoogleSheetsAdapter(http, created.id)
                adapter.write(local)
                val verified = adapter.readSnapshot()
                if (SnapshotFingerprint.of(verified) != expected) throw SyncVerificationException()
                DriveWorkbooks(http).mark(created.id)
                repository.completeSync(WorkbookBinding(created.id, created.name, actual.subject, actual.email),
                    verified, expected, download = false)
            } else {
                // Only IDs freshly listed for this account may be adopted; no arbitrary URL/ID import.
                check(DriveWorkbooks(http).list().any { it.id == workbook.id }) { "Workbook is no longer accessible" }
                syncWith(http, WorkbookBinding(workbook.id, workbook.name, actual.subject, actual.email))
            }
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
        WorkbookSynchronizer(repository).sync(GoogleSheetsAdapter(http, binding.id), binding)
    }

    suspend fun disconnect() = mutex.withLock {
        val binding = checkNotNull(repository.binding())
        // Do not report successful revocation when Google is unreachable.
        authorization.revoke(binding.email)
        repository.disconnect()
    }

    private suspend fun <T> withGoogle(email: String?, resolve: ResolveGoogleConsent, action: suspend (GoogleHttp) -> T): T {
        var token: String? = null
        try {
            token = authorization.token(email, resolve)
            return action(GoogleHttp(token))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (e is GoogleApiException && e.status == 401 && token != null) authorization.clearExpired(token)
            repository.syncStatus(when {
                e is SyncConflictException -> "conflict"
                e is WorkbookFormatException -> "incompatible"
                e is GoogleApiException && e.status in listOf(401, 403, 404) -> "access"
                else -> "failed"
            })
            throw e
        }
    }
}
