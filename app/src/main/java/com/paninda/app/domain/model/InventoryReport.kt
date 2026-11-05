package com.paninda.app.domain.model

import java.math.BigInteger
import java.time.LocalDate

data class VendorStock(val id: String, val name: String, val units: Long)

data class InventoryReport(val products: List<Product>) {
    val units = products.sumOf { it.quantity.toLong() }
    val low = products.count { it.isLowStock }
    val out = products.count { it.isOutOfStock }
    val negative = products.count { it.quantity < 0 }
    // Selling-price estimate of stock on hand; shortages are not assets or cost of goods.
    val retailValueMinor: BigInteger = products.fold(BigInteger.ZERO) { total, product ->
        total + BigInteger.valueOf(product.unitPriceMinor) * BigInteger.valueOf(product.quantity.coerceAtLeast(0).toLong())
    }
    val byVendor = products.groupBy { it.vendorId }.map { (id, rows) ->
        VendorStock(id, rows.first().vendorName, rows.sumOf { it.quantity.coerceAtLeast(0).toLong() })
    }.sortedWith(compareByDescending<VendorStock> { it.units }.thenBy { it.name.lowercase() }.thenBy { it.id })

    companion object {
        fun from(products: List<Product>, filter: ProductFilter = ProductFilter()) =
            InventoryReport(products.filter { filter.matches(it, "") })
    }
}

data class MovementReport(val rows: List<StockMovement>) {
    val added = rows.filter { it.quantityDelta > 0 }.sumOf { it.quantityDelta.toLong() }
    val removed = rows.filter { it.quantityDelta < 0 }.sumOf { -it.quantityDelta.toLong() }
    val net = added - removed

    companion object {
        fun from(movements: List<StockMovement>, products: List<Product>, date: DateFilter,
            today: LocalDate, vendorId: String? = null): MovementReport {
            val ids = products.filter { vendorId == null || it.vendorId == vendorId }.map { it.id }.toSet()
            return MovementReport(movements.filter { it.productId in ids && date.matches(it.occurredAt, today) }
                .sortedWith(compareByDescending<StockMovement> { it.occurredAt }.thenBy { it.id }))
        }
    }
}
