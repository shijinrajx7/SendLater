package com.sendlater.ui.edit

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sendlater.data.Repeat
import com.sendlater.data.ScheduledMessage
import com.sendlater.send.PhoneUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(id: Long, onDone: () -> Unit, vm: EditViewModel = hiltViewModel()) {
    val ctx = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var millis by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000L) }
    var repeat by remember { mutableStateOf(Repeat.NONE) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id > 0) vm.load(id)?.let {
            name = it.contactName; phone = it.phone; text = it.text
            millis = it.triggerAt; repeat = it.repeat
        }
    }

    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) showPicker = true
        else Toast.makeText(ctx, "Contacts permission denied. Type the number instead.", Toast.LENGTH_LONG).show()
    }

    if (showPicker) {
        ContactPickerDialog(
            onPick = { c -> name = c.name; phone = c.number; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id > 0) "Edit message" else "New schedule") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Send to", style = MaterialTheme.typography.labelLarge)
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                    showPicker = true
                else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
            }, modifier = Modifier.fillMaxWidth()) { Text("Choose from contacts") }

            Text("or type a number", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                phone, { phone = it },
                label = { Text("Phone number") },
                placeholder = { Text("98765 43210  or  +1 555 0100") },
                supportingText = {
                    if (phone.isNotBlank()) Text("Will send to +${PhoneUtil.normalize(phone, vm.countryCode)}")
                    else Text("Numbers without a country code get +${vm.countryCode} (change in Settings)")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Message", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                text, { text = it },
                label = { Text("Message") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Good morning ☀️", "Happy birthday 🎂", "Call me when you're free", "Don't forget!").forEach { t ->
                    AssistChip(
                        onClick = { text = if (text.isBlank()) t else "$text $t" },
                        label = { Text(t) }
                    )
                }
            }

            Text("When", style = MaterialTheme.typography.labelLarge)
            val label = SimpleDateFormat("EEE, d MMM yyyy  h:mm a", Locale.getDefault()).format(Date(millis))
            OutlinedButton(onClick = { pickDateTime(ctx, millis) { millis = it } }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { millis = System.currentTimeMillis() + 2 * 60_000L }, label = { Text("In 2 min") })
                AssistChip(onClick = { millis = System.currentTimeMillis() + 60 * 60_000L }, label = { Text("In 1 hour") })
                AssistChip(onClick = { millis = atTime(21, 0, false) }, label = { Text("Tonight 9 PM") })
                AssistChip(onClick = { millis = atTime(9, 0, true) }, label = { Text("Tomorrow 9 AM") })
            }

            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Repeat.values().forEach { r ->
                    FilterChip(
                        selected = repeat == r,
                        onClick = { repeat = r },
                        label = { Text(if (r == Repeat.NONE) "Once" else r.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Button(
                onClick = {
                    val digits = PhoneUtil.normalize(phone, vm.countryCode)
                    when {
                        digits.length < 8 -> toast(ctx, "Enter a valid phone number")
                        text.isBlank() -> toast(ctx, "Message can't be empty")
                        millis <= System.currentTimeMillis() -> toast(ctx, "Pick a time in the future")
                        else -> vm.save(
                            ScheduledMessage(
                                id = if (id > 0) id else 0,
                                contactName = name.trim(), phone = digits, text = text.trim(),
                                triggerAt = millis, repeat = repeat
                            ), onDone
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Schedule") }
        }
    }
}

private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

private fun atTime(hour: Int, minute: Int, forceTomorrow: Boolean): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, hour)
    c.set(Calendar.MINUTE, minute)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    if (forceTomorrow || c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
    return c.timeInMillis
}

private fun pickDateTime(ctx: Context, current: Long, onPicked: (Long) -> Unit) {
    val c = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(ctx, { _, y, m, d ->
        TimePickerDialog(ctx, { _, h, min ->
            c.set(y, m, d, h, min, 0)
            c.set(Calendar.MILLISECOND, 0)
            onPicked(c.timeInMillis)
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), DateFormat.is24HourFormat(ctx)).show()
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}
