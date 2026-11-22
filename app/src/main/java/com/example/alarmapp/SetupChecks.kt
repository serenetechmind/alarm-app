package com.example.alarmapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

data class SetupItem(val id: String, val title: String, val detail: String)

/** Things the phone must allow for alarms to ring reliably (and over the lock screen). */
object SetupChecks {

    fun missing(c: Context): List<SetupItem> = buildList {
        val nm = c.getSystemService(NotificationManager::class.java)

        val notificationsOk = if (Build.VERSION.SDK_INT >= 33) {
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            nm.areNotificationsEnabled()
        }
        if (!notificationsOk) {
            add(SetupItem("notifications", "Allow notifications", "Needed to show a ringing alarm."))
        }

        if (Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent()) {
            add(SetupItem("fullscreen", "Allow full-screen alarms", "Lets the alarm appear over your lock screen."))
        }

        val pm = c.getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(c.packageName)) {
            add(SetupItem("battery", "Remove battery limits", "Stops your phone from silencing the alarm."))
        }
    }

    fun fix(c: Context, id: String) {
        val pkg = Uri.parse("package:${c.packageName}")
        val intent: Intent? = when (id) {
            "notifications" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, c.packageName)
            "fullscreen" ->
                if (Build.VERSION.SDK_INT >= 34) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg) else null
            "battery" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
            else -> null
        }
        if (intent == null) return
        try {
            c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: Exception) {
            }
        }
    }
}
