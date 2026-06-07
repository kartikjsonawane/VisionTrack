package com.visiontrack.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// VisionTrack brand palette — deep navy + electric blue + amber accent
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF4FC3F7),   // Electric Blue
    onPrimary        = Color(0xFF0A0E1A),
    primaryContainer = Color(0xFF0D47A1),
    secondary        = Color(0xFFFFB74D),   // Amber
    onSecondary      = Color(0xFF0A0E1A),
    tertiary         = Color(0xFF80CBC4),
    background       = Color(0xFF0A0E1A),   // Deep Navy
    surface          = Color(0xFF12172A),
    onSurface        = Color(0xFFE0E6F5),
    surfaceVariant   = Color(0xFF1A2035),
    error            = Color(0xFFEF5350),
    onError          = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF0288D1),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFB3E5FC),
    secondary        = Color(0xFFF57F17),
    onSecondary      = Color.White,
    background       = Color(0xFFF5F7FA),
    surface          = Color.White,
    onSurface        = Color(0xFF0A0E1A)
)

@Composable
fun VisionTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
