package com.paninda.app.domain.model

data class PendingChange(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val createdAt: Long,
)
