package com.skripsi.cnnfreshscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FreshPrimary,
    secondary = FreshSecondary,
    tertiary = FreshAccentBlue,
    background = Color.Black,
    surface = CameraGlass,
    surfaceVariant = CameraGlassSoft,
    onPrimary = FreshSurface,
    onSecondary = FreshTextPrimary,
    onTertiary = FreshSurface,
    onBackground = FreshSurface,
    onSurface = FreshSurface,
    onSurfaceVariant = FreshSurfaceTint,
    errorContainer = FreshErrorContainer,
    onErrorContainer = FreshOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = FreshPrimary,
    secondary = FreshSecondary,
    tertiary = FreshAccentBlue,
    background = FreshBackground,
    surface = FreshSurface,
    surfaceVariant = FreshSurfaceMuted,
    primaryContainer = FreshSurfaceTint,
    secondaryContainer = FreshSurfaceTint,
    onPrimary = FreshSurface,
    onSecondary = FreshTextPrimary,
    onTertiary = FreshSurface,
    onBackground = FreshTextPrimary,
    onSurface = FreshTextPrimary,
    onSurfaceVariant = FreshTextSecondary,
    onPrimaryContainer = FreshTextPrimary,
    onSecondaryContainer = FreshTextPrimary,
    outline = FreshBorder,
    errorContainer = FreshErrorContainer,
    onErrorContainer = FreshOnErrorContainer
)

@Suppress("UNUSED_PARAMETER")
@Composable
fun CnnFreshScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}