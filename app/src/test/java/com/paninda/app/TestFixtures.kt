package com.paninda.app

import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.local.entities.toEntity
import com.paninda.app.domain.model.*
import java.time.Instant

/** Test-only records. Nothing in this file is packaged in the application. */
object TestFixtures {
    val now = Instant.parse("2026-11-05T01:00:00Z").toEpochMilli()
    fun snapshot(): ShopSnapshot {
        val vendor = Vendor("v-test", "Test Vendor")
        val product = Product("p-test", "TEST-001", "Test Notebook", vendor.id, vendor.name,
            "Stationery", 12345, 3, 8, updatedAt = now)
        val sale = Transaction("s-test", "R-TEST-1", now, "Test staff", PaymentMethod.CASH,
            10000, 20000, 2)
        val line = TransactionItem("i-test", sale.id, product.id, "Original notebook name", vendor.id, 2, 5000)
        val movement = StockMovement("m-test", product.id, 8, "OPENING", product.id, now, "Test opening")
        return ShopSnapshot(listOf(vendor), listOf(product), listOf(sale), listOf(line), listOf(movement))
    }

    suspend fun receipt(db: PanindaDatabase, product: Product, id: String, soldAt: Long) {
        val sale = Transaction(id, "R-" + id.uppercase(), soldAt, "Test staff", PaymentMethod.CASH,
            10000, 20000, 2)
        db.shopDao().putTransactions(listOf(sale.toEntity()))
        db.shopDao().putItems(listOf(TransactionItem("$id-line", id, product.id,
            "Original product name", product.vendorId, 2, 5000).toEntity()))
    }
}
