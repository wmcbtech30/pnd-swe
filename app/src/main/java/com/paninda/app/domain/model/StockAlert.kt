package com.paninda.app.domain.model

enum class StockAlertLevel { HEALTHY, LOW, OUT }
fun Product.stockAlertLevel(): StockAlertLevel = when {
    !isActive -> StockAlertLevel.HEALTHY
    isOutOfStock -> StockAlertLevel.OUT
    isLowStock -> StockAlertLevel.LOW
    else -> StockAlertLevel.HEALTHY
}

data class StockAlertLatch(val low: Boolean = false, val out: Boolean = false)
data class StockAlertPlan(val latches: Map<String, StockAlertLatch>, val candidates: List<Product>, val attention: List<Product>) {
    fun acknowledged(): Map<String, StockAlertLatch> = latches.toMutableMap().apply {
        candidates.forEach { product -> this[product.id] = StockAlertLatch(low = true, out = product.stockAlertLevel() == StockAlertLevel.OUT) }
    }
}

/** Rearm each threshold only after recovery; blocked delivery must never acknowledge an alert. */
object StockAlertRules {
    fun plan(products: List<Product>, previous: Map<String, StockAlertLatch>): StockAlertPlan {
        val latches = mutableMapOf<String, StockAlertLatch>()
        val candidates = mutableListOf<Product>()
        val attention = products.filter { it.stockAlertLevel() != StockAlertLevel.HEALTHY }
            .sortedWith(compareByDescending<Product> { it.isOutOfStock }.thenBy { it.name }.thenBy { it.id })
        attention.forEach { product ->
            val old = previous[product.id] ?: StockAlertLatch()
            val next = if (product.stockAlertLevel() == StockAlertLevel.LOW) old.copy(out = false) else old
            latches[product.id] = next
            if (if (product.isOutOfStock) !next.out else !next.low) candidates += product
        }
        return StockAlertPlan(latches, candidates, attention)
    }
}
