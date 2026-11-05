package com.paninda.app

import android.app.Application
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository

class PanindaApplication : Application() {
    val database by lazy { PanindaDatabase.create(this) }
    val repository by lazy { ShopRepository(database) }
}
