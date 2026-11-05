package com.paninda.app

import com.paninda.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class StockAlertRulesTest {
    @Test fun thresholdsDeduplicateEscalateAndRearmWithoutAcknowledgingBlockedDelivery() {
        val product = TestFixtures.snapshot().products.single()
        fun plan(quantity: Int, previous: Map<String, StockAlertLatch> = emptyMap()) = StockAlertRules.plan(listOf(product.copy(quantity = quantity)), previous)
        assertTrue(plan(4).attention.isEmpty())
        val low = plan(3)
        assertEquals(listOf(product.id), low.candidates.map { it.id })
        assertEquals(1, plan(2, low.latches).candidates.size) // No delivery acknowledgement yet.
        val deliveredLow = low.acknowledged()
        assertTrue(plan(2, deliveredLow).candidates.isEmpty())
        val out = plan(0, deliveredLow)
        assertEquals(1, out.candidates.size)
        val deliveredOut = out.acknowledged()
        assertTrue(plan(-2, deliveredOut).candidates.isEmpty())
        val restockedLow = plan(1, deliveredOut)
        assertTrue(restockedLow.candidates.isEmpty())
        assertEquals(1, plan(0, restockedLow.latches).candidates.size)
        val recovered = plan(4, deliveredOut)
        assertTrue(recovered.latches.isEmpty())
        assertEquals(1, plan(3, recovered.latches).candidates.size)
        assertTrue(StockAlertRules.plan(listOf(product.copy(quantity = 0, isActive = false)), deliveredOut).latches.isEmpty())
        assertTrue(StockAlertRules.plan(emptyList(), deliveredLow).latches.isEmpty())
        assertEquals(StockAlertLevel.OUT, product.copy(quantity = 0, lowStockThreshold = 0).stockAlertLevel())
        assertEquals(StockAlertLevel.HEALTHY, product.copy(quantity = 1, lowStockThreshold = 0).stockAlertLevel())
    }
}
