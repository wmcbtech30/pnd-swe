package com.paninda.app

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            PanindaTheme(darkTheme = false) {
                val vm: ShopViewModel = viewModel(factory = ShopViewModel.Factory(
                    (application as PanindaApplication).repository, { (application as PanindaApplication).googleSync }))
                PanindaApp(vm)
            }
        }
    }
}
