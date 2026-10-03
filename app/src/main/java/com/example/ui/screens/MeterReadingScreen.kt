package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserEntity
import com.example.data.model.MeterReadingEntity
import com.example.ui.theme.ElectricBlue
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterReadingScreen(
    users: List<UserEntity>,
    readings: List<MeterReadingEntity> = emptyList(),
    lastReadingFor: (String) -> Double,
    onSaveReading: (String, String, Double, String, String, String?) -> Unit,
    onUpdateReading: (String, Double, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var selectedUser by remember { mutableStateOf<UserEntity?>(users.firstOrNull()) }
    var showUsers by remember { mutableStateOf(false) }
    var readingText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var editingReadingId by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) imageUri = tempCameraUri
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) imageUri = uri
    }

    val cal = Calendar.getInstance()
    val datePicker = DatePickerDialog(context, { _, y, m, d ->
        dateText = "%02d/%02d/%d".format(d, m + 1, y)
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(if (editingReadingId == null) "قراءة العداد" else "تعديل قراءة العداد", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                if (editingReadingId == null) "تسجيل القراءة مع إمكانية تصحيحها لاحقاً."
                else "عدّل القراءة الحالية ثم احفظ التعديل.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text("المشترك", fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showUsers = true },
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedUser?.name ?: "اختر المشترك")
                        selectedUser?.let { Text("العداد: ${it.meterNumber}", style = MaterialTheme.typography.labelSmall) }
                    }
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            }
        }
        item {
            val editing = readings.firstOrNull { it.id == editingReadingId }
            val previous = editing?.previousReading ?: (selectedUser?.let { lastReadingFor(it.id) } ?: 0.0)
            OutlinedTextField(
                value = previous.toInt().toString(), onValueChange = {}, readOnly = true,
                label = { Text("القراءة السابقة") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = readingText, onValueChange = { readingText = it },
                label = { Text("القراءة الحالية *") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.ElectricMeter, contentDescription = null) }
            )
        }
        item {
            OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp)); Text("تاريخ القراءة: $dateText")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { val uri = createImageUri(context); tempCameraUri = uri; cameraLauncher.launch(uri) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("تصوير")
                }
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("صورة")
                }
            }
        }
        item {
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                Button(
                    onClick = {
                        val u = selectedUser ?: return@Button
                        val current = readingText.toDoubleOrNull()
                        if (current == null || current < 0) return@Button
                        val editId = editingReadingId
                        if (editId != null) {
                            onUpdateReading(editId, current, dateText, notes, imageUri?.toString())
                            editingReadingId = null
                        } else {
                            onSaveReading(u.id, u.name, current, dateText, notes, imageUri?.toString())
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (editingReadingId == null) "حفظ القراءة" else "حفظ التعديل")
                }
            }
        }
    }

        item {
            // سجل القراءات المحفوظة مع إمكانية تصحيح القراءة الخاطئة.
    if (readings.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("سجل القراءات", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            readings.sortedByDescending { it.createdAt }.take(20).forEach { reading ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(reading.userName, fontWeight = FontWeight.Bold)
                            Text("السابقة: " + reading.previousReading.toInt() + "  ←  الحالية: " + reading.currentReading.toInt())
                            Text("التاريخ: " + reading.readingDate, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            editingReadingId = reading.id
                            selectedUser = users.firstOrNull { it.id == reading.userId } ?: selectedUser
                            readingText = reading.currentReading.toString()
                            dateText = reading.readingDate
                            notes = reading.notes
                            imageUri = reading.imageUri?.let { Uri.parse(it) }
                            showUsers = false
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل القراءة")
                        }
                    }
                }
            }
        }
    }

    if (showUsers) {
        AlertDialog(
            onDismissRequest = { showUsers = false },
            title = { Text("اختر المشترك") },
            text = {
                LazyColumn { items(users.sortedBy { it.name }) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        supportingContent = { Text("العداد: ${user.meterNumber}") },
                        modifier = Modifier.clickable { selectedUser = user; readingText = ""; showUsers = false }
                    )
                } }
            },
            confirmButton = { TextButton(onClick = { showUsers = false }) { Text("إغلاق") } }
        )
    }
}
