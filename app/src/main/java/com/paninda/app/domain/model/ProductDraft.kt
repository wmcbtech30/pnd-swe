package com.paninda.app.domain.model

data class ProductDraft(
    val name: String,
    val sku: String,
    val vendorName: String,
    val category: String,
    val price: String,
    val initialQuantity: String,
    val threshold: String,
    val vendorId: String? = null,
)

sealed interface SaveProductResult {
    data class Saved(val id: String) : SaveProductResult
    data object Invalid : SaveProductResult
    data object DuplicateSku : SaveProductResult
    data object VendorUnavailable : SaveProductResult
    data object Missing : SaveProductResult
    data object Failed : SaveProductResult
}
