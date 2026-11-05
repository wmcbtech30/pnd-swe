package com.paninda.app.data.local.dao

import androidx.room.*
import com.paninda.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

private const val PRODUCT_QUERY = """
    SELECT products.*, vendors.name AS vendorName,
    COALESCE(SUM(stock_movements.quantityDelta), 0) AS quantity
    FROM products JOIN vendors ON products.vendorId = vendors.id
    LEFT JOIN stock_movements ON products.id = stock_movements.productId
    GROUP BY products.id ORDER BY products.rowid
"""

@Dao
interface ShopDao {
    @Query(PRODUCT_QUERY) fun observeProducts(): Flow<List<ProductRow>>
    @Query(PRODUCT_QUERY) suspend fun products(): List<ProductRow>
    @Query("SELECT * FROM transactions ORDER BY soldAt DESC, receiptNumber DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions ORDER BY soldAt DESC") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM vendors ORDER BY name") suspend fun vendors(): List<VendorEntity>
    @Query("SELECT * FROM transaction_items WHERE transactionId = :id ORDER BY rowid")
    suspend fun items(id: String): List<TransactionItemEntity>
    @Query("SELECT * FROM transaction_items ORDER BY rowid") suspend fun allItems(): List<TransactionItemEntity>
    @Query("SELECT * FROM stock_movements ORDER BY occurredAt DESC") suspend fun movements(): List<StockMovementEntity>
    @Query("SELECT * FROM stock_movements WHERE productId = :id ORDER BY occurredAt DESC")
    suspend fun productMovements(id: String): List<StockMovementEntity>
    @Query("SELECT COUNT(*) FROM pending_changes") fun observePendingCount(): Flow<Int>
    @Query("SELECT * FROM pending_changes") suspend fun pending(): List<PendingChangeEntity>
    @Query("SELECT * FROM app_state") fun observeState(): Flow<List<AppStateEntity>>
    @Query("SELECT value FROM app_state WHERE [key] = :key") suspend fun state(key: String): String?
    @Query("SELECT * FROM products WHERE id = :id") suspend fun product(id: String): ProductEntity?
    @Query("SELECT * FROM products WHERE sku = :sku") suspend fun productBySku(sku: String): ProductEntity?
    @Query("SELECT * FROM vendors WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun vendorByName(name: String): VendorEntity?
    @Upsert suspend fun putVendor(record: VendorEntity)
    @Upsert suspend fun putProduct(record: ProductEntity)
    @Insert suspend fun putTransactions(records: List<TransactionEntity>)
    @Insert suspend fun putItems(records: List<TransactionItemEntity>)
    @Insert suspend fun putMovements(records: List<StockMovementEntity>)
    @Upsert suspend fun putPending(record: PendingChangeEntity)
    @Upsert suspend fun putState(record: AppStateEntity)
    @Query("DELETE FROM transaction_items") suspend fun clearItems()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM stock_movements") suspend fun clearMovements()
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM vendors") suspend fun clearVendors()
    @Query("DELETE FROM pending_changes") suspend fun clearPending()
    @Query("DELETE FROM app_state WHERE [key] = 'last_checked'") suspend fun clearLastChecked()
}
