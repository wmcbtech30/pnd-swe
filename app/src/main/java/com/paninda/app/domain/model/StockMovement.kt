package com.paninda.app.domain.model

data class StockMovement(
    val id: String,
    val productId: String,
    val quantityDelta: Int,
    val movementType: String,
    val sourceId: String,
    val occurredAt: Long,
    val reason: String,
)
