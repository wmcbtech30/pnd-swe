package com.paninda.app.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.ui.theme.*

@Composable
internal fun ReportCard(title: String, value: String, modifier: Modifier = Modifier,
    warning: Boolean = false, note: String? = null) {
    Surface(modifier, shape = RoundedCornerShape(11.dp), color = if (warning) Palette.StockWarning else Palette.Card) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontFamily = AppFonts.Serif, fontStyle = FontStyle.Italic, fontSize = 16.sp)
            Text(value, fontFamily = AppFonts.Condensed, fontWeight = FontWeight.Thin, fontSize = 40.sp, lineHeight = 48.sp)
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = Palette.Section)
        }
    }
}
