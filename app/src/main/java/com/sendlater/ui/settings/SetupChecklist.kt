package com.sendlater.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sendlater.send.isAccessibilityServiceEnabled

private class SetupItem(val title: String, val ok: Boolean, val hint: String, val fix: () -> Unit)

private fun safeStart(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(ctx, "Open this from your phone's Settings > Apps > SendLater", Toast.LENGTH_LONG).show()
    }
}

private fun buildItems(ctx: Context): List<SetupItem> {
    val pkgUri = Uri.parse("package:${ctx.packageName}")
    val am = ctx.getSystemService(AlarmManager::class.java)
    val pm = ctx.getSystemService(PowerManager::class.java)
    return listOf(
        SetupItem(
            "Accessibility service",
            isAccessibilityServiceEnabled(ctx),
            "Lets SendLater tap Send for you."
        ) { safeStart(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        SetupItem(
            "Display over other apps",
            Settings.canDrawOverlays(ctx),
            "Lets SendLater open WhatsApp from the background."
        ) { safeStart(ctx, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)) },
        SetupItem(
            "Exact alarms",
            Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms(),
            "So messages go out on the minute."
        ) {
            if (Build.VERSION.SDK_INT >= 31) safeStart(ctx, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri))
        },
        SetupItem(
            "Battery: unrestricted",
            pm.isIgnoringBatteryOptimizations(ctx.packageName),
            "Stops your phone from killing SendLater."
        ) { safeStart(ctx, Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri)) },
        SetupItem(
            "Notifications",
            NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            "For reminders and failure alerts."
        ) {
            safeStart(
                ctx,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }
    )
}

@Composable
fun SetupChecklist() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose {}
    }
    val items = remember(tick) { buildItems(ctx) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            Text("Everything should be ✅ for messages to send by themselves.", style = MaterialTheme.typography.bodySmall)
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (item.ok) "✅" else "❌", modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title)
                        if (!item.ok) Text(item.hint, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!item.ok) TextButton(onClick = item.fix) { Text("Fix") }
                }
            }
        }
    }
}
