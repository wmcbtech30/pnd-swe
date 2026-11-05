package com.paninda.app.domain.model

data class ShopSnapshot(
    val vendors: List<Vendor> = emptyList(),
    val products: List<Product> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val items: List<TransactionItem> = emptyList(),
    val movements: List<StockMovement> = emptyList(),
)

data class ShopState(
    val loading: Boolean = true,
    val connected: Boolean = false,
    val products: List<Product> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val pendingCount: Int = 0,
    val lastCheckedAt: Long? = null,
    val loadFailed: Boolean = false,
)
