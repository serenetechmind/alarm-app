package com.example.alarmapp

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Holds a "Connect with AlarmApp" request opened from a link or QR code (alarmapp://connect/<id>). */
object ConnectRequests {
    var pendingId by mutableStateOf<String?>(null)

    fun handle(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "alarmapp" && uri.host == "connect") {
            pendingId = uri.lastPathSegment
        }
    }
}
