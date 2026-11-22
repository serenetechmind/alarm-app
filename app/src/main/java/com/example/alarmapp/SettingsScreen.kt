package com.example.alarmapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val firebaseReady = remember { runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false) }
    var token by remember { mutableStateOf(DeviceStore.fcmToken(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            token = DeviceStore.fcmToken(context)
            delay(2000)
        }
    }

    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Light)
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Appearance", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("System", ThemeStore.mode == ThemeMode.System) { ThemeStore.set(context, ThemeMode.System) }
                Pill("Light", ThemeStore.mode == ThemeMode.Light) { ThemeStore.set(context, ThemeMode.Light) }
                Pill("Dark", ThemeStore.mode == ThemeMode.Dark) { ThemeStore.set(context, ThemeMode.Dark) }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "The ringing screen always stays dark, so it's gentle on sleepy eyes.",
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))
            Text("Instant alarms", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            val (title, detail) = when {
                !firebaseReady -> "Not set up" to "Add google-services.json to the app so other apps can push alarms instantly."
                token == null -> "Waiting for a push token…" to "Firebase is set up. This usually takes a few seconds."
                else -> "Ready" to "This phone can receive alarms from connected apps instantly."
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(scheme.surfaceVariant).padding(18.dp)
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (title == "Ready") scheme.primary else scheme.onSurface,
                )
                Text(detail, fontSize = 13.sp, color = scheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(32.dp))
            Text("About", fontSize = 14.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("AlarmApp", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
