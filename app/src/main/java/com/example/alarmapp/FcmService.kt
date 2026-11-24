package com.example.alarmapp

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/** Receives pushes from the Alarm API server and applies them to the local alarms. */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        DeviceStore.setFcmToken(this, token)
        val app = applicationContext
        Thread {
            runCatching {
                if (DeviceStore.deviceToken(app) != null) ApiClient.updateFcmToken(app, token)
            }
        }.start()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = message.data["payload"]?.let { JSONObject(it) } ?: return
        when (message.data["type"]) {
            "alarm.upsert" -> AlarmSync.upsert(this, payload)
            "alarm.delete" -> AlarmSync.delete(this, payload.getInt("id"))
            "category.upsert" -> TodoSync.upsertCategory(this, payload)
            "category.delete" -> TodoSync.deleteCategoryLocal(this, AlarmSync.SERVER_ID_OFFSET + payload.getInt("id"))
            "task.upsert" -> TodoSync.upsertTask(this, payload)
            "task.delete" -> TodoSync.deleteTaskLocal(this, AlarmSync.SERVER_ID_OFFSET + payload.getInt("id"))
        }
    }
}
