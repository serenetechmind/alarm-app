package com.example.alarmapp

import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmApp() {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    var alarms by remember { mutableStateOf(repo.getAll()) }
    var editing by remember { mutableStateOf<Alarm?>(null) }
    var showApps by remember { mutableStateOf(false) }

    fun refresh() { alarms = repo.getAll() }

    // Ask Firebase for a push token, then mirror the server's alarms (ignored when offline).
    LaunchedEffect(Unit) {
        AlarmSync.refreshFcmToken(context)
        withContext(Dispatchers.IO) { runCatching { AlarmSync.fullSync(context) } }
        refresh()
    }
    // Pick up alarms pushed by other apps while this screen is open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            val latest = repo.getAll()
            if (latest != alarms) alarms = latest
        }
    }
    BackHandler(enabled = showApps) { showApps = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showApps) "Connected apps" else "Alarms") },
                actions = {
                    if (!showApps) TextButton(onClick = { showApps = true }) { Text("Connected apps") }
                },
            )
        },
        floatingActionButton = {
            if (!showApps) {
                FloatingActionButton(onClick = { editing = Alarm(id = 0, hour = 7, minute = 0) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm")
                }
            }
        },
    ) { padding ->
        if (showApps) {
            ConnectedAppsScreen(Modifier.padding(padding))
        } else if (alarms.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No alarms yet. Tap + to add one.")
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        onToggle = { on ->
                            val updated = repo.save(alarm.copy(enabled = on))
                            AlarmScheduler.schedule(context, updated)
                            refresh()
                        },
                        onClick = { editing = alarm },
                        onDelete = {
                            AlarmScheduler.cancel(context, alarm.id)
                            repo.delete(alarm.id)
                            refresh()
                        },
                    )
                }
            }
        }
    }

    editing?.let { current ->
        AlarmEditorDialog(
            initial = current,
            onDismiss = { editing = null },
            onSave = { edited ->
                val saved = repo.save(edited.copy(enabled = true))
                AlarmScheduler.schedule(context, saved)
                refresh()
                editing = null
            },
        )
    }
}

@Composable
fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(alarm.timeLabel(), fontSize = 32.sp, fontWeight = FontWeight.Light)
            Text(
                listOfNotNull(alarm.label.ifBlank { null }, alarm.daysLabel()).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
            alarm.sourceApp?.let {
                Text("via $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete alarm") }
        Switch(checked = alarm.enabled, onCheckedChange = onToggle)
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorDialog(initial: Alarm, onDismiss: () -> Unit, onSave: (Alarm) -> Unit) {
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    var days by remember { mutableStateOf(initial.repeatDays) }
    var label by remember { mutableStateOf(initial.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(hour = timeState.hour, minute = timeState.minute, repeatDays = days, label = label.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = timeState)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LETTERS.forEachIndexed { index, letter ->
                        val day = index + 1 // Calendar.DAY_OF_WEEK
                        val selected = day in days
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { days = if (selected) days - day else days + day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                letter,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                )
            }
        },
    )
}
