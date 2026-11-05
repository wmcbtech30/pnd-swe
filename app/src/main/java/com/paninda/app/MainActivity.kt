package com.paninda.app

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                PanindaApp(vm)
            }
        }
    }
}
