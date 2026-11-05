package com.paninda.app

import androidx.room.Room
import com.paninda.app.adapters.*
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.data.sync.*
import com.paninda.app.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/** Three data-loss regression checks; no Google account, network, or layout assertions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkbookSynchronizerTest {
    private lateinit var db: PanindaDatabase
    private lateinit var repo: ShopRepository
    private val binding = WorkbookBinding("test-book", "Test workbook", "test-subject", "test@example.invalid")
    private val draft = ProductDraft("Notebook", "TEST-001", "Test Vendor", "Stationery", "9.50", "12", "3")
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PanindaDatabase::class.java).build()
        repo = ShopRepository(db)
    }
    @After fun close() = db.close()

    private class Remote(var snapshot: ShopSnapshot) : WorkbookAdapter {
        var writes = 0
        var onWrite: suspend () -> Unit = {}
        var onRead: suspend () -> Unit = {}
        override suspend fun read(): AdapterResult<ShopSnapshot> { onRead(); return AdapterResult.Available(snapshot) }
        override suspend fun write(snapshot: ShopSnapshot): AdapterResult<Unit> {
            writes++; this.snapshot = snapshot; onWrite(); return AdapterResult.Available(Unit)
        }
    }
    private suspend fun baseline(): Remote {
        repo.initialize()
        repo.saveProduct(null, draft)
        val remote = Remote(repo.snapshot())
        repo.completeSync(binding, remote.snapshot, SnapshotFingerprint.of(remote.snapshot), false)
        return remote
    }
    private suspend fun expectConflict(block: suspend () -> Unit) {
        try { block(); fail("Expected a conflict") } catch (_: SyncConflictException) { }
    }

    @Test fun divergentCopiesAndSecondReadChangesNeverOverwrite() = runBlocking {
        val remote = baseline()
        val product = repo.snapshot().products.single()
        repo.saveProduct(product.id, draft.copy(name = "Local rename"))
        remote.snapshot = remote.snapshot.copy(products = remote.snapshot.products.map { it.copy(name = "Remote rename") })
        expectConflict { WorkbookSynchronizer(repo).sync(remote, binding) }
        assertEquals(0, remote.writes)
        assertEquals("Local rename", repo.snapshot().products.single().name)
        assertTrue(db.shopDao().pending().isNotEmpty())
        remote.snapshot = remote.snapshot.copy(products = listOf(product))
        var reads = 0
        remote.onRead = { if (++reads == 2) remote.snapshot = remote.snapshot.copy(products = listOf(product.copy(name = "Edited during sync"))) }
        expectConflict { WorkbookSynchronizer(repo).sync(remote, binding) }
        assertEquals(0, remote.writes)
    }

    @Test fun editsDuringUploadStayPendingAndAmbiguousFailureCanRetry() = runBlocking {
        val remote = baseline()
        val id = repo.snapshot().products.single().id
        repo.saveProduct(id, draft.copy(name = "First edit"))
        remote.onWrite = { repo.saveProduct(id, draft.copy(name = "Second edit")) }
        WorkbookSynchronizer(repo).sync(remote, binding)
        assertEquals("First edit", remote.snapshot.products.single().name)
        assertEquals("Second edit", repo.snapshot().products.single().name)
        assertTrue(db.shopDao().pending().isNotEmpty())
        remote.onWrite = { throw IOException("Response lost after upload") }
        try { WorkbookSynchronizer(repo).sync(remote, binding); fail("Expected transport failure") } catch (_: IOException) { }
        assertTrue(db.shopDao().pending().isNotEmpty())
        remote.onWrite = {}
        WorkbookSynchronizer(repo).sync(remote, binding)
        assertTrue(db.shopDao().pending().isEmpty())
        assertEquals("Second edit", remote.snapshot.products.single().name)
    }

    @Test fun resetRestoresRemoteAndImportRejectsNewLocalEdits() = runBlocking {
        val remote = baseline()
        repo.resetLocalData()
        WorkbookSynchronizer(repo).sync(remote, binding)
        assertEquals(0, remote.writes)
        assertEquals(12, repo.snapshot().products.single().quantity)
        repo.resetLocalData()
        remote.onRead = { repo.saveProduct(null, draft.copy(sku = "NEW-LOCAL")) }
        expectConflict { WorkbookSynchronizer(repo).sync(remote, binding) }
        assertEquals("NEW-LOCAL", repo.snapshot().products.single().sku)
        assertTrue(db.shopDao().pending().isNotEmpty())
        assertEquals(0, remote.writes)
    }
}
