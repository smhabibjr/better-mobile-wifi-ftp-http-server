package com.smhabibjr.betterwififtp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = AppAccent,
    onPrimary = Color(0xFF031018),
    primaryContainer = AppAccentSoft,
    onPrimaryContainer = AppAccent2,
    secondary = AppAccent2,
    background = AppBg,
    surface = AppCard,
    surfaceVariant = AppCard2,
    onBackground = AppText,
    onSurface = AppText,
    onSurfaceVariant = AppTextDim,
    outline = AppBorder,
    outlineVariant = AppBorderStrong,
)

@Composable
fun BetterWiFiFTPTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
