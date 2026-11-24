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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
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
            AlarmTheme(dark = true) {
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
private fun PlainRing(alarm: Alarm?, onSnooze: () -> Unit, onDismiss: () -> Unit) {
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

/** Plain ring screen, or the to-do version when the alarm is linked to a category. */
@Composable
private fun RingScreen(alarm: Alarm?, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val categoryId = alarm?.categoryId
    val categoryName = remember(categoryId) { categoryId?.let { TodoRepository(context).category(it)?.name } }
    if (alarm != null && categoryId != null && categoryName != null) {
        TodoRing(alarm, categoryId, categoryName, onSnooze, onDismiss)
    } else {
        PlainRing(alarm, onSnooze, onDismiss)
    }
}

/**
 * Shows every task in the alarm's category, to do first, then done. Tasks are only ticked by the
 * user tapping them: the alarm ringing or being dismissed never changes them.
 */
@Composable
private fun TodoRing(alarm: Alarm, categoryId: Int, categoryName: String, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { TodoRepository(context) }
    var tasks by remember { mutableStateOf(repo.tasksIn(categoryId)) }
    val todo = tasks.filter { !it.done }
    val finished = tasks.filter { it.done }

    Column(Modifier.fillMaxSize().background(Night).systemBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(alarm.hm(), fontSize = 48.sp, fontWeight = FontWeight.Light, color = TextHigh)
            Text(" " + alarm.ampm(), fontSize = 18.sp, color = TextDim, modifier = Modifier.padding(bottom = 9.dp))
        }
        Text(alarm.label.ifBlank { "Alarm" }, fontSize = 16.sp, color = Amber)
        Spacer(Modifier.height(18.dp))
        Text(
            "$categoryName · ${finished.size} of ${tasks.size} done",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextHigh,
        )

        LazyColumn(Modifier.weight(1f)) {
            if (tasks.isEmpty()) {
                item { Text("No tasks in this category yet.", color = TextDim, modifier = Modifier.padding(top = 14.dp)) }
            }
            if (todo.isNotEmpty()) {
                item { SectionLabel("To do · ${todo.size}") }
                items(todo, key = { it.id }) { t ->
                    RingTaskRow(t, t.id == alarm.taskId) {
                        TodoSync.setDone(context, t, true)
                        tasks = repo.tasksIn(categoryId)
                    }
                }
            }
            if (finished.isNotEmpty()) {
                item { SectionLabel("Done · ${finished.size}") }
                items(finished, key = { it.id }) { t ->
                    RingTaskRow(t, t.id == alarm.taskId) {
                        TodoSync.setDone(context, t, false)
                        tasks = repo.tasksIn(categoryId)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = OnAmber),
        ) { Text("Dismiss", fontSize = 19.sp, fontWeight = FontWeight.Medium) }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onSnooze,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NightHigh, contentColor = TextHigh),
        ) { Text("Snooze ${alarm.snoozeMinutes} min", fontSize = 16.sp) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, color = TextDim, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun RingTaskRow(task: TodoTask, highlighted: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.background(NightRaised, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Amber, uncheckedColor = TextDim, checkmarkColor = OnAmber),
        )
        Text(
            task.title,
            modifier = Modifier.weight(1f),
            fontSize = 19.sp,
            color = if (task.done) TextDim else TextHigh,
            textDecoration = if (task.done) TextDecoration.LineThrough else null,
        )
        if (highlighted) Text("this alarm", fontSize = 11.sp, color = Amber, modifier = Modifier.padding(end = 10.dp))
    }
}
