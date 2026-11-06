package com.paninda.app.domain.model

import java.time.LocalDate

/** Incoming deliveries are separate from physical counts and sales. */
data class StockDelivery(val id: String, val vendorId: String, val vendorNameSnapshot: String,
    val receivedAt: Long, val recordedAt: Long, val receivedBy: String, val reference: String, val notes: String)
data class StockDeliveryItem(val id: String, val deliveryId: String, val productId: String,
    val productNameSnapshot: String, val skuSnapshot: String, val quantity: Int)
data class ReceivingLineDraft(val productId: String, val quantity: String = "1")
data class ReceivingDraft(val id: String, val vendorId: String, val receivedDate: String,
    val receivedBy: String, val reference: String, val notes: String, val lines: List<ReceivingLineDraft>)

fun receivingDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
