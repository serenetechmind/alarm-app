package com.example.alarmapp

import org.json.JSONArray
import org.json.JSONObject

val DAY_LETTERS = listOf("S", "M", "T", "W", "T", "F", "S")
val DAY_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/** repeatDays uses Calendar.DAY_OF_WEEK values: 1 = Sunday ... 7 = Saturday. Empty = one-time alarm. */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<Int> = emptySet(),
    val label: String = "",
    val enabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val sourceApp: String? = null, // set when the alarm was created through the API
    val categoryId: Int? = null, // to-do category shown when this alarm rings
    val taskId: Int? = null, // the task this alarm is for (highlighted on the ring screen)
) {
    fun timeLabel(): String {
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        val suffix = if (hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(h12, minute, suffix)
    }

    fun daysLabel(): String = when {
        repeatDays.isEmpty() -> "Once"
        repeatDays.size == 7 -> "Every day"
        else -> repeatDays.sorted().joinToString(" ") { DAY_NAMES[it - 1] }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("hour", hour)
        put("minute", minute)
        put("repeatDays", JSONArray(repeatDays.toList()))
        put("label", label)
        put("enabled", enabled)
        put("snoozeMinutes", snoozeMinutes)
        put("sourceApp", sourceApp ?: JSONObject.NULL)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("taskId", taskId ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): Alarm {
            val arr = o.getJSONArray("repeatDays")
            return Alarm(
                id = o.getInt("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                repeatDays = (0 until arr.length()).map { arr.getInt(it) }.toSet(),
                label = o.getString("label"),
                enabled = o.getBoolean("enabled"),
                snoozeMinutes = o.getInt("snoozeMinutes"),
                sourceApp = if (o.isNull("sourceApp")) null else o.getString("sourceApp"),
                categoryId = if (o.isNull("categoryId")) null else o.getInt("categoryId"),
                taskId = if (o.isNull("taskId")) null else o.getInt("taskId"),
            )
        }
    }
}
