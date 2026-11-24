package com.example.alarmapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ConnectedAppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    var pairCode by remember { mutableStateOf(DeviceStore.pairCode(context)) }
    var connections by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            status = ""
            try {
                val conns = withContext(Dispatchers.IO) {
                    AlarmSync.fullSync(context)
                    ApiClient.fetchConnections(context)
                }
                connections = conns.map { it.getInt("id") to it.getString("app_name") }
                pairCode = DeviceStore.pairCode(context)
            } catch (e: Exception) {
                status = "Can't reach the server: ${e.message}"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Apps allowed to set alarms", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { load() }, enabled = !loading) { Text(if (loading) "Loading…" else "Refresh") }
        }

        if (loading) {
            Text(
                "Contacting the server… the first request can take up to a minute.",
                fontSize = 13.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        if (status.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(status, color = scheme.error)
        }

        Spacer(Modifier.height(8.dp))
        if (connections.isEmpty()) {
            Text(
                "No apps connected yet. Tap \"Connect with AlarmApp\" inside an app, then approve it here.",
                color = scheme.onSurfaceVariant,
            )
        } else {
            connections.forEach { (id, name) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    ApiClient.revokeConnection(context, id)
                                    AlarmSync.fullSync(context) // removes that app's alarms locally
                                }
                            } catch (e: Exception) {
                                status = "Couldn't disconnect: ${e.message}"
                            }
                            load()
                        }
                    }) { Text("Disconnect") }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        Text("Pair code", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        Text(
            pairCode?.chunked(3)?.joinToString(" ") ?: "— — —",
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp,
            color = scheme.primary,
        )
        Text(
            "Most apps have a Connect button, so you usually won't need this. If one asks for a code, enter this.",
            fontSize = 13.sp,
            color = scheme.onSurfaceVariant,
        )
    }
}
