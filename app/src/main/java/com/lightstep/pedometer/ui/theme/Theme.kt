package com.lightstep.pedometer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = StepGreen,
    onPrimary = Color.White,
    secondary = StepBlue,
    tertiary = StepOrange,
    background = StepBackground,
    surface = StepSurface,
    onBackground = StepInk,
    onSurface = StepInk,
    error = StepRed
)

private val DarkColors = darkColorScheme(
    primary = StepGreen,
    onPrimary = Color.White,
    secondary = StepBlue,
    tertiary = StepOrange,
    background = StepDarkBackground,
    surface = StepDarkSurface,
    onBackground = Color(0xFFE8F2ED),
    onSurface = Color(0xFFE8F2ED),
    error = Color(0xFFFF8A8C)
)

@Composable
fun LightStepTheme(
    themeMode: String,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
