package com.neo.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = AccentPrimary,
    onPrimary        = Color.White,
    background       = SpatialBg,
    onBackground     = TextPrimary,
    surface          = SpatialSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = SpatialSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline          = BorderDark,
)

@Composable
fun NeoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content,
    )
}