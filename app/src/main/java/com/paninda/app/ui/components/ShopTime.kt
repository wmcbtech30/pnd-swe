package com.paninda.app.ui.components

import androidx.compose.runtime.*
import com.paninda.app.domain.model.ShopZone
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun rememberShopDate(): LocalDate {
    val date by produceState(LocalDate.now(ShopZone)) {
        while (true) {
            delay(60_000)
            value = LocalDate.now(ShopZone)
        }
    }
    return date
}
