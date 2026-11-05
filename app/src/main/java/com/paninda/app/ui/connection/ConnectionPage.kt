package com.paninda.app.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.paninda.app.ui.theme.*

/** Shared onboarding banner; only the content scrolls and actions stay above the keyboard/navigation. */
@Composable
fun ConnectionPage(title: String, actions: @Composable RowScope.() -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Palette.White).imePadding().windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val heroHeight = (maxHeight * .52f).coerceIn(130.dp, 419.dp)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(heroHeight)
                    .background(Brush.verticalGradient(0f to Color(0xFF63ADC8),
                        .44231f to Color(0xFF63ADC8), 1f to Color(0xFF0084FF)))) {
                    Text(title, Modifier.align(Alignment.BottomStart).padding(start = 17.dp, end = 16.dp, bottom = 24.dp),
                        fontFamily = AppFonts.Serif, fontStyle = FontStyle.Italic, color = Palette.White,
                        fontSize = 64.sp, lineHeight = 76.sp)
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), content = content)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            content = actions)
    }
}
