package com.github.shiroedev2024.leaf.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        secondary = DarkSecondary,
        tertiary = DarkTertiary,
        background = DarkBackground,
        surface = DarkSurface,
        onPrimary = DarkOnPrimary,
        onSecondary = DarkOnSecondary,
        onTertiary = DarkOnTertiary,
        onBackground = DarkOnBackground,
        onSurface = DarkOnSurface,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        secondary = LightSecondary,
        tertiary = LightTertiary,
        background = LightBackground,
        surface = LightSurface,
        onPrimary = LightOnPrimary,
        onSecondary = LightOnSecondary,
        onTertiary = LightOnTertiary,
        onBackground = LightOnBackground,
        onSurface = LightOnSurface,
    )

@Composable
fun LeafAndroidTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
