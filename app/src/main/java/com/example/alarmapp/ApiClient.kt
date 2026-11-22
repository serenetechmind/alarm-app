package com.example.alarmapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import java.util.UUID

/** Minimal client for the Alarm API. All calls are blocking: run them off the main thread. */
object ApiClient {

    private fun request(
        context: Context,
        method: String,
        path: String,
        body: JSONObject? = null,
        auth: Boolean = true,
    ): String {
        // Read the token once, up front. If it is gone (cleared mid-sync), stop instead of sending a headerless request.
        val token = if (auth) DeviceStore.deviceToken(context) ?: throw UnauthorizedException() else null
        val conn = URL(DeviceStore.serverUrl(context) + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 70_000
            conn.setRequestProperty("Content-Type", "application/json")
            if (token != null) conn.setRequestProperty("X-Device-Token", token)
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code == 401 && auth) throw UnauthorizedException()
            if (code !in 200..299) throw java.io.IOException("HTTP $code $text")
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun objects(json: String): List<JSONObject> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    fun ensureRegistered(context: Context) {
        if (DeviceStore.deviceToken(context) != null) return
        val body = JSONObject()
            .put("fcm_token", DeviceStore.fcmToken(context) ?: "pending-${UUID.randomUUID()}")
            .put("timezone", TimeZone.getDefault().id)
        val res = JSONObject(request(context, "POST", "/v1/device/register", body, auth = false))
        DeviceStore.saveRegistration(context, res.getString("device_token"), res.getString("pair_code"))
    }

    fun updateFcmToken(context: Context, token: String) {
        request(context, "PUT", "/v1/device/fcm-token", JSONObject().put("fcm_token", token))
    }

    fun fetchAlarms(context: Context): List<JSONObject> =
        objects(request(context, "GET", "/v1/device/alarms"))

    fun fetchConnections(context: Context): List<JSONObject> =
        objects(request(context, "GET", "/v1/device/connections"))

    fun revokeConnection(context: Context, connectionId: Int) {
        request(context, "DELETE", "/v1/device/connections/$connectionId")
    }

    fun sendEvent(context: Context, serverAlarmId: Int, type: String) {
        request(
            context, "POST", "/v1/device/events",
            JSONObject().put("alarm_id", serverAlarmId).put("type", type),
        )
    }
}

/** The server no longer recognises this phone (for example after a database reset). */
class UnauthorizedException : java.io.IOException("Server no longer recognises this phone")
