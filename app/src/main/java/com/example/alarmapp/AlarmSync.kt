package com.example.alarmapp

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

/**
 * Keeps local alarms in step with the server.
 * Server alarms are stored locally with id = SERVER_ID_OFFSET + serverId, so they can never
 * collide with alarms the user made by hand on the phone.
 */
object AlarmSync {
    const val SERVER_ID_OFFSET = 1_000_000

    /** Blocking. Registers the phone if needed, then mirrors the server's alarm list. */
    private val syncLock = Any()

    fun fullSync(context: Context) {
        // Several screens can ask for a sync at once; running them one at a time avoids duplicate
        // registrations and a sync losing its token halfway through.
        synchronized(syncLock) {
            try {
                syncOnce(context)
            } catch (e: UnauthorizedException) {
                DeviceStore.clearRegistration(context) // register again with a fresh pair code
                syncOnce(context)
            }
        }
    }

    private fun syncOnce(context: Context) {
        ApiClient.ensureRegistered(context)
        DeviceStore.fcmToken(context)?.let { ApiClient.updateFcmToken(context, it) }

        val remote = ApiClient.fetchAlarms(context)
        val remoteIds = remote.map { SERVER_ID_OFFSET + it.getInt("id") }.toSet()
        val repo = AlarmRepository(context)

        repo.getAll()
            .filter { it.id >= SERVER_ID_OFFSET && it.id !in remoteIds }
            .forEach {
                AlarmScheduler.cancel(context, it.id)
                repo.delete(it.id)
            }
        remote.forEach { upsert(context, it) }
    }

    /** Applies one alarm JSON from the server (from a push or a full sync). */
    fun upsert(context: Context, o: JSONObject) {
        val days = o.getJSONArray("repeat_days")
        val id = SERVER_ID_OFFSET + o.getInt("id")
        val repo = AlarmRepository(context)
        val existing = repo.get(id)

        var alarm = Alarm(
            id = id,
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            repeatDays = (0 until days.length()).map { days.getInt(it) }.toSet(),
            label = o.optString("label", ""),
            enabled = o.getBoolean("enabled"),
            snoozeMinutes = o.optInt("snooze_minutes", 10),
            sourceApp = o.optString("source_app").ifBlank { null },
        )

        // A one-time alarm that already rang stays off locally until its time is changed,
        // even if the server hasn't heard the "fired" event yet.
        if (existing != null && !existing.enabled && existing.repeatDays.isEmpty() &&
            existing.hour == alarm.hour && existing.minute == alarm.minute &&
            existing.repeatDays == alarm.repeatDays
        ) {
            alarm = alarm.copy(enabled = false)
        }

        repo.save(alarm)
        if (existing == alarm) return // nothing changed: leave any pending snooze alone
        if (alarm.enabled) AlarmScheduler.schedule(context, alarm) else AlarmScheduler.cancel(context, alarm.id)
    }

    fun delete(context: Context, serverId: Int) {
        val id = SERVER_ID_OFFSET + serverId
        AlarmScheduler.cancel(context, id)
        AlarmRepository(context).delete(id)
    }

    /** Asks Firebase for this phone's push token. Does nothing until google-services.json is added. */
    fun refreshFcmToken(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                DeviceStore.setFcmToken(context, token)
                Thread {
                    runCatching {
                        if (DeviceStore.deviceToken(context) != null) ApiClient.updateFcmToken(context, token)
                    }
                }.start()
            }
        } catch (_: Exception) {
        }
    }
}
