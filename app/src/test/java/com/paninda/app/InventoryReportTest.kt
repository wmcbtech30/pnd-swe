package com.paninda.app

import com.paninda.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate

class InventoryReportTest {
    private val base = TestFixtures.snapshot().products.single()

    @Test fun stockBucketsFiltersAndValueUseActiveProductsWithoutCountingShortagesAsAssets() {
        val products = listOf(base.copy(quantity = 8), base.copy(id = "low", quantity = 2),
            base.copy(id = "zero", quantity = 0), base.copy(id = "negative", quantity = -3),
            base.copy(id = "inactive", quantity = 999, isActive = false),
            base.copy(id = "other", vendorId = "other", vendorName = "Other", quantity = 5))
        val report = InventoryReport.from(products)
        assertEquals(12L, report.units)
        assertEquals(1, report.low)
        assertEquals(2, report.out)
        assertEquals(1, report.negative)
        assertEquals(BigInteger.valueOf(15 * base.unitPriceMinor), report.retailValueMinor)
        assertEquals(listOf(10L, 5L), report.byVendor.map { it.units })
        assertEquals(4, InventoryReport.from(products, ProductFilter(vendorId = base.vendorId)).products.size)
        assertEquals(listOf("low"), InventoryReport.from(products, ProductFilter(stock = StockFilter.LOW)).products.map { it.id })
        assertTrue(InventoryReport.from(products, ProductFilter(category = "Missing")).products.isEmpty())
        val huge = InventoryReport.from(listOf(base.copy(unitPriceMinor = Long.MAX_VALUE, quantity = Int.MAX_VALUE)))
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE) * BigInteger.valueOf(Int.MAX_VALUE.toLong()), huge.retailValueMinor)
    }

    @Test fun movementsRespectManilaDayBoundsVendorAndArchivedProducts() {
        fun movement(id: String, product: String, quantity: Int, time: String) =
            StockMovement(id, product, quantity, "ADJUSTMENT", product, Instant.parse(time).toEpochMilli(), "Correction")
        val rows = listOf(movement("prior", base.id, 30, "2026-11-04T15:59:59Z"),
            movement("start", base.id, 5, "2026-11-04T16:00:00Z"),
            movement("end", base.id, -2, "2026-11-05T15:59:59Z"),
            movement("after", base.id, 40, "2026-11-05T16:00:00Z"),
            movement("other", "other-product", 100, "2026-11-05T01:00:00Z"))
        val products = listOf(base.copy(isActive = false), base.copy(id = "other-product", vendorId = "other"))
        val report = MovementReport.from(rows, products, DateFilter(), LocalDate.parse("2026-11-05"), base.vendorId)
        assertEquals(listOf("end", "start"), report.rows.map { it.id })
        assertEquals(5L, report.added)
        assertEquals(2L, report.removed)
        assertEquals(3L, report.net)
        assertTrue(MovementReport.from(rows, products, DateFilter(DateRange.CUSTOM, "bad", "date"), LocalDate.now()).rows.isEmpty())
    }
}
