package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TikIAColorScheme = darkColorScheme(
    primary = TikIARedPrimary,
    onPrimary = Color.White,
    primaryContainer = TikIARedDark,
    onPrimaryContainer = Color.White,
    secondary = TikIARedVariant,
    onSecondary = Color.White,
    tertiary = TikIARedDark,
    onTertiary = Color.White,
    background = TikIABlackBackground,
    onBackground = TikIATextPrimary,
    surface = TikIABlackSurface,
    onSurface = TikIATextPrimary,
    surfaceVariant = TikIABlackSurfaceVariant,
    onSurfaceVariant = TikIATextSecondary,
    outline = TikIABorder,
    error = TikIARedPrimary,
    onError = Color.White
)

@Composable
fun TikIATheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TikIAColorScheme,
        typography = Typography,
        content = content
    )
}

// Kept for backward compatibility with template previews/tests
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    TikIATheme(content = content)
}

