package com.paninda.app

import androidx.room.Room
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.local.entities.toEntity
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
import java.time.*
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivingRepositoryTest {
    private lateinit var db: PanindaDatabase
    private lateinit var repo: ShopRepository
    private val clock = Clock.fixed(Instant.parse("2026-11-05T16:05:00Z"), ZoneOffset.UTC)
    @Before fun setup() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PanindaDatabase::class.java).build()
        repo = ShopRepository(db, clock)
        repo.initialize()
        repo.saveProduct(null, ProductDraft("Notebook", "RECEIVE-001", "Vendor", "Stationery", "9.50", "2", "3"))
    }
    @After fun close() = db.close()
    private fun id() = UUID.randomUUID().toString()
    private fun draft(product: Product) = ReceivingDraft(id(), product.vendorId, "2026-11-06", " Recorder ", " DELIVERY-001 ", " Checked ", listOf(ReceivingLineDraft(product.id, "5")))

    @Test fun deliveriesValidateAtomicallyDeduplicateAndInvalidateOpenCounts() = runBlocking {
        val original = repo.snapshot().products.single()
        val draft = draft(original)
        val initial = repo.snapshot()
        listOf(draft.copy(receivedDate = "2026-11-07"), draft.copy(receivedDate = "invalid"),
            draft.copy(lines = listOf(ReceivingLineDraft(original.id, "0"))), draft.copy(lines = draft.lines + draft.lines),
            draft.copy(receivedBy = " ")).forEach { assertEquals(RecordResult.Invalid, repo.recordReceiving(it)) }
        assertEquals(RecordResult.Unavailable, repo.recordReceiving(draft.copy(lines = draft.lines + ReceivingLineDraft("missing", "1"))))
        assertEquals(initial, repo.snapshot())
        val other = (repo.saveVendor(null, VendorDraft("Other Vendor")) as SaveVendorResult.Saved).id
        assertEquals(RecordResult.Unavailable, repo.recordReceiving(draft.copy(vendorId = other)))
        repo.setVendorActive(original.vendorId, false)
        assertEquals(RecordResult.Unavailable, repo.recordReceiving(draft))
        repo.setVendorActive(original.vendorId, true)
        assertEquals(RecordResult.Saved(draft.id), repo.recordReceiving(draft))
        assertEquals(RecordResult.Saved(draft.id), repo.recordReceiving(draft))
        val saved = repo.snapshot()
        assertEquals(7, saved.products.single().quantity)
        assertEquals(original.unitPriceMinor, saved.products.single().unitPriceMinor)
        assertEquals(original.version + 1, saved.products.single().version)
        assertEquals(Instant.parse("2026-11-05T16:00:00Z").toEpochMilli(), saved.deliveries.single().receivedAt)
        assertEquals(clock.millis(), saved.deliveries.single().recordedAt)
        assertEquals("Recorder", saved.deliveries.single().receivedBy)
        assertEquals("DELIVERY-001", saved.deliveries.single().reference)
        assertEquals(5, saved.movements.single { it.movementType == "RECEIVING" }.quantityDelta)
        assertEquals(1, db.shopDao().pending().count { it.entityType == "stock_delivery" })
        assertEquals(RecordResult.StaleCount, repo.recordCount(CountDraft(id(), "Recorder", "", listOf(CountEntryDraft(original.id, original.quantity, original.version, "2")))))
        db.shopDao().putMovements(listOf(StockMovement(id(), original.id, Int.MAX_VALUE - 7, "ADJUSTMENT", "overflow-fixture", clock.millis(), "Large stock fixture").toEntity()))
        val beforeOverflow = repo.snapshot()
        assertEquals(RecordResult.Invalid, repo.recordReceiving(draft.copy(id = id(), lines = listOf(ReceivingLineDraft(original.id, "1")))))
        assertEquals(beforeOverflow, repo.snapshot())
    }

    @Test fun historicalSnapshotsAndDeliveryLedgerRoundTripAndResetImport() = runBlocking {
        val original = repo.snapshot().products.single()
        repo.recordReceiving(draft(original))
        repo.saveVendor(original.vendorId, VendorDraft("Renamed Vendor"))
        repo.saveProduct(original.id, ProductDraft("Renamed Product", "RENAMED-001", "Renamed Vendor", "Stationery", "10.00", "0", "3", original.vendorId))
        val before = repo.snapshot()
        assertEquals("Vendor", before.deliveries.single().vendorNameSnapshot)
        assertEquals("Notebook", before.deliveryItems.single().productNameSnapshot)
        assertEquals("RECEIVE-001", before.deliveryItems.single().skuSnapshot)
        val tables = SheetsMapper.toTables(before)
        val restored = SheetsMapper.fromTables(tables)
        assertEquals(before.deliveries, restored.deliveries)
        assertEquals(before.deliveryItems, restored.deliveryItems)
        assertEquals(SnapshotFingerprint.of(before), SnapshotFingerprint.of(restored))
        fun change(tableName: String, field: String, value: String) = tables.map { table ->
            if (table.name != tableName) table else table.copy(rows = table.rows.map { row -> row.mapIndexed { i, old -> if (i == table.headers.indexOf(field)) value else old } })
        }
        listOf(change("StockDeliveryItems", "quantity", "6"), change("StockDeliveries", "vendor_id", "missing"),
            change("StockDeliveries", "recorded_at", "2026-11-04T00:00:00Z")).forEach { broken ->
            assertThrows(WorkbookFormatException::class.java) { SheetsMapper.fromTables(broken) }
        }
        repo.resetLocalData()
        assertEquals(ShopSnapshot(), repo.snapshot())
        repo.completeSync(WorkbookBinding("delivery-book", "Deliveries", "subject", "test@example.invalid"), restored,
            SnapshotFingerprint.of(repo.snapshot()), download = true)
        assertEquals(before.deliveries, repo.snapshot().deliveries)
        assertEquals(before.deliveryItems, repo.snapshot().deliveryItems)
        assertEquals(7, repo.snapshot().products.single().quantity)
    }
}
