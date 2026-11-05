package com.paninda.app.data.notifications

import android.content.SharedPreferences
import com.paninda.app.domain.model.StockAlertLatch
import org.json.JSONObject

/** Device-only delivery markers; never part of the workbook/domain snapshot or pending sync queue. */
internal class StockAlertStore(private val preferences: SharedPreferences) {
    fun read(): Map<String, StockAlertLatch> = runCatching {
        val json = JSONObject(preferences.getString("latches", "{}") ?: "{}")
        json.keys().asSequence().associateWith { id ->
            val flags = json.getInt(id)
            StockAlertLatch(low = flags and 1 != 0, out = flags and 2 != 0)
        }
    }.getOrDefault(emptyMap())
    fun write(latches: Map<String, StockAlertLatch>) {
        val json = JSONObject()
        latches.toSortedMap().forEach { (id, latch) -> json.put(id, (if (latch.low) 1 else 0) + (if (latch.out) 2 else 0)) }
        val value = json.toString()
        if (preferences.getString("latches", null) != value) preferences.edit().putString("latches", value).commit()
    }
}
