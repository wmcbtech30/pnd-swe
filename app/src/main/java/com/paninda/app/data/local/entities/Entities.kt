package com.paninda.app.data.local.entities

import androidx.room.*
import com.paninda.app.domain.model.*
import com.paninda.app.domain.model.Transaction

@Entity(tableName = "vendors", indices = [Index(value = ["name"], unique = true)])
data class VendorEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "products",
    indices = [Index(value = ["sku"], unique = true), Index("vendorId")],
    foreignKeys = [ForeignKey(entity = VendorEntity::class, parentColumns = ["id"], childColumns = ["vendorId"])])
data class ProductEntity(
    @PrimaryKey val id: String, val sku: String, val name: String, val vendorId: String,
    val category: String, val unitPriceMinor: Long, val lowStockThreshold: Int,
    val isActive: Boolean, val updatedAt: Long, val version: Long,
)

data class ProductRow(@Embedded val record: ProductEntity, val vendorName: String, val quantity: Int) {
    fun toDomain() = Product(record.id, record.sku, record.name, record.vendorId, vendorName,
        record.category, record.unitPriceMinor, record.lowStockThreshold, quantity,
        record.isActive, record.updatedAt, record.version)
}

@Entity(tableName = "transactions", indices = [Index(value = ["receiptNumber"], unique = true)])
data class TransactionEntity(
    @PrimaryKey val id: String, val receiptNumber: String, val soldAt: Long, val cashierName: String,
    val paymentMethod: String, val totalMinor: Long, val amountTenderedMinor: Long?,
    val itemCount: Int, val syncState: String,
) {
    fun toDomain() = Transaction(id, receiptNumber, soldAt, cashierName,
        PaymentMethod.valueOf(paymentMethod), totalMinor, amountTenderedMinor,
        itemCount, SyncState.valueOf(syncState))
}

@Entity(tableName = "transaction_items",
    indices = [Index("transactionId"), Index("productId")],
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionId"]),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"]),
    ])
data class TransactionItemEntity(
    @PrimaryKey val id: String, val transactionId: String, val productId: String,
    val productNameSnapshot: String, val vendorIdSnapshot: String,
    val quantity: Int, val unitPriceMinor: Long,
) {
    fun toDomain() = TransactionItem(id, transactionId, productId, productNameSnapshot,
        vendorIdSnapshot, quantity, unitPriceMinor)
}

@Entity(tableName = "stock_movements", indices = [Index("productId")],
    foreignKeys = [ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"])])
data class StockMovementEntity(
    @PrimaryKey val id: String, val productId: String, val quantityDelta: Int,
    val movementType: String, val sourceId: String, val occurredAt: Long, val reason: String,
) {
    fun toDomain() = StockMovement(id, productId, quantityDelta, movementType, sourceId, occurredAt, reason)
}

@Entity(tableName = "pending_changes")
data class PendingChangeEntity(
    @PrimaryKey val id: String, val entityType: String, val entityId: String,
    val operation: String, val createdAt: Long,
)

@Entity(tableName = "app_state")
data class AppStateEntity(@PrimaryKey val key: String, val value: String)

fun Product.toEntity() = ProductEntity(id, sku, name, vendorId, category, unitPriceMinor,
    lowStockThreshold, isActive, updatedAt, version)
fun Transaction.toEntity() = TransactionEntity(id, receiptNumber, soldAt, cashierName,
    paymentMethod.name, totalMinor, amountTenderedMinor, itemCount, syncState.name)
fun TransactionItem.toEntity() = TransactionItemEntity(id, transactionId, productId,
    productNameSnapshot, vendorIdSnapshot, quantity, unitPriceMinor)
fun StockMovement.toEntity() = StockMovementEntity(id, productId, quantityDelta, movementType, sourceId, occurredAt, reason)
