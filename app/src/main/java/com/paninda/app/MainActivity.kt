package com.paninda.app

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.paninda.app.data.notifications.StockNotificationPublisher
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paninda.app.ui.PanindaApp
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.theme.PanindaTheme

class MainActivity : ComponentActivity() {
    private var openStockAlerts by mutableStateOf(false)
    override fun onResume() {
        super.onResume()
        (application as PanindaApplication).stockNotifications.start()
        (application as PanindaApplication).stockNotifications.refresh()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(StockNotificationPublisher.OPEN_ALERTS, false)) openStockAlerts = true
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_stock_alert", openStockAlerts)
        super.onSaveInstanceState(outState)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openStockAlerts = savedInstanceState?.getBoolean("pending_stock_alert")
            ?: intent.getBooleanExtra(StockNotificationPublisher.OPEN_ALERTS, false)
        (application as PanindaApplication).stockNotifications.start()
        // Compose is always light even when the device theme is dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.BLACK),
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            PanindaTheme(darkTheme = false) {
                val vm: ShopViewModel = viewModel(factory = ShopViewModel.Factory(
                    (application as PanindaApplication).repository, { (application as PanindaApplication).googleSync },
                    (application as PanindaApplication).network.changes))
                PanindaApp(vm, openStockAlerts) {
                    openStockAlerts = false
                    intent.removeExtra(StockNotificationPublisher.OPEN_ALERTS)
                }
            }
        }
    }
}
