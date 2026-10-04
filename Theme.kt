package app.upwake.ui.theme

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
import app.upwake.R

/**
 * Upwake palette, modelled on Apple's Clock app in dark mode: true black, iOS grouped-card
 * greys, white type, iOS system orange as the accent, and iOS green / red for start & stop.
 */
object W {
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)      // iOS secondarySystemGroupedBackground
    val Card2 = Color(0xFF2C2C2E)     // iOS tertiary fill
    val Line = Color(0xFF38383A)      // iOS separator
    val Text = Color(0xFFFFFFFF)
    val Text2 = Color(0xFF8E8E93)     // iOS secondaryLabel
    val Text3 = Color(0xFF636366)     // iOS systemGray2
    val Accent = Color(0xFFFFFFFF)
    val OnAccent = Color(0xFF000000)
    val AccentDim = Color(0x14FFFFFF)
    val Good = Color(0xFF30D158)
    val Warn = Color(0xFFBFBFBF)

    /** iOS system orange, the Clock app's tint. */
    val Dawn = Color(0xFFFF9F0A)
    val DawnDim = Color(0x33FF9F0A)
    val DawnGlow = Color(0x59FF9F0A)
    val OnDawn = Color(0xFF000000)

    /** iOS green / red, used for switches and Start / Stop. */
    val Green = Color(0xFF30D158)
    val GreenDim = Color(0x3330D158)
    val Red = Color(0xFFFF453A)
    val RedDim = Color(0x33FF453A)
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

private val UpwakeType = Typography().let { t ->
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
fun UpwakeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = UpwakeType,
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
