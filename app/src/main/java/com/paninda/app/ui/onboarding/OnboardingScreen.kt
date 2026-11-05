package com.paninda.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paninda.app.R
import com.paninda.app.ui.theme.PanindaTheme

private val BrandFont = FontFamily(Font(R.font.ibm_plex_serif_italic, style = FontStyle.Italic))
private val PromptFont = FontFamily(Font(R.font.ibm_plex_sans_regular))
private val ButtonFont = FontFamily(Font(R.font.roboto_regular))
private val GoogleBlue = Color(0xFF0B63D7)
private val LockedGray = Color(0xFF989898)
private val HeroGradient = Brush.verticalGradient(
    0f to Color(0xFF63ADC8),
    0.44231f to Color(0xFF63ADC8),
    1f to Color(0xFF0084FF),
)

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onContinue: () -> Unit = {}) {
    BoxWithConstraints(modifier.fillMaxSize().background(Color.White)) {
        val scale = (maxWidth / 412.dp).coerceIn(0.75f, 1f)
        val fontScale = LocalDensity.current.fontScale
        val minimumHeroHeight = 320.dp * (fontScale * scale).coerceAtLeast(1f)
        val heroHeight = minOf(
            maxHeight * (636f / 917f),
            maxHeight - 242.dp * fontScale.coerceAtLeast(1f),
        ).coerceAtLeast(minimumHeroHeight)
        val buttonWidth = (maxWidth - 48.dp).coerceIn(0.dp, 317.dp)
        val labelSize = (20f * scale).coerceAtLeast(16f).sp

        // The form can grow and scroll on short screens or with larger system text.
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(heroHeight).background(HeroGradient),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 39.dp * scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp * scale),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_brand),
                        color = Color.White,
                        fontFamily = BrandFont,
                        fontStyle = FontStyle.Italic,
                        fontSize = (64f * scale).sp,
                        letterSpacing = 0.sp,
                        lineHeight = (83f * scale).sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_tagline),
                        color = Color.White,
                        fontFamily = BrandFont,
                        fontStyle = FontStyle.Italic,
                        fontSize = labelSize,
                        letterSpacing = 0.sp,
                        lineHeight = (28f * scale).sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(top = 14.dp, bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_choose_source),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Color.Black,
                    fontFamily = PromptFont,
                    fontSize = labelSize,
                    letterSpacing = 0.sp,
                    lineHeight = (28f * scale).sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(19.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.width(buttonWidth).heightIn(min = 54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoogleBlue,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Reserved for the Google logo once the asset source is available.
                    Box(
                        modifier = Modifier.size(30.dp).background(Color.White, CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.onboarding_google_sheets),
                        modifier = Modifier.weight(1f),
                        fontFamily = ButtonFont,
                        fontSize = labelSize,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.width(buttonWidth).heightIn(min = 54.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = LockedGray,
                        disabledContentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_custom_database),
                        fontFamily = ButtonFont,
                        fontSize = labelSize,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(name = "Figma portrait", widthDp = 412, heightDp = 917)
@Preview(name = "Compact portrait", widthDp = 360, heightDp = 640)
@Preview(name = "Large text", widthDp = 360, heightDp = 800, fontScale = 1.5f)
@Composable
private fun OnboardingPreview() {
    PanindaTheme(darkTheme = false) {
        OnboardingScreen()
    }
}
