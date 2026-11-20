package com.example.alarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1

        when (intent?.action) {
            ACTION_SNOOZE -> {
                AlarmRepository(this).get(id)?.let { AlarmScheduler.scheduleSnooze(this, it) }
                report(id, "snoozed")
                stopRinging()
                return START_NOT_STICKY
            }
            ACTION_DISMISS -> {
                report(id, "dismissed")
                stopRinging()
                return START_NOT_STICKY
            }
        }

        val alarm = AlarmRepository(this).get(id)
        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createChannel()
        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(alarm),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        startRinging()
        report(id, "fired")
        return START_NOT_STICKY
    }

    /** Tells the server (and so the developer's webhook) what happened. Only for alarms made via the API. */
    private fun report(alarmId: Int, type: String) {
        if (alarmId < AlarmSync.SERVER_ID_OFFSET) return
        val app = applicationContext
        Thread {
            runCatching { ApiClient.sendEvent(app, alarmId - AlarmSync.SERVER_ID_OFFSET, type) }
        }.start()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH)
            .apply { setSound(null, null) } // the service plays the sound itself
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(alarm: Alarm) = run {
        val ringIntent = Intent(this, RingActivity::class.java)
            .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fullScreen = PendingIntent.getActivity(
            this, alarm.id, ringIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarm.label.ifBlank { "Alarm" })
            .setContentText(alarm.timeLabel())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .addAction(0, "Snooze", serviceIntent(ACTION_SNOOZE, alarm.id))
            .addAction(0, "Dismiss", serviceIntent(ACTION_DISMISS, alarm.id))
            .build()
    }

    private fun serviceIntent(action: String, alarmId: Int): PendingIntent =
        PendingIntent.getService(
            this, alarmId,
            Intent(this, AlarmService::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun startRinging() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, uri)
            isLooping = true
            prepare()
            start()
        }
        vibrator = getSystemService(Vibrator::class.java)?.also {
            it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
        }
    }

    private fun stopRinging() {
        player?.run { if (isPlaying) stop(); release() }
        player = null
        vibrator?.cancel()
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName)) // tells RingActivity to close
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        player?.release()
        vibrator?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.alarmapp.SNOOZE"
        const val ACTION_DISMISS = "com.example.alarmapp.DISMISS"
        const val ACTION_STOPPED = "com.example.alarmapp.STOPPED"
        private const val CHANNEL_ID = "alarms"
        private const val NOTIF_ID = 1
    }
}
