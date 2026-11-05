package com.paninda.app.domain.model

enum class PaymentMethod { CASH, MANUAL_OTHER }
enum class SyncState { PENDING, ACKNOWLEDGED }

data class Transaction(
    val id: String,
    val receiptNumber: String,
    val soldAt: Long,
    val cashierName: String,
    val paymentMethod: PaymentMethod,
    val totalMinor: Long,
    val amountTenderedMinor: Long?,
    val itemCount: Int,
    val syncState: SyncState = SyncState.PENDING,
)
