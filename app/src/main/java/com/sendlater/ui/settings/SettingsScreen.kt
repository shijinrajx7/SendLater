package com.sendlater.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sendlater.data.SendMode
import com.sendlater.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOnboarding: () -> Unit,
    onReplayIntro: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val mode by vm.mode.collectAsState()
    val theme by vm.theme.collectAsState()
    val maker by vm.maker.collectAsState()
    val cc by vm.countryCode.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SetupChecklist()
            TextButton(onClick = onOnboarding) { Text("How to turn on the accessibility service") }

            Spacer(Modifier.height(8.dp))
            Text("How to send", style = MaterialTheme.typography.titleMedium)
            RadioRow("Auto: SendLater taps Send for me", mode == SendMode.AUTO) { vm.setMode(SendMode.AUTO) }
            RadioRow("Safe: notify me, I tap Send", mode == SendMode.SAFE) { vm.setMode(SendMode.SAFE) }

            Spacer(Modifier.height(8.dp))
            Text("Your name (shown in the opening)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = maker,
                onValueChange = { vm.setMaker(it.take(24)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onReplayIntro) { Text("Replay opening") }

            Spacer(Modifier.height(8.dp))
            Text("Default country code", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = cc,
                onValueChange = { vm.setCountryCode(it) },
                singleLine = true,
                prefix = { Text("+") },
                supportingText = { Text("Added to numbers typed without one (91 = India).") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            RadioRow("Follow system", theme == ThemeMode.SYSTEM) { vm.setTheme(ThemeMode.SYSTEM) }
            RadioRow("Light", theme == ThemeMode.LIGHT) { vm.setTheme(ThemeMode.LIGHT) }
            RadioRow("Dark", theme == ThemeMode.DARK) { vm.setTheme(ThemeMode.DARK) }

            Spacer(Modifier.height(8.dp))
            Text("Xiaomi / Redmi / Poco / Oppo / Vivo", style = MaterialTheme.typography.titleMedium)
            Text(
                "These phones block background apps. Open App info > Other permissions and allow " +
                    "\"Display pop-up windows while running in the background\" and \"Show on lock screen\", " +
                    "and turn on Autostart. Without these, auto-send can silently fail."
            )
            OutlinedButton(onClick = {
                try {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Open Settings > Apps > SendLater", Toast.LENGTH_LONG).show()
                }
            }) { Text("Open App info") }

            Spacer(Modifier.height(8.dp))
            Text(
                "If your phone has a PIN, pattern or fingerprint lock, Android won't let any app unlock it. " +
                    "Messages due while it's locked arrive as a notification: unlock and tap it, and SendLater sends it automatically.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = Role.RadioButton).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}
