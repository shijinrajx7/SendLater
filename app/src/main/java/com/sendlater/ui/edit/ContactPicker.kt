package com.sendlater.ui.edit

import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactEntry(val name: String, val number: String)

/** Every contact that has a phone number, A-Z. */
fun loadContacts(ctx: Context): List<ContactEntry> {
    val out = ArrayList<ContactEntry>()
    val seen = HashSet<String>()
    val phone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val nameCol = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
    val numCol = ContactsContract.CommonDataKinds.Phone.NUMBER
    ctx.contentResolver.query(phone, arrayOf(nameCol, numCol), null, null, "$nameCol COLLATE NOCASE ASC")?.use { c ->
        val ni = c.getColumnIndex(nameCol)
        val pi = c.getColumnIndex(numCol)
        while (c.moveToNext()) {
            val name = c.getString(ni) ?: ""
            val num = c.getString(pi) ?: ""
            val key = name + num.filter { it.isDigit() }.takeLast(10)
            if (num.isNotBlank() && seen.add(key)) out.add(ContactEntry(name, num))
        }
    }
    return out
}

/** Full-screen searchable contact list, like a dialer's contacts tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerDialog(onPick: (ContactEntry) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactEntry>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) { loadContacts(ctx) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search name or number") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                val list = contacts
                if (list == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    val q = query.trim()
                    val filtered = if (q.isEmpty()) list else list.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            it.number.replace(" ", "").contains(q.replace(" ", ""))
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No contacts found.\nGo back and type the number instead.")
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered) { c ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPick(c) }.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(c.name.take(1).uppercase().ifBlank { "#" })
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(c.name.ifBlank { "Unknown" }, style = MaterialTheme.typography.titleMedium)
                                        Text(c.number, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
