package com.paninda.app.data.repository

import androidx.room.withTransaction
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.local.entities.*
import com.paninda.app.domain.model.*
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Instant
import java.util.UUID

/** Delivery header, item snapshots, stock ledger and outbox commit together. */
internal class ReceivingRepository(private val db: PanindaDatabase, private val clock: Clock) {
    private val dao = db.shopDao()
    private suspend fun queue(type: String, id: String, now: Long) =
        dao.putPending(PendingChangeEntity("$type:$id", type, id, "UPSERT", now))

    suspend fun record(draft: ReceivingDraft): RecordResult = try {
        db.withTransaction {
            if (dao.delivery(draft.id) != null) return@withTransaction RecordResult.Saved(draft.id)
            val now = clock.millis()
            val date = receivingDate(draft.receivedDate) ?: return@withTransaction RecordResult.Invalid
            if (draft.id.isBlank() || draft.id.length > 100 || date > Instant.ofEpochMilli(now).atZone(ShopZone).toLocalDate() ||
                draft.receivedBy.trim().length !in 1..120 || draft.reference.trim().length > 120 || draft.notes.trim().length > 1000 ||
                draft.lines.isEmpty() || draft.lines.size > 1000 || draft.lines.map { it.productId }.distinct().size != draft.lines.size)
                return@withTransaction RecordResult.Invalid
            val vendor = dao.vendor(draft.vendorId)?.takeIf { it.isActive } ?: return@withTransaction RecordResult.Unavailable
            val products = dao.products().associateBy { it.record.id }
            val receivedAt = date.atStartOfDay(ShopZone).toInstant().toEpochMilli()
            val items = mutableListOf<StockDeliveryItem>()
            for (line in draft.lines) {
                val product = products[line.productId]?.takeIf { it.record.isActive && it.record.vendorId == vendor.id }
                    ?: return@withTransaction RecordResult.Unavailable
                val quantity = line.quantity.toIntOrNull()?.takeIf { it in 1..1_000_000 } ?: return@withTransaction RecordResult.Invalid
                val balance = product.quantity.toLong() + quantity
                if (balance !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return@withTransaction RecordResult.Invalid
                Math.addExact(product.record.version, 1) // Validate every update before any write.
                items += StockDeliveryItem(UUID.randomUUID().toString(), draft.id, product.record.id,
                    product.record.name, product.record.sku, quantity)
            }
            val delivery = StockDelivery(draft.id, vendor.id, vendor.name, receivedAt, now, draft.receivedBy.trim(), draft.reference.trim(), draft.notes.trim())
            dao.putDelivery(delivery.toEntity())
            dao.putDeliveryItems(items.map { it.toEntity() })
            items.forEach { item ->
                val reason = if (delivery.reference.isBlank()) "Stock received" else "Stock received: ${delivery.reference}"
                val movement = StockMovement(UUID.randomUUID().toString(), item.productId, item.quantity, "RECEIVING", delivery.id, receivedAt, reason)
                dao.putMovements(listOf(movement.toEntity()))
                val product = products.getValue(item.productId).record
                dao.putProduct(product.copy(updatedAt = now, version = product.version + 1))
                queue("product", product.id, now)
                queue("stock_movement", movement.id, now)
                queue("stock_delivery_item", item.id, now)
            }
            queue("stock_delivery", delivery.id, now)
            RecordResult.Saved(delivery.id)
        }
    } catch (e: CancellationException) { throw e }
    catch (_: ArithmeticException) { RecordResult.Invalid }
    catch (_: Exception) { RecordResult.Failed }
}
