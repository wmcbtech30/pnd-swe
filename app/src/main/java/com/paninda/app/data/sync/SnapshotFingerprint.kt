package com.paninda.app.data.sync

import com.paninda.app.adapters.sheets.SheetsMapper
import com.paninda.app.domain.model.ShopSnapshot
import java.security.MessageDigest

/** Independent of Room ordering, worksheet row order and receipt acknowledgement state. */
object SnapshotFingerprint {
    fun of(snapshot: ShopSnapshot): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun field(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update("${bytes.size}:".toByteArray(Charsets.UTF_8))
            digest.update(bytes)
        }
        SheetsMapper.toTables(snapshot).forEach { table ->
            field(table.name)
            table.headers.forEach(::field)
            table.rows.sortedBy { it.firstOrNull().orEmpty() }.forEach { row -> row.forEach(::field) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
