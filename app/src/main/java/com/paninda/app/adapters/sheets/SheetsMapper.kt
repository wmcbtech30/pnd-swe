package com.paninda.app.adapters.sheets

import com.paninda.app.domain.model.*
import java.time.Instant
import java.util.Locale

/** Pure conversion and validation. It never calls a network API or writes to the local database. */
object SheetsMapper {
    fun toTables(snapshot: ShopSnapshot): List<WorkbookTable> {
        fun instant(value: Long) = Instant.ofEpochMilli(value).toString()
        fun table(name: String, rows: List<List<String>>) =
            WorkbookTable(name, WorkbookSchema.headers.getValue(name), rows)
        val tables = listOf(
            table("Meta", listOf(listOf("schema_version", WorkbookSchema.VERSION),
                listOf("currency", "PHP"), listOf("shop_timezone", ShopZone.id))),
            table("Vendors", snapshot.vendors.map { listOf(it.id, it.name, it.contactPerson, it.phone,
                it.email, it.address, it.notes, if (it.isActive) "TRUE" else "FALSE", instant(it.updatedAt), it.version.toString()) }),
            table("Products", snapshot.products.map { listOf(it.id, it.sku, it.name, it.vendorId,
                it.category, it.unitPriceMinor.toString(), it.lowStockThreshold.toString(),
                if (it.isActive) "TRUE" else "FALSE", instant(it.updatedAt), it.version.toString()) }),
            table("Sales", snapshot.transactions.map { listOf(it.id, it.receiptNumber, instant(it.soldAt),
                it.cashierName, it.paymentMethod.name, it.totalMinor.toString(),
                it.amountTenderedMinor?.toString().orEmpty(), it.itemCount.toString()) }),
            table("SaleItems", snapshot.items.map { listOf(it.id, it.transactionId, it.productId,
                it.productNameSnapshot, it.vendorIdSnapshot, it.quantity.toString(), it.unitPriceMinor.toString()) }),
            table("StockCounts", snapshot.counts.map { listOf(it.id, instant(it.countedAt), it.countedBy, it.notes) }),
            table("StockCountItems", snapshot.countItems.map { listOf(it.id, it.countId, it.productId, it.productNameSnapshot,
                it.skuSnapshot, it.vendorIdSnapshot, it.expectedQuantity.toString(), it.countedQuantity.toString()) }),
            table("StockMovements", snapshot.movements.map { listOf(it.id, it.productId, it.quantityDelta.toString(),
                it.movementType, it.sourceId, instant(it.occurredAt), it.reason) }),
        )
        val parsed = fromTables(tables)
        ensure(snapshot.products.associate { it.id to it.quantity } ==
            parsed.products.associate { it.id to it.quantity }, "Product quantities disagree with stock movements")
        return tables
    }

    fun fromTables(tables: List<WorkbookTable>): ShopSnapshot {
        ensure(tables.map { it.name }.distinct().size == tables.size, "Duplicate worksheet names")
        val byName = tables.associateBy { it.name }
        fun records(name: String): List<Map<String, String>> {
            val table = byName[name] ?: fail("Missing worksheet: $name")
            ensure(table.headers.distinct().size == table.headers.size, "Duplicate headers in $name")
            val required = WorkbookSchema.headers.getValue(name)
            ensure(table.headers.containsAll(required), "Missing headers in $name")
            return table.rows.filter { row -> row.any { it.isNotBlank() } }.map { row ->
                ensure(row.size <= table.headers.size, "Values beyond headers in $name")
                table.headers.mapIndexed { index, header -> header to row.getOrElse(index) { "" } }.toMap()
            }
        }
        val metaRows = records("Meta")
        unique(metaRows.map { it.text("key") }, "metadata keys")
        val meta = metaRows.associate { it.text("key") to it.text("value") }
        ensure(meta["schema_version"] == WorkbookSchema.VERSION, "Unsupported schema version")
        ensure(meta["currency"] == "PHP" && meta["shop_timezone"] == ShopZone.id, "Unsupported currency or timezone")

        val vendors = records("Vendors").map { row ->
            fun optional(key: String, limit: Int): String = row[key].orEmpty().trim().also {
                ensure(it.length <= limit, "Vendor field too long: $key")
            }
            val email = optional("email", 254)
            ensure(email.isEmpty() || Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email), "Invalid vendor email")
            val active = row.boolean("is_active")
            val updated = row.time("updated_at")
            val version = row.nonnegative("version")
            ensure(version >= 1 && row.text("name").length <= 120, "Invalid vendor name or version")
            Vendor(row.text("vendor_id"), row.text("name"), optional("contact_person", 120), optional("phone", 40),
                email, optional("address", 300), optional("notes", 1000), active, updated, version)
        }
        unique(vendors.map { it.id }, "vendor IDs")
        unique(vendors.map { it.name.lowercase(Locale.ROOT) }, "vendor names")
        val vendorNames = vendors.associate { it.id to it.name }
        val movements = records("StockMovements").map {
            StockMovement(it.text("movement_id"), it.text("product_id"), it.integer("quantity_delta"),
                it.text("movement_type"), it.text("source_id"), it.time("occurred_at"), it.text("reason"))
        }
        unique(movements.map { it.id }, "movement IDs")
        val quantities = movements.groupBy { it.productId }.mapValues { (_, rows) ->
            val sum = rows.sumOf { it.quantityDelta.toLong() }
            ensure(sum in Int.MIN_VALUE..Int.MAX_VALUE, "Stock quantity overflow")
            sum.toInt()
        }
        val products = records("Products").map {
            val id = it.text("product_id")
            val vendorId = it.text("vendor_id")
            val sku = it.text("sku").uppercase(Locale.ROOT)
            ensure(Regex("[A-Z0-9._-]{1,64}").matches(sku), "Invalid SKU")
            Product(id, sku, it.text("name"), vendorId, vendorNames[vendorId] ?: fail("Unknown vendor"),
                it.text("category"), it.nonnegative("unit_price_minor"), it.integer("low_stock_threshold").also { n ->
                    ensure(n >= 0, "Negative threshold")
                }, quantities[id] ?: 0, it.boolean("is_active"), it.time("updated_at"),
                it.nonnegative("version").also { n -> ensure(n >= 1, "Invalid version") })
        }
        unique(products.map { it.id }, "product IDs")
        unique(products.map { it.sku }, "SKUs")
        val productIds = products.map { it.id }.toSet()
        ensure(movements.all { it.productId in productIds }, "Movement references unknown product")

        val transactions = records("Sales").map {
            val payment = runCatching { PaymentMethod.valueOf(it.text("payment_method")) }.getOrElse { fail("Invalid payment method") }
            val total = it.nonnegative("total_minor")
            val tendered = it["amount_tendered_minor"]?.trim()?.takeIf(String::isNotEmpty)?.let { n ->
                n.toLongOrNull()?.takeIf { amount -> amount >= total } ?: fail("Invalid amount tendered")
            }
            ensure(payment != PaymentMethod.CASH || tendered != null, "Missing cash amount tendered")
            Transaction(it.text("sale_id"), it.text("receipt_number"), it.time("sold_at"),
                it.text("cashier_name"), payment, total, tendered, it.integer("item_count").also { n ->
                    ensure(n >= 1, "Invalid item count")
                }, SyncState.ACKNOWLEDGED)
        }
        unique(transactions.map { it.id }, "sale IDs")
        unique(transactions.map { it.receiptNumber }, "receipt numbers")
        val saleIds = transactions.map { it.id }.toSet()
        val items = records("SaleItems").map {
            TransactionItem(it.text("sale_item_id"), it.text("sale_id"), it.text("product_id"),
                it.text("product_name_snapshot"), it.text("vendor_id_snapshot"),
                it.integer("quantity").also { q -> ensure(q > 0, "Invalid sale quantity") },
                it.nonnegative("unit_price_minor"))
        }
        unique(items.map { it.id }, "sale item IDs")
        ensure(items.all { it.transactionId in saleIds && it.productId in productIds && it.vendorIdSnapshot in vendorNames },
            "Sale item has an unknown reference")
        val groupedItems = items.groupBy { it.transactionId }
        transactions.forEach { transaction ->
            val lines = groupedItems[transaction.id].orEmpty()
            val total = try { lines.fold(0L) { total, line -> Math.addExact(total, line.lineTotalMinor) } }
                catch (_: ArithmeticException) { fail("Receipt total overflow") }
            ensure(total == transaction.totalMinor && lines.sumOf { it.quantity.toLong() } == transaction.itemCount.toLong(),
                "Receipt total or item count does not match its lines")
        }
        val counts = records("StockCounts").map {
            val recorder = it.text("counted_by")
            val notes = it["notes"].orEmpty().trim()
            ensure(recorder.length <= 120 && notes.length <= 1000, "Invalid count recorder or notes")
            StockCount(it.text("count_id"), it.time("counted_at"), recorder, notes)
        }
        unique(counts.map { it.id }, "count IDs")
        val countIds = counts.map { it.id }.toSet()
        val countItems = records("StockCountItems").map {
            val counted = it.integer("counted_quantity")
            ensure(counted in 0..1_000_000, "Invalid physical quantity")
            StockCountItem(it.text("count_item_id"), it.text("count_id"), it.text("product_id"),
                it.text("product_name_snapshot"), it.text("sku_snapshot"), it.text("vendor_id_snapshot"),
                it.integer("expected_quantity"), counted)
        }
        unique(countItems.map { it.id }, "count item IDs")
        ensure(countItems.all { it.countId in countIds && it.productId in productIds && it.vendorIdSnapshot in vendorNames }, "Unknown count reference")
        counts.forEach { count ->
            val lines = countItems.filter { it.countId == count.id }
            ensure(lines.isNotEmpty(), "Empty stock count")
            unique(lines.map { it.productId }, "count product IDs")
            ensure(lines.none { it.variance != 0L } || count.notes.isNotBlank(), "Missing variance explanation")
            lines.forEach { line ->
                val adjustments = movements.filter { it.movementType == "COUNT_ADJUSTMENT" && it.sourceId == count.id && it.productId == line.productId }
                ensure(if (line.variance == 0L) adjustments.isEmpty() else adjustments.size == 1 && adjustments.single().quantityDelta.toLong() == line.variance,
                    "Count adjustment does not match variance")
            }
        }
        ensure(movements.filter { it.movementType == "COUNT_ADJUSTMENT" }.all { movement ->
            countItems.any { it.countId == movement.sourceId && it.productId == movement.productId }
        }, "Unknown count adjustment reference")
        return ShopSnapshot(vendors, products, transactions, items, movements, counts, countItems)
    }

    private fun Map<String, String>.text(key: String): String =
        get(key)?.trim()?.takeIf { it.isNotEmpty() } ?: fail("Missing value: $key")
    private fun Map<String, String>.integer(key: String): Int = text(key).toIntOrNull() ?: fail("Invalid integer: $key")
    private fun Map<String, String>.nonnegative(key: String): Long =
        text(key).toLongOrNull()?.takeIf { it >= 0 } ?: fail("Invalid nonnegative integer: $key")
    private fun Map<String, String>.time(key: String): Long =
        runCatching { Instant.parse(text(key)).toEpochMilli() }.getOrElse { fail("Invalid timestamp: $key") }
    private fun Map<String, String>.boolean(key: String): Boolean = when (text(key).uppercase(Locale.ROOT)) {
        "TRUE" -> true
        "FALSE" -> false
        else -> fail("Invalid boolean: $key")
    }
    private fun unique(values: List<String>, label: String) = ensure(values.distinct().size == values.size, "Duplicate $label")
    private fun ensure(condition: Boolean, message: String) { if (!condition) fail(message) }
    private fun fail(message: String): Nothing = throw WorkbookFormatException(message)
}
