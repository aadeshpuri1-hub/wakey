package com.aditya.wakey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aditya.wakey.R

/** Wakey palette: near-black, soft cards, coral-red accent (Alarmy vibes). */
object W {
    val Bg = Color(0xFF111217)
    val Card = Color(0xFF1C1D24)
    val Card2 = Color(0xFF282A35)
    val Accent = Color(0xFFFF4D5A)
    val AccentDim = Color(0x33FF4D5A)
    val Text2 = Color(0xFF9A9CA8)
    val Good = Color(0xFF3DDC84)
    val Warn = Color(0xFFFFB020)
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Big clock digits: display cut, even-width numbers so they don't jiggle, tight tracking. */
val ClockStyle = TextStyle(
    fontFamily = FontFamily(Font(R.font.inter_display_bold, FontWeight.Bold)),
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
    letterSpacing = (-1.5).sp,
)

private fun TextStyle.inter() = copy(fontFamily = Inter, fontFeatureSettings = "tnum")

private val WakeyType = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.inter(),
        displayMedium = t.displayMedium.inter(),
        displaySmall = t.displaySmall.inter(),
        headlineLarge = t.headlineLarge.inter(),
        headlineMedium = t.headlineMedium.inter(),
        headlineSmall = t.headlineSmall.inter(),
        titleLarge = t.titleLarge.inter(),
        titleMedium = t.titleMedium.inter(),
        titleSmall = t.titleSmall.inter(),
        bodyLarge = t.bodyLarge.inter(),
        bodyMedium = t.bodyMedium.inter(),
        bodySmall = t.bodySmall.inter(),
        labelLarge = t.labelLarge.inter(),
        labelMedium = t.labelMedium.inter(),
        labelSmall = t.labelSmall.inter(),
    )
}

@Composable
fun WakeyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = WakeyType,
        colorScheme = darkColorScheme(
            primary = W.Accent,
            onPrimary = Color.White,
            secondary = W.Accent,
            background = W.Bg,
            onBackground = Color.White,
            surface = W.Bg,
            onSurface = Color.White,
            surfaceVariant = W.Card,
            onSurfaceVariant = W.Text2,
            surfaceContainer = Color(0xFF17181E),
            surfaceContainerHigh = W.Card,
            surfaceContainerHighest = W.Card2,
            secondaryContainer = W.AccentDim,
            onSecondaryContainer = Color.White,
            outline = W.Card2,
        ),
        content = content,
    )
}
