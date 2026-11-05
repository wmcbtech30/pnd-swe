package com.paninda.app.domain.model

data class TransactionItem(
    val id: String,
    val transactionId: String,
    val productId: String,
    val productNameSnapshot: String,
    val vendorIdSnapshot: String,
    val quantity: Int,
    val unitPriceMinor: Long,
) {
    val lineTotalMinor get() = Math.multiplyExact(quantity.toLong(), unitPriceMinor)
}
