package com.paninda.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.paninda.app.R

object Palette {
    val White = Color.White
    val Ink = Color.Black
    val Icon = Color(0xFF1E1E1E)
    val Blue = Color(0xFF0B63D7)
    val Link = Color(0xFF0062FF)
    val Card = Color(0xFFF3F8FD)
    val Section = Color(0xFF4A4A4A)
    val Navigation = Color.Black.copy(alpha = 0.04f)
    val Search = Color.Black.copy(alpha = 0.10f)
    val Hint = Color.Black.copy(alpha = 0.15f)
    val StockWarning = Color(0xFFE60000).copy(alpha = 0.28f)
    val ProductWarning = Color(0xFFE90000).copy(alpha = 0.09f)
    val Disabled = Color(0xFF161616).copy(alpha = 0.44f)
}

object AppFonts {
    val Serif = FontFamily(Font(R.font.ibm_plex_serif_italic, style = FontStyle.Italic))
    val Sans = FontFamily(Font(R.font.roboto_regular))
    val Plex = FontFamily(Font(R.font.ibm_plex_sans_regular))
    val Condensed = FontFamily(
        Font(R.font.roboto_condensed_thin, FontWeight.Thin),
        Font(R.font.roboto_condensed_light, FontWeight.Light),
        Font(R.font.roboto_condensed_regular, FontWeight.Normal),
    )
}
