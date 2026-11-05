package com.paninda.app

import android.app.Application
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.data.sync.GoogleWorkbookSync
import com.paninda.app.adapters.google.GoogleAuthorization

class PanindaApplication : Application() {
    val database by lazy { PanindaDatabase.create(this) }
    val repository by lazy { ShopRepository(database) }
    val googleSync by lazy { GoogleWorkbookSync(repository, GoogleAuthorization(this)) }
}
