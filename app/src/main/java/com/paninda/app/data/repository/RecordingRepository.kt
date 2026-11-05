package com.paninda.app.data.repository

import androidx.room.withTransaction
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.local.entities.*
import com.paninda.app.domain.model.*
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.util.UUID
import java.util.Locale

/** Atomic local recording. No payment processor, network or Google dependency. */
internal class RecordingRepository(private val db: PanindaDatabase, private val clock: Clock) {
    private val dao = db.shopDao()
    private suspend fun queue(type: String, id: String, now: Long) =
        dao.putPending(PendingChangeEntity("$type:$id", type, id, "UPSERT", now))

    suspend fun recordSale(draft: SaleDraft): RecordResult = guarded {
        db.withTransaction {
            if (dao.transaction(draft.id) != null) return@withTransaction RecordResult.Saved(draft.id)
            if (draft.id.isBlank() || draft.id.length > 100 || draft.recordedBy.trim().length !in 1..120 ||
                draft.lines.isEmpty() || draft.lines.size > 1000 || draft.lines.map { it.productId }.distinct().size != draft.lines.size)
                return@withTransaction RecordResult.Invalid
            val products = dao.products().associateBy { it.record.id }
            val now = clock.millis()
            val items = mutableListOf<TransactionItem>()
            val movements = mutableListOf<StockMovement>()
            var total = 0L
            var units = 0
            for (line in draft.lines) {
                val product = products[line.productId]?.takeIf { it.record.isActive } ?: return@withTransaction RecordResult.Unavailable
                val quantity = line.quantity.toIntOrNull()?.takeIf { it in 1..1_000_000 } ?: return@withTransaction RecordResult.Invalid
                val price = Money.parse(line.price) ?: return@withTransaction RecordResult.Invalid
                if (quantity > product.quantity) return@withTransaction RecordResult.InsufficientStock
                total = Math.addExact(total, Math.multiplyExact(quantity.toLong(), price))
                units = Math.addExact(units, quantity)
                items += TransactionItem(UUID.randomUUID().toString(), draft.id, product.record.id,
                    product.record.name, product.record.vendorId, quantity, price)
                movements += StockMovement(UUID.randomUUID().toString(), product.record.id, -quantity, "SALE", draft.id, now, "Recorded sale")
            }
            val sale = Transaction(draft.id, "SR-${draft.id.replace("-", "").uppercase(Locale.ROOT)}", now,
                draft.recordedBy.trim(), PaymentMethod.NOT_RECORDED, total, null, units)
            dao.putTransactions(listOf(sale.toEntity()))
            dao.putItems(items.map { it.toEntity() })
            dao.putMovements(movements.map { it.toEntity() })
            items.forEach {
                val product = products.getValue(it.productId).record
                dao.putProduct(product.copy(updatedAt = now, version = Math.addExact(product.version, 1)))
                queue("product", product.id, now)
                queue("sale_item", it.id, now)
            }
            movements.forEach { queue("stock_movement", it.id, now) }
            queue("sale", sale.id, now)
            RecordResult.Saved(sale.id)
        }
    }

    suspend fun recordCount(draft: CountDraft): RecordResult = guarded {
        db.withTransaction {
            if (dao.count(draft.id) != null) return@withTransaction RecordResult.Saved(draft.id)
            val entries = draft.entries.filter { it.countedQuantity.isNotBlank() }
            if (draft.id.isBlank() || draft.id.length > 100 || draft.countedBy.trim().length !in 1..120 || draft.notes.trim().length > 1000 ||
                entries.isEmpty() || entries.map { it.productId }.distinct().size != entries.size) return@withTransaction RecordResult.Invalid
            val products = dao.products().associateBy { it.record.id }
            val now = clock.millis()
            val items = mutableListOf<StockCountItem>()
            for (entry in entries) {
                val product = products[entry.productId]?.takeIf { it.record.isActive } ?: return@withTransaction RecordResult.Unavailable
                if (product.quantity != entry.expectedQuantity || product.record.version != entry.expectedVersion) return@withTransaction RecordResult.StaleCount
                val counted = entry.countedQuantity.toIntOrNull()?.takeIf { it in 0..1_000_000 } ?: return@withTransaction RecordResult.Invalid
                val item = StockCountItem(UUID.randomUUID().toString(), draft.id, product.record.id,
                    product.record.name, product.record.sku, product.record.vendorId, entry.expectedQuantity, counted)
                if (item.variance !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return@withTransaction RecordResult.Invalid
                items += item
            }
            if (items.any { it.variance != 0L } && draft.notes.isBlank()) return@withTransaction RecordResult.Invalid
            dao.putCount(StockCount(draft.id, now, draft.countedBy.trim(), draft.notes.trim()).toEntity())
            dao.putCountItems(items.map { it.toEntity() })
            items.forEach { item ->
                if (item.variance != 0L) {
                    val movement = StockMovement(UUID.randomUUID().toString(), item.productId, item.variance.toInt(),
                        "COUNT_ADJUSTMENT", draft.id, now, draft.notes.trim())
                    dao.putMovements(listOf(movement.toEntity()))
                    queue("stock_movement", movement.id, now)
                }
                // A completed count invalidates other open counts even if it found no variance.
                val product = products.getValue(item.productId).record
                dao.putProduct(product.copy(updatedAt = now, version = Math.addExact(product.version, 1)))
                queue("product", product.id, now)
                queue("stock_count_item", item.id, now)
            }
            queue("stock_count", draft.id, now)
            RecordResult.Saved(draft.id)
        }
    }

    private suspend fun guarded(block: suspend () -> RecordResult): RecordResult = try { block() }
        catch (e: CancellationException) { throw e }
        catch (_: ArithmeticException) { RecordResult.Invalid }
        catch (_: Exception) { RecordResult.Failed }
}
