package com.sendlater.ui.home

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sendlater.data.Repeat
import com.sendlater.data.ScheduledMessage
import com.sendlater.data.SendMode
import com.sendlater.data.Status
import com.sendlater.send.SendActions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val messages by vm.messages.collectAsState()
    val maker by vm.maker.collectAsState()
    val mode by vm.mode.collectAsState()

    // Notification permission (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Exact alarm permission (Android 12+)
    val am = remember { ctx.getSystemService(AlarmManager::class.java) }
    var canExact by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SendLater")
                        Text("by $maker", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "New schedule") }
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            if (!canExact) {
                Card(Modifier.padding(16.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Allow exact alarms so messages go out right on time.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                            )
                        }) { Text("Allow") }
                    }
                }
            }
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No scheduled messages yet.\nTap + to add one.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { m ->
                        MessageRow(
                            m,
                            onClick = { onEdit(m.id) },
                            onDelete = { vm.delete(m.id) },
                            onSendNow = {
                                SendActions.sendNow(ctx, m, mode == SendMode.AUTO)
                                    ?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageRow(m: ScheduledMessage, onClick: () -> Unit, onDelete: () -> Unit, onSendNow: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFB3261E)).padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
        }
    ) {
        Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.contactName.ifBlank { "+${m.phone}" },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusLabel(m.status)
                }
                Spacer(Modifier.height(4.dp))
                Text(m.text, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                if (m.status == Status.FAILED && m.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(m.note, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val whenText = SimpleDateFormat("EEE, d MMM yyyy  h:mm a", Locale.getDefault()).format(Date(m.triggerAt))
                    val rep = if (m.repeat == Repeat.NONE) "" else "  ·  ${m.repeat.name.lowercase()}"
                    Text(
                        whenText + rep,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (m.status != Status.SENT) {
                        TextButton(onClick = onSendNow) { Text("Send now") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(s: Status) {
    val color = when (s) {
        Status.PENDING -> MaterialTheme.colorScheme.primary
        Status.SENT -> Color(0xFF2E7D32)
        Status.FAILED -> MaterialTheme.colorScheme.error
    }
    Text(s.name.lowercase().replaceFirstChar { it.uppercase() }, color = color, style = MaterialTheme.typography.labelLarge)
}
