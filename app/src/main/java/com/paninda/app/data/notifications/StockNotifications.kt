package com.paninda.app.data.notifications

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.paninda.app.data.local.PanindaDatabase
import com.paninda.app.domain.model.StockAlertRules
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class StockNotificationStatus(val enabled: Boolean = true, val permitted: Boolean = false, val prompted: Boolean = false, val requested: Boolean = false)

/** Runs while the app process is alive. It observes committed Room data and never polls Google. */
class StockNotifications internal constructor(context: Context, private val database: PanindaDatabase, private val publisher: StockAlertSink) {
    constructor(context: Context, database: PanindaDatabase) : this(context, database, StockNotificationPublisher(context))
    private val preferences = context.getSharedPreferences("stock_notifications", Context.MODE_PRIVATE)
    private val store = StockAlertStore(preferences)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        Log.e("PanindaStockAlerts", "Stock notification observation stopped", error)
    })
    private val mutableStatus = MutableStateFlow(readStatus())
    val status = mutableStatus.asStateFlow()
    private var job: Job? = null
    private fun readStatus() = StockNotificationStatus(preferences.getBoolean("enabled", true), publisher.permitted(), preferences.getBoolean("prompted", false), preferences.getBoolean("requested", false))
    fun refresh() { mutableStatus.value = readStatus() }
    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("enabled", enabled).apply()
        refresh()
    }
    fun markPermissionRequested() { preferences.edit().putBoolean("requested", true).apply(); refresh() }
    fun markPrompted() { preferences.edit().putBoolean("prompted", true).apply(); refresh() }
    fun start() {
        if (job?.isActive == true) return
        refresh()
        job = scope.launch {
            combine(database.invalidationTracker.createFlow("products", "vendors", "stock_movements", "app_state"), status) { _, config -> config }
                .collect { config ->
                    val (connected, products) = database.withTransaction {
                        val dao = database.shopDao()
                        (dao.state("connected") == "true" && !dao.state("workbook_id").isNullOrBlank()) to dao.products().map { it.toDomain() }
                    }
                    val plan = StockAlertRules.plan(products, store.read())
                    // Rearm recovery even if permission/settings currently prevent delivery.
                    store.write(plan.latches)
                    if (!connected || !config.enabled || !publisher.permitted() || plan.attention.isEmpty()) publisher.cancel()
                    else if (plan.candidates.isNotEmpty()) {
                        if (publisher.publish(plan.attention, alert = true)) store.write(plan.acknowledged())
                    } else if (publisher.active()) publisher.publish(plan.attention, alert = false)
                }
        }
    }
    fun stop() { job?.cancel(); job = null }
}
