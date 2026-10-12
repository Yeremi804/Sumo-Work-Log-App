package com.resu.sumo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueSecondary,
    onPrimary = NavyDark,
    primaryContainer = NavyDarkContainer,
    onPrimaryContainer = NavyLightContainer,
    secondary = EmeraldTertiary,
    onSecondary = NavyDark,
    secondaryContainer = EmeraldTertiary,
    tertiary = AmberAccent
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyLightContainer,
    onPrimaryContainer = NavyPrimary,
    secondary = SkyBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = SkyBlueLightContainer,
    onSecondaryContainer = NavyPrimary,
    tertiary = EmeraldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLightContainer,
    onTertiaryContainer = EmeraldTertiary
)

@Composable
fun SumoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
