package com.paninda.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.theme.Palette

data class Pillar(val route: String, val label: Int, val icon: ImageVector)
val Pillars = listOf(
    Pillar("home", R.string.common_home, Icons.Default.Home),
    Pillar("sell", R.string.common_sell, Icons.Default.AttachMoney),
    Pillar("inventory", R.string.common_inventory, Icons.Default.Inventory2),
    Pillar("history", R.string.common_history, Icons.Default.History),
    Pillar("tools", R.string.common_tools, Icons.Default.MoreHoriz),
)

fun pillarFor(route: String): String = when {
    route.startsWith("product") -> "inventory"
    route.startsWith("transaction") -> "history"
    route == "sync" || route == "settings" -> "tools"
    else -> route.substringBefore("?")
}

@Composable
fun BottomNavigation(route: String, onNavigate: (String) -> Unit) {
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(Modifier.fillMaxWidth().background(Palette.Navigation).padding(bottom = maxOf(navigationInset, 24.dp))
        .height(66.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Pillars.forEach { pillar ->
            val selected = pillarFor(route) == pillar.route
            Box(Modifier.size(48.dp).selectable(selected, role = Role.Tab,
                onClick = { onNavigate(pillar.route) }), contentAlignment = Alignment.Center) {
                Icon(pillar.icon, stringResource(pillar.label), Modifier.size(32.dp),
                    tint = if (selected) Palette.Blue else Palette.Icon)
            }
        }
    }
}
