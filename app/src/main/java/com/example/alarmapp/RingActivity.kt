package com.example.alarmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class RingActivity : ComponentActivity() {

    private var alarmId = -1

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmRepository(this).get(alarmId)

        ContextCompat.registerReceiver(
            this, stopReceiver, IntentFilter(AlarmService.ACTION_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(alarm?.timeLabel() ?: "", fontSize = 56.sp)
                        Text(alarm?.label?.ifBlank { "Alarm" } ?: "Alarm", fontSize = 22.sp)
                        Spacer(Modifier.height(64.dp))
                        Button(onClick = { send(AlarmService.ACTION_SNOOZE) }, Modifier.fillMaxWidth().height(64.dp)) {
                            Text("Snooze", fontSize = 20.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { send(AlarmService.ACTION_DISMISS) }, Modifier.fillMaxWidth().height(64.dp)) {
                            Text("Dismiss", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }

    private fun send(action: String) {
        startService(
            Intent(this, AlarmService::class.java)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        )
        finish()
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        super.onDestroy()
    }
}
