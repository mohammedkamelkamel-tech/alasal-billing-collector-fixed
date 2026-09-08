package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ReadingReminderEntity
import com.example.data.model.UserEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReadingRemindersScreen(
    users: List<UserEntity>,
    reminders: List<ReadingReminderEntity>,
    onSchedule: (String, String, Long, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    var selectedUser by remember { mutableStateOf<UserEntity?>(users.firstOrNull()) }
    var showUsers by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US) }

    fun pickDateTime() {
        val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                val chosen = Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }
                dateMillis = chosen.timeInMillis
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع") }
                Column {
                    Text("تذكير قراءة العدادات", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text("حدد التاريخ والوقت ليصلك تنبيه تلقائي.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text("المشترك", fontWeight = FontWeight.Bold)
            Surface(Modifier.fillMaxWidth().clickable { showUsers = true }, tonalElevation = 2.dp) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedUser?.name ?: "اختر المشترك", modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
        }
        item {
            OutlinedButton(onClick = ::pickDateTime, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Schedule, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(formatter.format(Date(dateMillis)))
            }
        }
        item { OutlinedTextField(note, { note = it }, label = { Text("ملاحظة (اختياري)") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(
                onClick = {
                    selectedUser?.let {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onSchedule(it.id, it.name, dateMillis, note)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Icon(Icons.Filled.NotificationsActive, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("حفظ التذكير") }
        }
        item { Text("التذكيرات القادمة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        if (reminders.isEmpty()) item { Text("لا توجد تذكيرات مجدولة.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(reminders, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.userName, fontWeight = FontWeight.Bold)
                        Text(formatter.format(Date(r.reminderAt)), style = MaterialTheme.typography.labelMedium)
                        if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onDelete(r.id) }) { Icon(Icons.Filled.Delete, contentDescription = "حذف") }
                }
            }
        }
    }

    if (showUsers) {
        AlertDialog(
            onDismissRequest = { showUsers = false }, title = { Text("اختر المشترك") },
            text = { LazyColumn { items(users.sortedBy { it.name }) { u -> ListItem(headlineContent = { Text(u.name) }, modifier = Modifier.clickable { selectedUser = u; showUsers = false }) } } },
            confirmButton = { TextButton(onClick = { showUsers = false }) { Text("إغلاق") } }
        )
    }
}
