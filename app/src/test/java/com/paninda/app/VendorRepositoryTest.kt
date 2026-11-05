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
class VendorRepositoryTest {
    private lateinit var db: PanindaDatabase
    private lateinit var repo: ShopRepository
    private val vendor = VendorDraft("Campus Craft", "Ana", "09123456789", "ana@example.invalid", "Malolos", "Weekly delivery")
    private val product = ProductDraft("Notebook", "VENDOR-001", "campus craft", "Stationery", "9.50", "12", "3")
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PanindaDatabase::class.java).build()
        repo = ShopRepository(db)
    }
    @After fun close() = db.close()

    @Test fun vendorDetailsValidateAndRenameWithoutChangingProductOrReceiptOwnership() = runBlocking {
        repo.initialize()
        assertEquals(SaveVendorResult.Invalid, repo.saveVendor(null, vendor.copy(email = "bad email")))
        val id = (repo.saveVendor(null, vendor) as SaveVendorResult.Saved).id
        assertEquals(SaveVendorResult.DuplicateName, repo.saveVendor(null, vendor.copy(name = " campus CRAFT ")))
        assertTrue(repo.saveProduct(null, product) is SaveProductResult.Saved)
        val original = repo.snapshot().products.single()
        TestFixtures.receipt(db, original, "vendor-history", TestFixtures.now)
        repo.saveVendor(id, vendor.copy(name = "Campus Craft Updated", notes = "Updated contact instructions"))
        val state = repo.state.first()
        assertEquals(id, state.vendors.single().id)
        assertEquals("ana@example.invalid", state.vendors.single().email)
        assertEquals(2L, state.vendors.single().version)
        assertEquals("Campus Craft Updated", state.products.single().vendorName)
        assertEquals(12, state.products.single().quantity)
        assertEquals(id, repo.snapshot().items.single().vendorIdSnapshot)
        assertEquals("Original product name", repo.snapshot().items.single().productNameSnapshot)
        assertEquals(1, db.shopDao().pending().count { it.entityType == "vendor" })
        assertEquals(1, repo.snapshot().vendors.size)
    }

    @Test fun archivePreservesExistingLinksBlocksNewAssignmentsAndCanBeRestored() = runBlocking {
        repo.initialize()
        val id = (repo.saveVendor(null, vendor) as SaveVendorResult.Saved).id
        val linked = product.copy(vendorId = id)
        repo.saveProduct(null, linked)
        val original = repo.snapshot().products.single()
        repo.setVendorActive(id, false)
        assertFalse(repo.snapshot().vendors.single().isActive)
        assertEquals(SaveProductResult.VendorUnavailable, repo.saveProduct(null, linked.copy(sku = "NEW-ARCHIVED")))
        assertEquals(SaveProductResult.VendorUnavailable, repo.saveProduct(null, product.copy(sku = "NAME-ARCHIVED")))
        assertTrue(repo.saveProduct(original.id, linked.copy(name = "Renamed notebook")) is SaveProductResult.Saved)
        assertEquals(12, repo.snapshot().products.single().quantity)
        assertEquals(SaveVendorResult.DuplicateName, repo.saveVendor(null, vendor))
        repo.setVendorActive(id, true)
        assertTrue(repo.saveProduct(null, linked.copy(sku = "RESTORED-001")) is SaveProductResult.Saved)
        assertEquals(2, repo.snapshot().products.size)
        assertEquals(1, repo.snapshot().vendors.size)
        assertTrue(VendorFilter.ACTIVE.matches(repo.snapshot().vendors.single(), "Ana"))
    }
}
