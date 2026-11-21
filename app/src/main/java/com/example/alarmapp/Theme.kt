package com.example.alarmapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0B0F1A)
val NightRaised = Color(0xFF141A2B)
val NightHigh = Color(0xFF1F2742)
val Amber = Color(0xFFFFB547)
val AmberDeep = Color(0xFF3A2708)
val OnAmber = Color(0xFF1C1200)
val TextHigh = Color(0xFFF2F4FA)
val TextDim = Color(0xFF9BA6C4)

private val NightScheme = darkColorScheme(
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

/** Night-sky look: near-black navy surfaces with a warm amber accent, like a sunrise. */
@Composable
fun AlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NightScheme, content = content)
}
