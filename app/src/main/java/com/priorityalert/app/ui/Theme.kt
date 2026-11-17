package com.priorityalert.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette (same colors as the launcher icon).
val Navy = Color(0xFF1B1F3B)
val NavyDeep = Color(0xFF12152B)
val NavyLight = Color(0xFF2B3163)
val Coral = Color(0xFFFF6B5B)
val Amber = Color(0xFFFBBF24)
val Mint = Color(0xFF34D399)

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E0),
    onPrimaryContainer = Color(0xFF5C1A12),
    secondary = Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E0),
    onSecondaryContainer = Color(0xFF5C1A12),
    background = Color(0xFFF5F6FA),
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFECEDF4),
    onSurfaceVariant = Color(0xFF5B6080),
    outline = Color(0xFFD5D7E3),
    outlineVariant = Color(0xFFE6E7EF),
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C2620),
    onPrimaryContainer = Color(0xFFFFD9D3),
    secondary = Color(0xFFC7CBEA),
    onSecondary = Navy,
    secondaryContainer = Color(0xFF4A2A30),
    onSecondaryContainer = Color(0xFFFFD9D3),
    background = NavyDeep,
    onBackground = Color(0xFFE8E9F3),
    surface = Color(0xFF1E2244),
    onSurface = Color(0xFFE8E9F3),
    surfaceVariant = Color(0xFF262B52),
    onSurfaceVariant = Color(0xFFA3A8C8),
    outline = Color(0xFF3A4070),
    outlineVariant = Color(0xFF2E3360),
    error = Color(0xFFF87171),
)

@Composable
fun PriorityTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
