package com.example.alarmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val repo = AlarmRepository(context)
        val alarm = repo.get(id) ?: return

        // One-time alarms switch off after ringing; repeating ones queue their next occurrence.
        if (alarm.repeatDays.isEmpty()) {
            repo.save(alarm.copy(enabled = false))
        } else {
            AlarmScheduler.schedule(context, alarm)
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java).putExtra(AlarmScheduler.EXTRA_ALARM_ID, id),
        )
    }
}
