package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import com.example.data.model.UserEntity
import com.example.utils.CurrencyFormatter
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VibrantGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun createImageUri(context: android.content.Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    val image = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillScreen(
    users: List<UserEntity>,
    lastReadingFor: (String) -> Double,
    arrearsFor: (String) -> Double,
    onSaveBill: (
        userId: String,
        userName: String,
        userPhone: String,
        userAddress: String,
        prevReading: Double,
        currentReading: Double,
        date: String,
        notes: String,
        unitPrice: Double,
        readingImageUri: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    var selectedUser by remember { mutableStateOf(users.firstOrNull()) }
    var showUserSelectDialog by remember { mutableStateOf(false) }
    var userSearchQuery by remember { mutableStateOf("") }

    // القراءة السابقة تُجلب تلقائياً من آخر قراءة محفوظة للمشترك ولا يُدخلها المستخدم
    var prevReadingText by remember { mutableStateOf("0") }
    var currentReadingText by remember { mutableStateOf("") }
    // تاريخ اليوم تلقائياً مع إبقاء إمكانية تعديله يدوياً عبر التقويم
    var readingDateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))
    }
    var notesText by remember { mutableStateOf("") }
    // المتأخرات السابقة للمشترك المحدد
    var arrears by remember { mutableStateOf(0.0) }

    // عند اختيار المشترك: جلب آخر قراءة + المتأخرات تلقائياً
    LaunchedEffect(selectedUser?.id) {
        val uid = selectedUser?.id
        if (uid != null) {
            prevReadingText = lastReadingFor(uid).toInt().toString()
            arrears = arrearsFor(uid)
        } else {
            prevReadingText = "0"
            arrears = 0.0
        }
    }
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri = tempCameraUri
        }
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
        }
    }

    val prevNum = prevReadingText.toDoubleOrNull() ?: 0.0
    val currNum = currentReadingText.toDoubleOrNull() ?: 0.0
    val consumption = (currNum - prevNum).coerceAtLeast(0.0)
    val unitPrice = selectedUser?.unitPrice ?: 170.0
    val subtotal = consumption * unitPrice
    // لا توجد أي ضريبة: الإجمالي = قيمة الاستهلاك + المتأخرات
    val totalAmount = subtotal + arrears

    // Calendar setup for date picker
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            readingDateText = "%02d/%02d/%d".format(selectedDayOfMonth, selectedMonth + 1, selectedYear)
        },
        year, month, day
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إصدار / إضافة فاتورة جديدة",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Subscriber Selection Field (Clicking displays all subscribers)
        Text(text = "اسم المشترك / المستخدم:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { showUserSelectDialog = true }
                .testTag("user_select_box"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Person, contentDescription = "مشترك", tint = ElectricBlue)
                    Column {
                        Text(
                            text = selectedUser?.name ?: "اضغط هنا لاختيار المشترك...",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedUser != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        selectedUser?.let {
                            Text(
                                text = "الرمز: ${it.userIdCode} - الهاتف: ${it.phone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "قائمة المشتركين")
            }
        }

        // Price per kWh (سعر الكيلو - تحديد يدوي)

        // Meter Readings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = prevReadingText,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text("القراءة السابقة (تلقائية)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("prev_reading_input"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = currentReadingText,
                onValueChange = { currentReadingText = it },
                label = { Text("القراءة الحالية (ك.و.س)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("curr_reading_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Camera / Gallery Image Picker
        Text(text = "صورة القراءة (اختياري):", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val uri = createImageUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "كاميرا")
                Spacer(modifier = Modifier.width(4.dp))
                Text("التقاط صورة", maxLines = 1)
            }
            
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch("image/*")
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = "معرض الصور")
                Spacer(modifier = Modifier.width(4.dp))
                Text("اختيار صورة", maxLines = 1)
            }
        }
        
        if (imageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "صورة القراءة",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { imageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "حذف", tint = Color.White)
                }
            }
        }

        // Reading Date Picker (تحديد التاريخ يدوياً عبر التقويم)
        Text(text = "تاريخ القراءة:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { datePickerDialog.show() }
                .testTag("reading_date_picker_box"),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.DateRange, contentDescription = "تاريخ القراءة", tint = ElectricBlue)
                    Text(
                        text = readingDateText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text("تغيير التاريخ", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            }
        }

        // Notes
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text("ملاحظات الفاتورة (اختياري)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .testTag("notes_input"),
            shape = RoundedCornerShape(12.dp)
        )

        // Auto Calculated Billing Details Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "الحساب التلقائي للفاتورة:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "القراءة السابقة", value = "${prevNum.toInt()} ك.و.س")
                DetailRow(label = "القراءة الحالية", value = "${currNum.toInt()} ك.و.س")
                DetailRow(label = "الاستهلاك المحسوب", value = "${consumption.toInt()} ك.و.س")
                DetailRow(label = "سعر الكيلو المعتمد", value = "${unitPrice.toInt()} ريال")
                DetailRow(label = "قيمة الاستهلاك", value = CurrencyFormatter.riyal(subtotal))
                DetailRow(label = "المتأخرات السابقة", value = CurrencyFormatter.riyal(arrears))

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المبلغ الإجمالي النهائي:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = CurrencyFormatter.riyalFull(totalAmount),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    Toast.makeText(
                        context,
                        "المبلغ الإجمالي المحسوب بسعر الكيلو (${unitPrice.toInt()} ريال): ${CurrencyFormatter.riyal(totalAmount)}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Calculate, contentDescription = "حساب")
                Spacer(modifier = Modifier.width(4.dp))
                Text("إعادة حساب")
            }

            Button(
                onClick = {
                    val user = selectedUser
                    if (user != null) {
                        onSaveBill(
                            user.id,
                            user.name,
                            user.phone,
                            user.address,
                            prevNum,
                            currNum,
                            readingDateText,
                            notesText,
                            unitPrice,
                            imageUri?.toString()
                        )
                        Toast.makeText(context, "تم حفظ الفاتورة بسعر المشترك ${unitPrice.toInt()} ريال/ك.و.س بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "يرجى اختيار المشترك أولاً", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_bill_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = "حفظ")
                Spacer(modifier = Modifier.width(4.dp))
                Text("حفظ الفاتورة")
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    }

    // Dialog for displaying and selecting from ALL subscribers
    if (showUserSelectDialog) {
        val filteredUsers = users.filter { u ->
            userSearchQuery.isBlank() || u.name.contains(userSearchQuery, ignoreCase = true) || u.userIdCode.contains(userSearchQuery, ignoreCase = true) || u.phone.contains(userSearchQuery)
        }

        AlertDialog(
            onDismissRequest = { showUserSelectDialog = false },
            title = {
                Text(text = "قائمة المشتركين المتاحين", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = { userSearchQuery = it },
                        placeholder = { Text("بحث باسم المشترك أو الكود...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredUsers, key = { it.id }) { u ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUser = u
                                        showUserSelectDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedUser?.id == u.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("${u.userIdCode} • ${u.phone.ifBlank { "بدون هاتف" }}", style = MaterialTheme.typography.labelSmall)
                                        Text(u.address, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                    if (selectedUser?.id == u.id) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "محدد", tint = ElectricBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserSelectDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
