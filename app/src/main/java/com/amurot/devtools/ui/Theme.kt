package com.amurot.devtools.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// "Terminal green" brand: dark-first, one accent.
private val Dark = darkColorScheme(
    primary = Color(0xFF39D98A),
    onPrimary = Color(0xFF04210F),
    primaryContainer = Color(0xFF0F3D26),
    onPrimaryContainer = Color(0xFFB7F5D2),
    secondary = Color(0xFF8FD4AE),
    onSecondary = Color(0xFF04210F),
    tertiary = Color(0xFFF2C46D),
    background = Color(0xFF0E1411),
    onBackground = Color(0xFFDDE6E0),
    surface = Color(0xFF0E1411),
    onSurface = Color(0xFFDDE6E0),
    surfaceVariant = Color(0xFF1D2622),
    onSurfaceVariant = Color(0xFFA9B8AF),
    surfaceContainerLowest = Color(0xFF090E0B),
    surfaceContainerLow = Color(0xFF141B17),
    surfaceContainer = Color(0xFF18211C),
    surfaceContainerHigh = Color(0xFF1F2924),
    surfaceContainerHighest = Color(0xFF26312B),
    outline = Color(0xFF55645B),
    outlineVariant = Color(0xFF2E3A33),
    error = Color(0xFFFF8A80),
)

private val Light = lightColorScheme(
    primary = Color(0xFF1F9D63),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4F2D9),
    onPrimaryContainer = Color(0xFF00391F),
    secondary = Color(0xFF3E6A53),
    tertiary = Color(0xFF8A6100),
    background = Color(0xFFF4F8F5),
    onBackground = Color(0xFF151D18),
    surface = Color(0xFFF4F8F5),
    onSurface = Color(0xFF151D18),
    surfaceVariant = Color(0xFFE2EAE5),
    onSurfaceVariant = Color(0xFF46544B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEDF3EF),
    surfaceContainer = Color(0xFFE7EEE9),
    surfaceContainerHigh = Color(0xFFE1E9E4),
    surfaceContainerHighest = Color(0xFFDBE4DE),
    outline = Color(0xFF76857C),
    outlineVariant = Color(0xFFC5D0C9),
)

private val DevShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun DevToolsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, shapes = DevShapes, content = content)
}
