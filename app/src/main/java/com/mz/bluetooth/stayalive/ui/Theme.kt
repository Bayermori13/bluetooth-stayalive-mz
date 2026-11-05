package com.mz.bluetooth.stayalive.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF070B14)
val CardDark = Color(0xFF10192C)
val CardBorder = Color(0xFF1E2D4A)
val NeonCyan = Color(0xFF00E5FF)
val NeonTeal = Color(0xFF00FFC4)
val NeonRed = Color(0xFFFF3366)
val TextMuted = Color(0xFF8B9CB5)
val TextWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonTeal,
    background = BgDark,
    surface = CardDark,
    onPrimary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun MZBluetoothTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
