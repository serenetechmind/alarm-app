package com.example.alarmapp

import android.content.Context

/** Small local store for this phone's identity with the Alarm API server. */
object DeviceStore {
    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("device", Context.MODE_PRIVATE)

    fun serverUrl(c: Context): String =
        prefs(c).getString("server_url", BuildConfig.API_BASE_URL)!!

    /** Changing the server means the old registration is meaningless, so it is cleared. */
    fun setServerUrl(c: Context, url: String) {
        prefs(c).edit()
            .putString("server_url", url.trim().trimEnd('/'))
            .remove("device_token")
            .remove("pair_code")
            .apply()
    }

    fun deviceToken(c: Context): String? = prefs(c).getString("device_token", null)
    fun pairCode(c: Context): String? = prefs(c).getString("pair_code", null)

    fun saveRegistration(c: Context, deviceToken: String, pairCode: String) {
        prefs(c).edit().putString("device_token", deviceToken).putString("pair_code", pairCode).apply()
    }

    fun fcmToken(c: Context): String? = prefs(c).getString("fcm_token", null)
    fun setFcmToken(c: Context, token: String) {
        prefs(c).edit().putString("fcm_token", token).apply()
    }
}
