package com.paninda.app

import androidx.room.Room
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.*
import com.paninda.app.data.sync.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingRepositoryTest {
    private lateinit var db: PanindaDatabase
    private lateinit var repo: ShopRepository
    @Before fun setup() = runBlocking<Unit> {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PanindaDatabase::class.java).build()
        repo = ShopRepository(db)
        repo.initialize()
        repo.saveProduct(null, ProductDraft("Notebook", "RECORD-001", "Vendor", "Stationery", "9.50", "12", "3"))
    }
    @After fun close() = db.close()
    private fun id() = UUID.randomUUID().toString()
    private suspend fun product() = repo.snapshot().products.single()
    private fun entry(product: Product, physical: String) = CountEntryDraft(product.id, product.quantity, product.version, physical)

    @Test fun completedSaleIsAtomicIdempotentAndHasNoRecordedPayment() = runBlocking {
        val product = product()
        val original = repo.snapshot()
        val invalid = SaleDraft(id(), "Recorder", listOf(SaleLineDraft(product.id, "2", "8.25"), SaleLineDraft("missing", "1", "1")))
        assertEquals(RecordResult.Unavailable, repo.recordSale(invalid))
        assertEquals(original, repo.snapshot())
        assertEquals(RecordResult.InsufficientStock, repo.recordSale(SaleDraft(id(), "Recorder", listOf(SaleLineDraft(product.id, "13", "9.50")))))
        val draft = SaleDraft(id(), " Recorder ", listOf(SaleLineDraft(product.id, "2", "8.25")))
        assertEquals(RecordResult.Saved(draft.id), repo.recordSale(draft))
        assertEquals(RecordResult.Saved(draft.id), repo.recordSale(draft))
        val snapshot = repo.snapshot()
        assertEquals(10, snapshot.products.single().quantity)
        assertEquals(1, snapshot.transactions.size)
        assertEquals(1650L, snapshot.transactions.single().totalMinor)
        assertEquals(PaymentMethod.NOT_RECORDED, snapshot.transactions.single().paymentMethod)
        assertNull(snapshot.transactions.single().amountTenderedMinor)
        assertEquals("Recorder", snapshot.transactions.single().cashierName)
        assertEquals(product.name, snapshot.items.single().productNameSnapshot)
        assertEquals(-2, snapshot.movements.single { it.movementType == "SALE" }.quantityDelta)
        assertEquals(1, db.shopDao().pending().count { it.entityType == "sale" })
        assertEquals(snapshot.items, SheetsMapper.fromTables(SheetsMapper.toTables(snapshot)).items)
    }

    @Test fun countsPreserveZeroNoChangeAndVarianceHistoryAndRoundTripThroughSheets() = runBlocking {
        val original = product()
        assertEquals(RecordResult.Invalid, repo.recordCount(CountDraft(id(), "Recorder", "", listOf(entry(original, "9")))))
        val count = CountDraft(id(), "Recorder", "Physical check", listOf(entry(original, "9")))
        assertEquals(RecordResult.Saved(count.id), repo.recordCount(count))
        assertEquals(RecordResult.Saved(count.id), repo.recordCount(count))
        assertEquals(9, product().quantity)
        assertEquals(-3L, repo.snapshot().countItems.single().variance)
        val unchanged = CountDraft(id(), "Recorder", "", listOf(entry(product(), "9")))
        assertTrue(repo.recordCount(unchanged) is RecordResult.Saved)
        val zero = CountDraft(id(), "Recorder", "No physical stock", listOf(entry(product(), "0")))
        assertTrue(repo.recordCount(zero) is RecordResult.Saved)
        assertEquals(0, product().quantity)
        val before = repo.snapshot()
        assertEquals(3, before.counts.size)
        assertEquals(2, before.movements.count { it.movementType == "COUNT_ADJUSTMENT" })
        val tables = SheetsMapper.toTables(before)
        val parsed = SheetsMapper.fromTables(tables)
        assertEquals(before.counts, parsed.counts)
        assertEquals(before.countItems, parsed.countItems)
        val broken = tables.map { table -> if (table.name != "StockCountItems") table else table.copy(rows = table.rows.mapIndexed { i, row ->
            if (i != 0) row else row.mapIndexed { index, value -> if (index == table.headers.indexOf("counted_quantity")) "8" else value }
        }) }
        assertThrows(WorkbookFormatException::class.java) { SheetsMapper.fromTables(broken) }
        repo.resetLocalData()
        repo.completeSync(WorkbookBinding("count-book", "Counts", "subject", "test@example.invalid"), parsed,
            SnapshotFingerprint.of(repo.snapshot()), download = true)
        assertEquals(before.countItems, repo.snapshot().countItems)
        assertEquals(before.counts.toSet(), repo.snapshot().counts.toSet())
    }

    @Test fun staleCountsCannotOverwriteSalesOrOtherCountsAndBlankFieldsDoNotMeanZero() = runBlocking {
        val original = product()
        val old = CountDraft(id(), "Recorder", "Physical check", listOf(entry(original, "7")))
        repo.recordSale(SaleDraft(id(), "Recorder", listOf(SaleLineDraft(original.id, "1", "9.50"))))
        val afterSale = repo.snapshot()
        assertEquals(RecordResult.StaleCount, repo.recordCount(old))
        assertEquals(afterSale, repo.snapshot())
        assertEquals(RecordResult.Invalid, repo.recordCount(CountDraft(id(), "Recorder", "", listOf(entry(product(), "")))))
        val same = CountDraft(id(), "Recorder", "", listOf(entry(product(), "11")))
        assertTrue(repo.recordCount(same) is RecordResult.Saved)
        assertEquals(RecordResult.StaleCount, repo.recordCount(same.copy(id = id())))
        assertEquals(11, product().quantity)
        assertEquals(1, repo.snapshot().counts.size)
    }
}
