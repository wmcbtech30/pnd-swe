package com.paninda.app

import androidx.room.Room
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShopRepositoryTest {
    private lateinit var db: PanindaDatabase
    private lateinit var repository: ShopRepository
    private val draft = ProductDraft("Notebook", "TEST-001", "Test Vendor", "Stationery", "9.50", "12", "3")

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PanindaDatabase::class.java).build()
        repository = ShopRepository(db)
    }
    @After fun close() { db.close() }

    @Test fun startupDisconnectAndResetNeverCreateRecords() = runBlocking {
        repository.initialize()
        TestFixtures.connect(repository)
        assertEquals(ShopSnapshot(), repository.snapshot())
        assertTrue(db.shopDao().pending().isEmpty())
        repository.saveProduct(null, draft)
        val lastSuccess = repository.state.first().lastCheckedAt
        val binding = repository.binding()
        for (failure in listOf("offline", "unreachable", "access", "failed")) {
            repository.syncStatus(failure)
            val state = repository.state.first()
            assertTrue(state.connected)
            assertEquals(lastSuccess, state.lastCheckedAt)
            assertEquals(binding, repository.binding())
            assertEquals(3, state.pendingCount)
            assertEquals(1, state.products.size)
        }
        repository.disconnect()
        assertFalse(repository.state.first().connected)
        assertEquals(1, repository.snapshot().products.size)
        TestFixtures.connect(repository)
        repository.resetLocalData()
        repository.initialize()
        assertTrue(repository.state.first().connected)
        assertEquals(ShopSnapshot(), repository.snapshot())
        assertTrue(db.shopDao().pending().isEmpty())
    }

    @Test fun validatedProductChangesAreAtomicAndKeepReceiptSnapshots() = runBlocking {
        repository.initialize()
        assertEquals(SaveProductResult.Invalid, repository.saveProduct(null, draft.copy(price = "1.001")))
        assertEquals(ShopSnapshot(), repository.snapshot())
        assertTrue(repository.saveProduct(null, draft) is SaveProductResult.Saved)
        val original = repository.snapshot().products.single()
        assertEquals(950L, original.unitPriceMinor)
        assertEquals(12, original.quantity)
        assertEquals(3, db.shopDao().pending().size)
        assertEquals(SaveProductResult.DuplicateSku,
            repository.saveProduct(null, draft.copy(sku = "test-001", vendorName = "Another vendor")))
        assertEquals(1, repository.snapshot().vendors.size)
        TestFixtures.receipt(db, original, "test-sale", TestFixtures.now)
        repository.saveProduct(original.id, draft.copy(name = "Renamed", price = "25.00", initialQuantity = "999"))
        val snapshot = repository.snapshot()
        assertEquals(12, snapshot.products.single().quantity)
        assertEquals(2500L, snapshot.products.single().unitPriceMinor)
        assertEquals("Original product name", snapshot.items.single().productNameSnapshot)
        assertEquals(5000L, snapshot.items.single().unitPriceMinor)
        assertEquals(10000L, snapshot.transactions.single().totalMinor)
        assertEquals(1, repository.movements(original.id).size)
        assertEquals(3, db.shopDao().pending().size)
    }

    @Test fun fileBackedDatabaseSurvivesReopening() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, PanindaDatabase::class.java, name).build()
        try {
            val repo = ShopRepository(first)
            repo.initialize()
            TestFixtures.connect(repo)
            repo.saveProduct(null, draft)
        } finally { first.close() }
        val second = Room.databaseBuilder(context, PanindaDatabase::class.java, name).build()
        try {
            val repo = ShopRepository(second)
            repo.initialize()
            assertEquals(12, repo.snapshot().products.single().quantity)
            assertTrue(repo.state.first().connected)
        } finally { second.close(); context.deleteDatabase(name) }
    }
}
