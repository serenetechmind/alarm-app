package com.example.alarmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmRepository(this).get(alarmId)

        ContextCompat.registerReceiver(
            this, stopReceiver, IntentFilter(AlarmService.ACTION_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            AlarmTheme {
                RingScreen(
                    alarm = alarm,
                    onSnooze = { send(AlarmService.ACTION_SNOOZE) },
                    onDismiss = { send(AlarmService.ACTION_DISMISS) },
                )
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

@Composable
private fun RingScreen(alarm: Alarm?, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val glow by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Column(
        Modifier.fillMaxSize().background(Night).systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(300.dp).scale(scale).clip(CircleShape).background(Amber.copy(alpha = glow)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(alarm?.hm() ?: "", fontSize = 76.sp, fontWeight = FontWeight.Light, color = TextHigh)
                    Text(" " + (alarm?.ampm() ?: ""), fontSize = 24.sp, color = TextDim, modifier = Modifier.padding(bottom = 14.dp))
                }
                Text(alarm?.label?.ifBlank { "Alarm" } ?: "Alarm", fontSize = 22.sp, color = Amber)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            shape = RoundedCornerShape(36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = OnAmber),
        ) { Text("Dismiss", fontSize = 20.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NightHigh, contentColor = TextHigh),
        ) { Text("Snooze ${alarm?.snoozeMinutes ?: 10} min", fontSize = 17.sp) }
        Spacer(Modifier.height(16.dp))
    }
}
