package com.paninda.app

import com.paninda.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MoneyAndFiltersTest {
    @Test fun moneyUsesExactMinorUnits() {
        assertEquals(950L, Money.parse("9.50"))
        assertEquals(950L, Money.parse("9.5"))
        assertEquals(0L, Money.parse("0"))
        listOf("", "-1", "1.001", "1e3", "NaN", "12,00", "10000000.00").forEach {
            assertNull(it, Money.parse(it))
        }
        assertEquals("1,234.50", Money.amount(123450))
    }

    @Test fun lowAndZeroStockAreSeparateAndSearchMatchesSku() {
        val product = TestFixtures.snapshot().products.single()
        assertTrue(ProductFilter().matches(product, "test-001"))
        assertFalse(ProductFilter(stock = StockFilter.LOW).matches(product, ""))
        assertTrue(ProductFilter(stock = StockFilter.LOW).matches(product.copy(quantity = 3), ""))
        assertFalse(ProductFilter(stock = StockFilter.LOW).matches(product.copy(quantity = 0), ""))
        assertTrue(ProductFilter(stock = StockFilter.OUT).matches(product.copy(quantity = 0), ""))
        assertFalse(ProductFilter(vendorId = "other").matches(product, ""))
    }

    @Test fun datesUseTheShopDayAndValidateCustomRanges() {
        val sale = TestFixtures.snapshot().transactions.single()
        val day = LocalDate.parse("2026-11-05")
        val midnight = Instant.parse("2026-11-04T16:00:00Z").toEpochMilli()
        assertTrue(DateFilter().matches(sale.copy(soldAt = midnight), day))
        assertFalse(DateFilter().matches(sale.copy(soldAt = midnight - 1), day))
        assertTrue(DateFilter(DateRange.WEEK).matches(sale.copy(soldAt = midnight - 6 * 86_400_000), day))
        assertFalse(DateFilter(DateRange.CUSTOM, "2026-11-06", "2026-11-01").valid)
        assertFalse(DateFilter(DateRange.CUSTOM, "not-a-date", "2026-11-05").valid)
        assertTrue(DateFilter(DateRange.CUSTOM, "2026-11-05", "2026-11-05").matches(sale, day))
    }
}
