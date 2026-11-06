package com.paninda.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paninda.app.data.local.dao.ShopDao
import com.paninda.app.data.local.entities.*

@Database(
    entities = [VendorEntity::class, ProductEntity::class, TransactionEntity::class,
        TransactionItemEntity::class, StockMovementEntity::class,
        PendingChangeEntity::class, AppStateEntity::class, StockCountEntity::class, StockCountItemEntity::class, StockDeliveryEntity::class, StockDeliveryItemEntity::class],
    version = 1, exportSchema = true,
)
abstract class PanindaDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    companion object {
        fun create(context: Context): PanindaDatabase =
            Room.databaseBuilder(context.applicationContext, PanindaDatabase::class.java, "paninda-local.db").build()
    }
}
