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

/** Monochrome palette: jet black, grey-black surfaces, white as the only accent. */
object W {
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF111111)
    val Card2 = Color(0xFF1C1C1C)
    val Line = Color(0xFF242424)
    val Text = Color(0xFFFFFFFF)
    val Text2 = Color(0xFF8C8C8C)
    val Text3 = Color(0xFF4D4D4D)
    val Accent = Color(0xFFFFFFFF)
    val OnAccent = Color(0xFF000000)
    val AccentDim = Color(0x14FFFFFF)
    val Good = Color(0xFFFFFFFF)
    val Warn = Color(0xFFBFBFBF)
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Big clock digits: display cut, even-width numbers, tight tracking. */
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
            onPrimary = W.OnAccent,
            secondary = W.Accent,
            onSecondary = W.OnAccent,
            background = W.Bg,
            onBackground = W.Text,
            surface = W.Bg,
            onSurface = W.Text,
            surfaceVariant = W.Card,
            onSurfaceVariant = W.Text2,
            surfaceContainerLow = W.Card,
            surfaceContainer = W.Card,
            surfaceContainerHigh = W.Card,
            surfaceContainerHighest = W.Card2,
            secondaryContainer = W.Card2,
            onSecondaryContainer = W.Text,
            outline = W.Line,
            outlineVariant = W.Line,
        ),
        content = content,
    )
}
