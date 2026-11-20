package com.example.alarmapp

import android.content.Context
import org.json.JSONArray

/** Local storage so alarms keep working offline. The server (later) is only a sync source. */
class AlarmRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun getAll(): List<Alarm> {
        val arr = JSONArray(prefs.getString("list", "[]"))
        return (0 until arr.length())
            .map { Alarm.fromJson(arr.getJSONObject(it)) }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun get(id: Int): Alarm? = getAll().find { it.id == id }

    /** Pass id = 0 to create a new alarm. Returns the saved alarm with its id. */
    fun save(alarm: Alarm): Alarm {
        val saved = if (alarm.id == 0) alarm.copy(id = nextId()) else alarm
        persist(getAll().filter { it.id != saved.id } + saved)
        return saved
    }

    fun delete(id: Int) = persist(getAll().filter { it.id != id })

    private fun nextId(): Int {
        val n = prefs.getInt("next_id", 1)
        prefs.edit().putInt("next_id", n + 1).apply()
        return n
    }

    private fun persist(list: List<Alarm>) {
        prefs.edit().putString("list", JSONArray(list.map { it.toJson() }).toString()).apply()
    }
}
