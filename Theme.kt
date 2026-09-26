package com.aditya.wakey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun WakeyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
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
