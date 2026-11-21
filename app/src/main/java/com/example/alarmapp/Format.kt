package com.example.alarmapp

fun Alarm.hm(): String = "%d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)

fun Alarm.ampm(): String = if (hour < 12) "AM" else "PM"

/** "6h 20m", "2d 3h", "12m". */
fun formatCountdown(ms: Long): String {
    val totalMin = ((ms + 59_999L) / 60_000L).coerceAtLeast(0L)
    val d = totalMin / 1440
    val h = (totalMin % 1440) / 60
    val m = totalMin % 60
    return when {
        d > 0 -> "${d}d ${h}h"
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "less than a minute"
    }
}
