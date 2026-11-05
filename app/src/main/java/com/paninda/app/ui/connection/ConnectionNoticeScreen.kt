package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.ui.theme.*

/** Figma 28:404: fixed gradient hero/actions, independently scrolling body on compact displays. */
@Composable
fun ConnectionNoticeScreen(failed: Boolean, reconnect: Boolean = false, busy: Boolean = false,
    onStartOver: () -> Unit, onProceed: () -> Unit, bodyOverride: Int? = null) {
    BackHandler { if (!busy) onStartOver() }
    Column(Modifier.fillMaxSize().background(Palette.White).windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val heroHeight = (maxHeight * .52f).coerceIn(170.dp, 419.dp)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(heroHeight)
                    .background(Brush.verticalGradient(0f to Color(0xFF63ADC8),
                        .44231f to Color(0xFF63ADC8), 1f to Color(0xFF0084FF)))) {
                    Text(stringResource(if (failed) R.string.connection_failed_title else R.string.connection_warning_title),
                        Modifier.align(Alignment.BottomStart).padding(start = 17.dp, end = 16.dp, bottom = 24.dp),
                        fontFamily = AppFonts.Serif, fontStyle = FontStyle.Italic, color = Palette.White,
                        fontSize = 64.sp, lineHeight = 76.sp)
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(16.dp)) {
                    Text(stringResource(bodyOverride ?: when {
                        failed -> R.string.connection_failed_body
                        reconnect -> R.string.connection_resume_warning
                        else -> R.string.connection_new_warning
                    }), fontFamily = AppFonts.Sans, fontSize = 20.sp, lineHeight = 24.sp)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onStartOver, enabled = !busy, modifier = Modifier.heightIn(min = 54.dp)) {
                Text(stringResource(R.string.connection_start_over))
            }
            Button(onProceed, enabled = !busy, shape = CircleShape, modifier = Modifier.widthIn(min = 139.dp).heightIn(min = 54.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Palette.White)
                else Text(stringResource(if (failed) R.string.connection_try_again else R.string.connection_proceed))
            }
        }
    }
}
