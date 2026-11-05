package com.paninda.app.domain.model

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val vendorId: String,
    val vendorName: String,
    val category: String,
    val unitPriceMinor: Long,
    val lowStockThreshold: Int,
    val quantity: Int = 0,
    val isActive: Boolean = true,
    val updatedAt: Long,
    val version: Long = 1,
) {
    val isLowStock get() = quantity in 1..lowStockThreshold
    val isOutOfStock get() = quantity <= 0
}
