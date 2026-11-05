package com.paninda.app.adapters

import com.paninda.app.domain.model.ShopSnapshot

sealed interface AdapterResult<out T> {
    data class Available<T>(val value: T) : AdapterResult<T>
    data object Unavailable : AdapterResult<Nothing>
}

/** Transport boundary only. Business rules and Room queries do not depend on a workbook provider. */
interface WorkbookAdapter {
    suspend fun read(): AdapterResult<ShopSnapshot>
    suspend fun write(snapshot: ShopSnapshot): AdapterResult<Unit>
}
