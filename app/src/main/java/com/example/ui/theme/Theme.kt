package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AudiophilesColorScheme = darkColorScheme(
    primary = PurpleNeon,
    onPrimary = Color.White,
    primaryContainer = CosmicBorder,
    onPrimaryContainer = TextPrimary,
    secondary = GlowNeon,
    onSecondary = Color.White,
    secondaryContainer = CosmicCard,
    onSecondaryContainer = TextPrimary,
    tertiary = CyanAccent,
    onTertiary = CosmicBackground,
    background = CosmicBackground,
    onBackground = TextPrimary,
    surface = CosmicSurface,
    onSurface = TextPrimary,
    surfaceVariant = CosmicCard,
    onSurfaceVariant = TextSecondary,
    outline = CosmicBorder,
    outlineVariant = CosmicBorderLight
)

@Composable
fun AudiophilesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AudiophilesColorScheme,
        typography = Typography,
        content = content
    )
}
