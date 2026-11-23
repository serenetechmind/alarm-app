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

    var pairCode by remember { mutableStateOf(DeviceStore.pairCode(context)) }
    var connections by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var serverUrl by remember { mutableStateOf(DeviceStore.serverUrl(context)) }
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
        Text("Your pair code", style = MaterialTheme.typography.labelLarge)
        Text(
            pairCode?.chunked(3)?.joinToString(" ") ?: "— — —",
            fontSize = 44.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("Most apps have a Connect button, so you usually won't need this. If one asks for a code, enter this.")

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            DeviceStore.setServerUrl(context, serverUrl)
            pairCode = null
            load()
        }) { Text("Save server and reconnect") }

        if (loading) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Contacting the server… the first request can take up to a minute on free hosting.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (status.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Connected apps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { load() }, enabled = !loading) { Text(if (loading) "Loading…" else "Refresh") }
        }
        if (connections.isEmpty()) {
            Text("No apps connected yet.")
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
                                status = "Couldn't revoke: ${e.message}"
                            }
                            load()
                        }
                    }) { Text("Revoke") }
                }
            }
        }
    }
}
