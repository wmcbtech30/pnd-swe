package com.paninda.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Colors = lightColorScheme(
    primary = Palette.Blue, onPrimary = Palette.White,
    primaryContainer = Palette.Card, onPrimaryContainer = Palette.Ink,
    secondary = Palette.Blue, onSecondary = Palette.White,
    secondaryContainer = Palette.Card, onSecondaryContainer = Palette.Ink,
    tertiary = Palette.Blue, onTertiary = Palette.White,
    tertiaryContainer = Palette.Card, onTertiaryContainer = Palette.Ink,
    background = Palette.White, onBackground = Palette.Ink,
    surface = Palette.White, onSurface = Palette.Ink,
    surfaceVariant = Palette.Card, onSurfaceVariant = Palette.Section,
    surfaceTint = Palette.White, outline = Palette.Section, outlineVariant = Palette.Search,
    error = Palette.Ink, onError = Palette.White,
    errorContainer = Palette.ProductWarning, onErrorContainer = Palette.Ink,
    inverseSurface = Palette.Icon, inverseOnSurface = Palette.White, inversePrimary = Palette.White,
    scrim = Palette.Ink, surfaceBright = Palette.White, surfaceDim = Palette.Navigation,
    surfaceContainer = Palette.Card, surfaceContainerHigh = Palette.Card,
    surfaceContainerHighest = Palette.Card, surfaceContainerLow = Palette.White,
    surfaceContainerLowest = Palette.White,
)
private val Body = TextStyle(fontFamily = AppFonts.Sans, fontWeight = FontWeight.Normal,
    fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.sp)
private val AppTypography = Typography(
    bodyLarge = Body, bodyMedium = Body.copy(fontSize = 16.sp),
    bodySmall = Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = Body, labelMedium = Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = Body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    titleLarge = Body.copy(fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = Body, titleSmall = Body.copy(fontSize = 16.sp),
)

@Composable
fun PanindaTheme(@Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // The supplied design defines a light palette only; system dark mode must not add new colors.
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
