package com.example.alarmapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Phase { Loading, Ready, Working, Done, Error }

/** "Test app wants to set alarms on this phone. Allow?" */
@Composable
fun ConnectConsentScreen(requestId: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    var appName by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(Phase.Loading) }
    var message by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            phase = Phase.Loading
            message = ""
            try {
                val info = withContext(Dispatchers.IO) { ApiClient.fetchConnectRequest(context, requestId) }
                appName = info.getString("app_name")
                if (info.getString("status") == "pending") {
                    phase = Phase.Ready
                } else {
                    phase = Phase.Error
                    message = "This link was already used or has expired. Ask the app for a new one."
                }
            } catch (e: Exception) {
                phase = Phase.Error
                message = "Couldn't reach the server: ${e.message}"
            }
        }
    }

    fun allow() {
        scope.launch {
            phase = Phase.Working
            try {
                withContext(Dispatchers.IO) { ApiClient.respondConnect(context, requestId, true) }
                phase = Phase.Done
                delay(1800)
                onClose()
            } catch (e: Exception) {
                phase = Phase.Error
                message = "Couldn't connect: ${e.message}"
            }
        }
    }

    fun deny() {
        scope.launch {
            withContext(Dispatchers.IO) { runCatching { ApiClient.respondConnect(context, requestId, false) } }
            onClose()
        }
    }

    LaunchedEffect(requestId) { load() }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(72.dp)
        Spacer(Modifier.height(28.dp))
        when (phase) {
            Phase.Loading, Phase.Working -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(if (phase == Phase.Loading) "Contacting the server…" else "Connecting…", fontSize = 18.sp)
                Text(
                    "The first request can take up to a minute on free hosting.",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Phase.Ready -> {
                Text(
                    "$appName wants to set alarms on this phone",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "It will be able to add, change and remove alarms here. You can disconnect it any time from Connected apps.",
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = { allow() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Allow", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { deny() }) { Text("Not now", color = scheme.onSurfaceVariant) }
            }
            Phase.Done -> {
                Text("Connected ✓", fontSize = 28.sp, color = scheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$appName can now set alarms on this phone.",
                    fontSize = 14.sp,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Phase.Error -> {
                Text(message, color = scheme.error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { load() }) { Text("Try again") }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
