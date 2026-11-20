package com.example.alarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "alarm_id"

    /** Next time this alarm should ring, or null if it has none. */
    fun nextTrigger(alarm: Alarm): Long? {
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayOk = alarm.repeatDays.isEmpty() || c.get(Calendar.DAY_OF_WEEK) in alarm.repeatDays
            if (c.timeInMillis > now && dayOk) return c.timeInMillis
        }
        return null
    }

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(context, alarm.id)
            return
        }
        val at = nextTrigger(alarm) ?: return
        setAt(context, alarm.id, at, requestCode = alarm.id)
    }

    fun scheduleSnooze(context: Context, alarm: Alarm) {
        val at = System.currentTimeMillis() + alarm.snoozeMinutes * 60_000L
        setAt(context, alarm.id, at, requestCode = -alarm.id)
    }

    fun cancel(context: Context, alarmId: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pending(context, alarmId, alarmId))
        am.cancel(pending(context, alarmId, -alarmId))
    }

    fun rescheduleAll(context: Context) {
        AlarmRepository(context).getAll().forEach { schedule(context, it) }
    }

    // setAlarmClock is the alarm-clock API: exact, survives Doze, and needs no exact-alarm permission.
    private fun setAt(context: Context, alarmId: Int, triggerAt: Long, requestCode: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, show),
            pending(context, alarmId, requestCode),
        )
    }

    private fun pending(context: Context, alarmId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
