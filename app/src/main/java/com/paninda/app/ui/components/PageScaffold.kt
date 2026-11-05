package com.paninda.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.ui.theme.*

@Composable
fun PageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: @Composable ColumnScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = Palette.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Column(Modifier.padding(top = maxOf(statusInset, 24.dp))) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End,
                    content = actions)
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.width(32.dp).height(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                stringResource(R.string.common_back), Modifier.size(28.dp), tint = Palette.Icon)
                        }
                    } else Spacer(Modifier.width(6.dp))
                    Text(title, fontFamily = AppFonts.Serif, fontStyle = FontStyle.Italic,
                        fontSize = 32.sp, lineHeight = 42.sp, modifier = Modifier.weight(1f))
                }
                if (subtitle != null) {
                    Text(subtitle, fontFamily = AppFonts.Plex, fontSize = 13.sp, lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 28.dp, end = 20.dp))
                }
                Spacer(Modifier.height(12.dp))
                header()
            }
        },
        floatingActionButton = fab,
        content = content,
    )
}
