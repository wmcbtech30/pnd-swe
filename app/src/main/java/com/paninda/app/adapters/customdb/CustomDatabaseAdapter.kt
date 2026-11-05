package com.paninda.app.adapters.customdb

import com.paninda.app.adapters.*
import com.paninda.app.domain.model.ShopSnapshot

class CustomDatabaseAdapter : WorkbookAdapter {
    override suspend fun read(): AdapterResult<ShopSnapshot> = AdapterResult.Unavailable
    override suspend fun write(snapshot: ShopSnapshot): AdapterResult<Unit> = AdapterResult.Unavailable
}
