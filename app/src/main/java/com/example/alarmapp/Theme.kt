package com.example.alarmapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark palette (also used by the always-dark ringing screen)
val Night = Color(0xFF0B0F1A)
val NightRaised = Color(0xFF141A2B)
val NightHigh = Color(0xFF1F2742)
val Amber = Color(0xFFFFB547)
val AmberDeep = Color(0xFF3A2708)
val OnAmber = Color(0xFF1C1200)
val TextHigh = Color(0xFFF2F4FA)
val TextDim = Color(0xFF9BA6C4)

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = OnAmber,
    primaryContainer = AmberDeep,
    onPrimaryContainer = Amber,
    secondary = Color(0xFF8FA6FF),
    onSecondary = Night,
    background = Night,
    onBackground = TextHigh,
    surface = Night,
    onSurface = TextHigh,
    surfaceVariant = NightRaised,
    onSurfaceVariant = TextDim,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHigh,
    outline = Color(0xFF3A4466),
    outlineVariant = Color(0xFF262E4A),
    error = Color(0xFFFF6B6B),
)

// Light palette: warm paper background, deep amber accent (readable on light surfaces)
private val LightScheme = lightColorScheme(
    primary = Color(0xFFB35F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3B8),
    onPrimaryContainer = Color(0xFF6B3A00),
    secondary = Color(0xFF4A5FC1),
    onSecondary = Color.White,
    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFF2ECE0),
    onSurfaceVariant = Color(0xFF6B665B),
    surfaceContainer = Color(0xFFF2ECE0),
    surfaceContainerHigh = Color(0xFFE9E1D2),
    surfaceContainerHighest = Color(0xFFE2D9C7),
    outline = Color(0xFFB9B1A1),
    outlineVariant = Color(0xFFDDD5C5),
    error = Color(0xFFBA1A1A),
)

@Composable
fun AlarmTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
