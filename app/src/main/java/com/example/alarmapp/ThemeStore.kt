package com.example.alarmapp

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

/** Remembers whether the user wants the light, dark or phone-default look. */
object ThemeStore {
    var mode by mutableStateOf(ThemeMode.System)
        private set

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(context: Context) {
        val saved = prefs(context).getString("theme", null)
        mode = ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.System
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        prefs(context).edit().putString("theme", newMode.name).apply()
    }
}
