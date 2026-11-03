package com.sendlater.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Set up auto mode") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Why this permission?", style = MaterialTheme.typography.titleMedium)
            Text(
                "At the scheduled time SendLater opens the chat and taps WhatsApp's Send button for you. " +
                    "Android only allows that through an accessibility service. SendLater only watches " +
                    "WhatsApp windows, and nothing leaves your phone."
            )
            Text("Steps", style = MaterialTheme.typography.titleMedium)
            Text("1. Tap the button below to open Accessibility settings.")
            Text("2. Open \"Installed apps\" (or \"Downloaded apps\") and pick SendLater.")
            Text("3. Turn it on and confirm.")
            Text(
                "If the switch is greyed out (Android 13+, app installed from an APK): go to Settings > Apps > " +
                    "SendLater > tap the ⋮ menu > \"Allow restricted settings\", then try again."
            )
            Text(
                "Heads up: WhatsApp may not approve of automated sending. Use auto mode only for your own " +
                    "normal messages, never for spam."
            )
            Button(
                onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Accessibility settings") }
        }
    }
}
