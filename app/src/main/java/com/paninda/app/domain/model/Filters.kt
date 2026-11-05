package com.paninda.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val ShopZone: ZoneId = ZoneId.of("Asia/Manila")

enum class StockFilter { ALL, AVAILABLE, LOW, OUT }

data class ProductFilter(
    val stock: StockFilter = StockFilter.ALL,
    val vendorId: String? = null,
    val category: String? = null,
) {
    val active get() = stock != StockFilter.ALL || vendorId != null || category != null
    fun matches(product: Product, query: String): Boolean =
        product.isActive &&
            (query.isBlank() || product.name.contains(query.trim(), true) || product.sku.contains(query.trim(), true)) &&
            (vendorId == null || product.vendorId == vendorId) &&
            (category == null || product.category == category) &&
            when (stock) {
                StockFilter.ALL -> true
                StockFilter.AVAILABLE -> product.quantity > 0
                StockFilter.LOW -> product.isLowStock
                StockFilter.OUT -> product.isOutOfStock
            }
}

enum class DateRange { TODAY, WEEK, MONTH, ALL, CUSTOM }

data class DateFilter(
    val range: DateRange = DateRange.TODAY,
    val start: String = "",
    val end: String = "",
) {
    val valid get() = range != DateRange.CUSTOM || runCatching {
        !LocalDate.parse(start).isAfter(LocalDate.parse(end))
    }.getOrDefault(false)

    fun matches(transaction: Transaction, today: LocalDate): Boolean {
        return matches(transaction.soldAt, today)
    }

    fun matches(timestamp: Long, today: LocalDate): Boolean {
        val date = Instant.ofEpochMilli(timestamp).atZone(ShopZone).toLocalDate()
        return when (range) {
            DateRange.TODAY -> date == today
            DateRange.WEEK -> date >= today.minusDays(6) && date <= today
            DateRange.MONTH -> date >= today.minusDays(29) && date <= today
            DateRange.ALL -> true
            DateRange.CUSTOM -> valid && date >= LocalDate.parse(start) && date <= LocalDate.parse(end)
        }
    }
}
