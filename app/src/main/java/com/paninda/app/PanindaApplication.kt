package com.paninda.app

import android.app.Application
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.data.sync.GoogleWorkbookSync
import com.paninda.app.adapters.google.GoogleAuthorization

class PanindaApplication : Application() {
    val microcopies by lazy {
        val preferences = getSharedPreferences("home_microcopies", MODE_PRIVATE)
        com.paninda.app.ui.home.MicrocopyRotation(
            { period -> preferences.getInt(period.name, -1) },
            { period, index -> preferences.edit().putInt(period.name, index).apply() })
    }
    val stockNotifications by lazy { com.paninda.app.data.notifications.StockNotifications(this, database) }
    val network by lazy { com.paninda.app.data.network.NetworkMonitor(this) }
    val database by lazy { PanindaDatabase.create(this) }
    val repository by lazy { ShopRepository(database) }
    val googleSync by lazy { GoogleWorkbookSync(repository, GoogleAuthorization(this), network::current) }
}
