package com.paninda.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.paninda.app.R
import com.paninda.app.ui.theme.Palette
import com.paninda.app.ui.theme.AppFonts

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
    route.startsWith("counts") || route.startsWith("vendor") || route.startsWith("reports") ||
        route == "sync" || route == "settings" -> "tools"
    else -> route.substringBefore("?")
}

@Composable
fun BottomNavigation(route: String, onNavigate: (String) -> Unit) {
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(Modifier.fillMaxWidth().background(Palette.Navigation).padding(bottom = maxOf(navigationInset, 24.dp))
        .height(IntrinsicSize.Min).padding(horizontal = 4.dp)) {
        Pillars.forEach { pillar ->
            val selected = pillarFor(route) == pillar.route
            val label = stringResource(pillar.label)
            val color = if (selected) Palette.Blue else Palette.Icon
            Column(Modifier.weight(1f).fillMaxHeight().heightIn(min = 72.dp)
                .selectable(selected, role = Role.Tab, onClick = { onNavigate(pillar.route) })
                .semantics { contentDescription = label }
                .padding(horizontal = 2.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(pillar.icon, null, Modifier.size(32.dp), tint = color)
                Spacer(Modifier.height(4.dp))
                Text(label, fontFamily = AppFonts.Sans, fontSize = 12.sp, lineHeight = 16.sp,
                    color = color, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics {})
            }
        }
    }
}
