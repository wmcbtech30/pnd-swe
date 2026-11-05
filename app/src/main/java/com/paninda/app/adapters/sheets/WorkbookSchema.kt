package com.paninda.app.adapters.sheets

data class WorkbookTable(val name: String, val headers: List<String>, val rows: List<List<String>>) {
    val startCell: String get() = "A1"
}

object WorkbookSchema {
    const val VERSION = "1"
    val headers = linkedMapOf(
        "Meta" to listOf("key", "value"),
        "Vendors" to listOf("vendor_id", "name", "contact_person", "phone", "email", "address", "notes", "is_active", "updated_at", "version"),
        "Products" to listOf("product_id", "sku", "name", "vendor_id", "category",
            "unit_price_minor", "low_stock_threshold", "is_active", "updated_at", "version"),
        "Sales" to listOf("sale_id", "receipt_number", "sold_at", "cashier_name", "payment_method",
            "total_minor", "amount_tendered_minor", "item_count"),
        "SaleItems" to listOf("sale_item_id", "sale_id", "product_id", "product_name_snapshot",
            "vendor_id_snapshot", "quantity", "unit_price_minor"),
        "StockCounts" to listOf("count_id", "counted_at", "counted_by", "notes"),
        "StockCountItems" to listOf("count_item_id", "count_id", "product_id", "product_name_snapshot", "sku_snapshot", "vendor_id_snapshot", "expected_quantity", "counted_quantity"),
        "StockMovements" to listOf("movement_id", "product_id", "quantity_delta",
            "movement_type", "source_id", "occurred_at", "reason"),
    )
    fun acceptsLiveHeaders(name: String, actual: List<String>) = actual == headers.getValue(name)
}

class WorkbookFormatException(message: String) : IllegalArgumentException(message)
