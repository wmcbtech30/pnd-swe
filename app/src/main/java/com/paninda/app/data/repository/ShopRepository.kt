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
import com.paninda.app.data.sync.SnapshotFingerprint
import com.paninda.app.data.sync.WorkbookBinding

class ShopRepository(private val db: PanindaDatabase, private val clock: Clock = Clock.systemUTC()) {
    private val dao = db.shopDao()
    private val baseState = combine(dao.observeProducts(), dao.observeTransactions(),
        dao.observePendingCount(), dao.observeState(), dao.observeVendors()) { products, transactions, pending, settings, vendors ->
        val values = settings.associate { it.key to it.value }
        ShopState(loading = values["initialized"] != "true", connected = values["connected"] == "true" && !values["workbook_id"].isNullOrBlank(),
            products = products.map { it.toDomain() }, transactions = transactions.map { it.toDomain() },
            pendingCount = pending, lastCheckedAt = values["last_checked"]?.toLongOrNull(),
            vendors = vendors.map { it.toDomain() }, workbookName = values["workbook_name"], accountEmail = values["account_email"],
            syncStatus = if (pending > 0 && values["sync_status"] == "synced") "pending" else values["sync_status"] ?: "idle")
    }

    val state = combine(baseState, dao.observeMovements(), dao.observeCounts()) { state, movements, counts ->
        state.copy(movements = movements.map { it.toDomain() }, counts = counts.map { it.toDomain() })
    }

    suspend fun initialize() = db.withTransaction {
        if (dao.state("initialized") == null) {
            dao.putState(AppStateEntity("initialized", "true"))
        }
    }

    suspend fun disconnect() = db.withTransaction {
        dao.putState(AppStateEntity("connected", "false"))
        // Retain the last binding/base to reconnect safely, and retain all pending local data.
        dao.putState(AppStateEntity("sync_status", "idle"))
    }
    suspend fun syncStatus(value: String) = dao.putState(AppStateEntity("sync_status", value))
    suspend fun binding(): WorkbookBinding? {
        val id = dao.state("workbook_id")?.takeIf { it.isNotBlank() } ?: return null
        return WorkbookBinding(id, dao.state("workbook_name").orEmpty(),
            dao.state("account_subject").orEmpty(), dao.state("account_email").orEmpty())
    }
    suspend fun baseFor(binding: WorkbookBinding): String? {
        val previous = binding()
        return if (previous?.id == binding.id && previous.subject == binding.subject)
            dao.state("sync_base")?.takeIf { it.isNotBlank() } else null
    }

    /** The comparison and import/acknowledgement share a transaction with every product edit. */
    suspend fun completeSync(binding: WorkbookBinding, remote: ShopSnapshot, expectedLocal: String,
        download: Boolean) = db.withTransaction {
        val unchanged = SnapshotFingerprint.of(snapshot()) == expectedLocal
        if (download && !unchanged) throw com.paninda.app.data.sync.SyncConflictException()
        if (download) {
            clearBusinessData()
            remote.vendors.forEach { dao.putVendor(it.toEntity()) }
            remote.products.forEach { dao.putProduct(it.toEntity()) }
            dao.putTransactions(remote.transactions.map { it.toEntity() })
            dao.putItems(remote.items.map { it.toEntity() })
            dao.putMovements(remote.movements.map { it.toEntity() })
            remote.counts.forEach { dao.putCount(it.toEntity()) }
            dao.putCountItems(remote.countItems.map { it.toEntity() })
        }
        if (unchanged) {
            dao.clearPending()
            dao.acknowledgeTransactions(remote.transactions.map { it.id })
        }
        mapOf("workbook_id" to binding.id, "workbook_name" to binding.name,
            "account_subject" to binding.subject, "account_email" to binding.email,
            "sync_base" to SnapshotFingerprint.of(remote), "connected" to "true",
            "sync_status" to "synced", "last_checked" to clock.millis().toString())
            .forEach { (key, value) -> dao.putState(AppStateEntity(key, value)) }
    }

    suspend fun resetLocalData() = db.withTransaction {
        clearBusinessData()
        dao.clearLastChecked()
        // Local reset must never be translated into deleting the remote workbook.
        dao.putState(AppStateEntity("sync_base", ""))
        dao.putState(AppStateEntity("sync_status", "idle"))
        dao.putState(AppStateEntity("initialized", "true"))
    }

    private suspend fun clearBusinessData() {
        dao.clearCountItems()
        dao.clearCounts()
        dao.clearItems()
        dao.clearTransactions()
        dao.clearMovements()
        dao.clearProducts()
        dao.clearVendors()
        dao.clearPending()
    }

    suspend fun recordSale(draft: SaleDraft) = RecordingRepository(db, clock).recordSale(draft)
    suspend fun recordCount(draft: CountDraft) = RecordingRepository(db, clock).recordCount(draft)
    suspend fun countItems(id: String) = dao.countItems(id).map { it.toDomain() }

    suspend fun items(transactionId: String) = dao.items(transactionId).map { it.toDomain() }
    suspend fun movements(productId: String) = dao.productMovements(productId).map { it.toDomain() }
    suspend fun snapshot() = db.withTransaction {
        ShopSnapshot(dao.vendors().map { it.toDomain() }, dao.products().map { it.toDomain() },
            dao.transactions().map { it.toDomain() }, dao.allItems().map { it.toDomain() },
            dao.movements().map { it.toDomain() }, dao.counts().map { it.toDomain() }, dao.allCountItems().map { it.toDomain() })
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
                val selected = if (draft.vendorId != null) dao.vendor(draft.vendorId)
                    ?: return@withTransaction SaveProductResult.VendorUnavailable else dao.vendors().firstOrNull { it.name.equals(vendorName, true) }
                if (selected?.isActive == false && existing?.vendorId != selected.id)
                    return@withTransaction SaveProductResult.VendorUnavailable
                val vendor = selected ?: VendorEntity(UUID.randomUUID().toString(), vendorName, updatedAt = now).also {
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

    suspend fun saveVendor(id: String?, draft: VendorDraft): SaveVendorResult {
        val clean = VendorDraft(draft.name.trim(), draft.contactPerson.trim(), draft.phone.trim(),
            draft.email.trim(), draft.address.trim(), draft.notes.trim())
        if (clean.name.isBlank() || clean.name.length > 120 || clean.contactPerson.length > 120 ||
            clean.phone.length > 40 || clean.email.length > 254 || clean.address.length > 300 || clean.notes.length > 1000 ||
            (clean.email.isNotEmpty() && !Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(clean.email))) return SaveVendorResult.Invalid
        return try {
            db.withTransaction {
                val existing = id?.let { dao.vendor(it) }
                if (id != null && existing == null) return@withTransaction SaveVendorResult.Missing
                val duplicate = dao.vendors().firstOrNull { it.name.equals(clean.name, true) && it.id != id }
                if (duplicate != null) return@withTransaction SaveVendorResult.DuplicateName
                val now = clock.millis()
                val vendorId = id ?: UUID.randomUUID().toString()
                dao.putVendor(VendorEntity(vendorId, clean.name, clean.contactPerson, clean.phone,
                    clean.email, clean.address, clean.notes, existing?.isActive ?: true, now, (existing?.version ?: 0) + 1))
                queue("vendor", vendorId, now)
                SaveVendorResult.Saved(vendorId)
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { SaveVendorResult.Failed }
    }

    suspend fun setVendorActive(id: String, active: Boolean): SaveVendorResult = try {
        db.withTransaction {
            val vendor = dao.vendor(id) ?: return@withTransaction SaveVendorResult.Missing
            if (vendor.isActive != active) {
                val now = clock.millis()
                dao.putVendor(vendor.copy(isActive = active, updatedAt = now, version = vendor.version + 1))
                queue("vendor", id, now)
            }
            SaveVendorResult.Saved(id)
        }
    } catch (e: CancellationException) { throw e }
    catch (_: Exception) { SaveVendorResult.Failed }

    private suspend fun queue(type: String, id: String, now: Long) =
        dao.putPending(PendingChangeEntity("$type:$id", type, id, "UPSERT", now))
}
