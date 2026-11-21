package com.example.alarmapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class Page { Home, Apps, Settings }

@Composable
fun AlarmApp() {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    var alarms by remember { mutableStateOf(repo.getAll()) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var page by remember { mutableStateOf(Page.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var setup by remember { mutableStateOf(SetupChecks.missing(context)) }

    fun refresh() { alarms = repo.getAll() }

    // Ask Firebase for a push token, then mirror the server's alarms (ignored when offline).
    LaunchedEffect(Unit) {
        AlarmSync.refreshFcmToken(context)
        withContext(Dispatchers.IO) { runCatching { AlarmSync.fullSync(context) } }
        refresh()
    }
    // Keeps the countdown fresh, picks up pushed alarms, and notices when a setting was fixed.
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            now = System.currentTimeMillis()
            val latest = repo.getAll()
            if (latest != alarms) alarms = latest
            setup = SetupChecks.missing(context)
        }
    }
    BackHandler(enabled = editing != null || page != Page.Home) {
        if (editing != null) editing = null else page = Page.Home
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val current = editing
        when {
            current != null -> AlarmEditorScreen(
                initial = current,
                onClose = { editing = null },
                onSave = { edited ->
                    val saved = repo.save(edited.copy(enabled = true))
                    AlarmScheduler.schedule(context, saved)
                    refresh()
                    editing = null
                },
            )
            page == Page.Apps -> Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page = Page.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Connected apps", fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
                ConnectedAppsScreen(Modifier.weight(1f))
            }
            page == Page.Settings -> SettingsScreen(onBack = { page = Page.Home })
            else -> AlarmHome(
                alarms = alarms,
                now = now,
                setup = setup,
                onOpenApps = { page = Page.Apps },
                onOpenSettings = { page = Page.Settings },
                onNew = { editing = Alarm(id = 0, hour = 7, minute = 0) },
                onEdit = { editing = it },
                onToggle = { alarm, on ->
                    val updated = repo.save(alarm.copy(enabled = on))
                    AlarmScheduler.schedule(context, updated)
                    refresh()
                },
                onDelete = { alarm ->
                    AlarmScheduler.cancel(context, alarm.id)
                    repo.delete(alarm.id)
                    refresh()
                },
            )
        }
    }
}

@Composable
private fun AlarmHome(
    alarms: List<Alarm>,
    now: Long,
    setup: List<SetupItem>,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New alarm", fontWeight = FontWeight.Medium) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Alarms", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(scheme.surfaceVariant)
                            .clickable(onClick = onOpenApps)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Connected apps", fontSize = 13.sp, color = scheme.primary) }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = scheme.onSurfaceVariant)
                    }
                }
            }
            item { NextAlarmCard(alarms, now) }
            if (setup.isNotEmpty()) item { SetupCard(setup) }
            if (alarms.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No alarms yet", fontSize = 20.sp)
                        Text("Tap New alarm to set your first one.", fontSize = 14.sp, color = scheme.onSurfaceVariant)
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onClick = { onEdit(alarm) },
                        onToggle = { onToggle(alarm, it) },
                        onDelete = { onDelete(alarm) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NextAlarmCard(alarms: List<Alarm>, now: Long) {
    val scheme = MaterialTheme.colorScheme
    val next = alarms
        .filter { it.enabled }
        .mapNotNull { a -> AlarmScheduler.nextTrigger(a)?.let { a to it } }
        .minByOrNull { it.second }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(scheme.primaryContainer).padding(22.dp)
    ) {
        Text("Next alarm", fontSize = 13.sp, color = scheme.onPrimaryContainer.copy(alpha = 0.8f))
        Spacer(Modifier.height(6.dp))
        if (next != null) {
            Text("Rings in ${formatCountdown(next.second - now)}", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = scheme.onPrimaryContainer)
            val (alarm, _) = next
            val tail = if (alarm.label.isNotBlank()) " · ${alarm.label}" else ""
            Text("${alarm.hm()} ${alarm.ampm()}$tail", fontSize = 14.sp, color = scheme.onPrimaryContainer.copy(alpha = 0.75f))
        } else {
            Text("Nothing scheduled", fontSize = 22.sp, color = scheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun SetupCard(items: List<SetupItem>) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainerHigh).padding(20.dp)) {
        Text("Finish setup", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("Do these so your alarms ring reliably.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(item.detail, fontSize = 12.sp, color = scheme.onSurfaceVariant)
                }
                TextButton(onClick = { SetupChecks.fix(context, item.id) }) { Text("Fix") }
            }
        }
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = 22.dp, top = 18.dp, end = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (alarm.enabled) 1f else 0.5f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(alarm.hm(), fontSize = 46.sp, fontWeight = FontWeight.Light)
                    Text(" " + alarm.ampm(), fontSize = 16.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 9.dp))
                }
                if (alarm.label.isNotBlank()) Text(alarm.label, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                if (alarm.repeatDays.isEmpty()) {
                    Text("Once", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DAY_LETTERS.forEachIndexed { i, letter ->
                            val on = (i + 1) in alarm.repeatDays
                            Text(
                                letter,
                                fontSize = 13.sp,
                                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                                color = if (on) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.35f),
                            )
                        }
                    }
                }
                alarm.sourceApp?.let {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(50)).background(scheme.primaryContainer).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("via $it", fontSize = 12.sp, color = scheme.onPrimaryContainer)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = scheme.onPrimary,
                        checkedTrackColor = scheme.primary,
                        uncheckedThumbColor = scheme.onSurfaceVariant,
                        uncheckedTrackColor = scheme.surfaceContainerHigh,
                        uncheckedBorderColor = scheme.outline,
                    ),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete alarm", tint = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text, fontSize = 14.sp, color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant)
    }
}

@Composable
fun AlarmEditorScreen(initial: Alarm, onClose: () -> Unit, onSave: (Alarm) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var h12 by remember { mutableIntStateOf(if (initial.hour % 12 == 0) 12 else initial.hour % 12) }
    var minute by remember { mutableIntStateOf(initial.minute) }
    var pm by remember { mutableStateOf(initial.hour >= 12) }
    var days by remember { mutableStateOf(initial.repeatDays) }
    var label by remember { mutableStateOf(initial.label) }
    var snooze by remember { mutableIntStateOf(initial.snoozeMinutes) }

    val hour24 = (h12 % 12) + if (pm) 12 else 0
    val preview = AlarmScheduler.nextTrigger(Alarm(0, hour24, minute, days))
        ?.let { "Rings in ${formatCountdown(it - System.currentTimeMillis())}" } ?: ""

    val hourLabels = remember { (1..12).map { "%02d".format(it) } }
    val minuteLabels = remember { (0..59).map { "%02d".format(it) } }
    val meridiemLabels = remember { listOf("AM", "PM") }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("Cancel", color = scheme.onSurfaceVariant) }
            Button(
                onClick = {
                    onSave(initial.copy(hour = hour24, minute = minute, repeatDays = days, label = label.trim(), snoozeMinutes = snooze))
                },
                shape = RoundedCornerShape(50),
            ) { Text("Save", fontWeight = FontWeight.Medium) }
        }

        Spacer(Modifier.height(12.dp))
        Text(if (initial.id == 0) "New alarm" else "Edit alarm", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text(preview, fontSize = 16.sp, color = scheme.primary)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WheelPicker(hourLabels, h12 - 1, { h12 = it + 1 }, Modifier.weight(1f))
            Text(":", fontSize = 30.sp, fontWeight = FontWeight.Light)
            WheelPicker(minuteLabels, minute, { minute = it }, Modifier.weight(1f))
            WheelPicker(meridiemLabels, if (pm) 1 else 0, { pm = it == 1 }, Modifier.weight(0.85f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Repeat", fontSize = 14.sp, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_LETTERS.forEachIndexed { index, letter ->
                val day = index + 1 // Calendar.DAY_OF_WEEK
                val selected = day in days
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (selected) scheme.primary else scheme.surfaceVariant)
                        .clickable { days = if (selected) days - day else days + day },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Once", days.isEmpty()) { days = emptySet() }
            Pill("Weekdays", days == setOf(2, 3, 4, 5, 6)) { days = setOf(2, 3, 4, 5, 6) }
            Pill("Weekends", days == setOf(1, 7)) { days = setOf(1, 7) }
            Pill("Every day", days.size == 7) { days = (1..7).toSet() }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Label") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = scheme.primary,
                unfocusedBorderColor = scheme.outlineVariant,
                focusedLabelColor = scheme.primary,
            ),
        )

        Spacer(Modifier.height(24.dp))
        Text("Snooze", fontSize = 14.sp, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15).forEach { m -> Pill("$m min", snooze == m) { snooze = m } }
        }
        Spacer(Modifier.height(40.dp))
    }
}
