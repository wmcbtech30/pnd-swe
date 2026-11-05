package com.paninda.app.domain.model

/** Records a completed sale; payment handling is deliberately outside this workflow. */
data class SaleLineDraft(val productId: String, val quantity: String = "1", val price: String)
data class SaleDraft(val id: String, val recordedBy: String, val lines: List<SaleLineDraft>)

data class StockCount(val id: String, val countedAt: Long, val countedBy: String, val notes: String)
data class StockCountItem(val id: String, val countId: String, val productId: String,
    val productNameSnapshot: String, val skuSnapshot: String, val vendorIdSnapshot: String,
    val expectedQuantity: Int, val countedQuantity: Int) {
    val variance get() = countedQuantity.toLong() - expectedQuantity.toLong()
}
data class CountEntryDraft(val productId: String, val expectedQuantity: Int, val expectedVersion: Long,
    val countedQuantity: String = "")
data class CountDraft(val id: String, val countedBy: String, val notes: String, val entries: List<CountEntryDraft>)

sealed interface RecordResult {
    data class Saved(val id: String) : RecordResult
    data object Invalid : RecordResult
    data object Unavailable : RecordResult
    data object InsufficientStock : RecordResult
    data object StaleCount : RecordResult
    data object Failed : RecordResult
}
