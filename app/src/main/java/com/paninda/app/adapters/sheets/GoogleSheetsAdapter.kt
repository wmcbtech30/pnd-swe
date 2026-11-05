package com.paninda.app.adapters.sheets

import com.paninda.app.adapters.*
import com.paninda.app.domain.model.ShopSnapshot

/** No OAuth, workbook connection, or network transport is configured in this offline build. */
class GoogleSheetsAdapter : WorkbookAdapter {
    override suspend fun read(): AdapterResult<ShopSnapshot> = AdapterResult.Unavailable
    override suspend fun write(snapshot: ShopSnapshot): AdapterResult<Unit> = AdapterResult.Unavailable
}
