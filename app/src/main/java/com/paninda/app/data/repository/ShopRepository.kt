package com.paninda.app.data.repository

import androidx.room.withTransaction
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.local.entities.*
import com.paninda.app.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.util.Locale
import java.util.UUID

class ShopRepository(private val db: PanindaDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.shopDao()
    val state = combine(dao.observeProducts(), dao.observeTransactions(),
        dao.observePendingCount(), dao.observeState()) { products, transactions, pending, settings ->
        val values = settings.associate { it.key to it.value }
        ShopState(loading = values["initialized"] != "true", connected = values["connected"] == "true",
            products = products.map { it.toDomain() }, transactions = transactions.map { it.toDomain() },
            pendingCount = pending, lastCheckedAt = values["last_checked"]?.toLongOrNull())
    }

    suspend fun initialize() = db.withTransaction {
        if (dao.state("initialized") == null) {
            dao.putState(AppStateEntity("initialized", "true"))
        }
    }

    suspend fun connectLocal() = dao.putState(AppStateEntity("connected", "true"))
    suspend fun disconnect() = dao.putState(AppStateEntity("connected", "false"))
    suspend fun checkOffline() = dao.putState(AppStateEntity("last_checked", clock.millis().toString()))

    suspend fun resetLocalData() = db.withTransaction {
        dao.clearItems()
        dao.clearTransactions()
        dao.clearMovements()
        dao.clearProducts()
        dao.clearVendors()
        dao.clearPending()
        dao.clearLastChecked()
        // Preserve initialization and the local session. No records are seeded.
        dao.putState(AppStateEntity("initialized", "true"))
    }

    suspend fun items(transactionId: String) = dao.items(transactionId).map { it.toDomain() }
    suspend fun movements(productId: String) = dao.productMovements(productId).map { it.toDomain() }
    suspend fun snapshot() = db.withTransaction {
        ShopSnapshot(dao.vendors().map { Vendor(it.id, it.name) }, dao.products().map { it.toDomain() },
            dao.transactions().map { it.toDomain() }, dao.allItems().map { it.toDomain() },
            dao.movements().map { it.toDomain() })
    }

    suspend fun saveProduct(id: String?, draft: ProductDraft): SaveProductResult {
        val name = draft.name.trim()
        val sku = draft.sku.trim().uppercase(Locale.ROOT)
        val vendorName = draft.vendorName.trim()
        val category = draft.category.trim()
        val price = Money.parse(draft.price)
        val quantity = draft.initialQuantity.toIntOrNull()
        val threshold = draft.threshold.toIntOrNull()
        if (name.isEmpty() || name.length > 120 || !Regex("[A-Z0-9._-]{1,64}").matches(sku) ||
            vendorName.isEmpty() || vendorName.length > 120 || category.isEmpty() || category.length > 80 ||
            price == null || threshold == null || threshold !in 0..1_000_000 ||
            (id == null && (quantity == null || quantity !in 0..1_000_000))) return SaveProductResult.Invalid
        return try {
            db.withTransaction {
                val existing = id?.let { dao.product(it) }
                if (id != null && existing == null) return@withTransaction SaveProductResult.Missing
                val duplicate = dao.productBySku(sku)
                if (duplicate != null && duplicate.id != id) return@withTransaction SaveProductResult.DuplicateSku
                val now = clock.millis()
                val vendor = dao.vendorByName(vendorName) ?: VendorEntity(UUID.randomUUID().toString(), vendorName).also {
                    dao.putVendor(it)
                    queue("vendor", it.id, now)
                }
                val productId = id ?: UUID.randomUUID().toString()
                dao.putProduct(ProductEntity(productId, sku, name, vendor.id, category, price, threshold,
                    existing?.isActive ?: true, now, (existing?.version ?: 0) + 1))
                if (existing == null) {
                    val movement = StockMovement(UUID.randomUUID().toString(), productId, requireNotNull(quantity),
                        "OPENING", productId, now, "Opening stock")
                    dao.putMovements(listOf(movement.toEntity()))
                    queue("stock_movement", movement.id, now)
                }
                queue("product", productId, now)
                SaveProductResult.Saved(productId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            SaveProductResult.Failed
        }
    }

    private suspend fun queue(type: String, id: String, now: Long) =
        dao.putPending(PendingChangeEntity("$type:$id", type, id, "UPSERT", now))
}
