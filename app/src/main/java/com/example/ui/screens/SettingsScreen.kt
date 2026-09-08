package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BillingViewModel,
    onNavigateToRBAC: () -> Unit,
    onLogoutAllDevices: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToReadingReminders: () -> Unit,
) {
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val newBillNotification by viewModel.newBillNotification.collectAsStateWithLifecycle()
    val paymentNotification by viewModel.paymentNotification.collectAsStateWithLifecycle()
    val notificationSound by viewModel.notificationSound.collectAsStateWithLifecycle()

    val orgName by viewModel.orgName.collectAsStateWithLifecycle()
    val orgPhone by viewModel.orgPhone.collectAsStateWithLifecycle()
    val orgAddress by viewModel.orgAddress.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    var autoBackupEnabled by remember { mutableStateOf(viewModel.getPreferenceBoolean("backup_auto_enabled", true)) }
    var localTreeUri by remember { mutableStateOf(viewModel.getPreferenceString("backup_local_tree_uri", "")) }
    var driveTreeUri by remember { mutableStateOf(viewModel.getPreferenceString("backup_drive_tree_uri", "")) }

    var languageMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var fontSizeSlider by remember { mutableStateOf(1f) } // 0: Small, 1: Medium, 2: Large

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(context, "لم يتم منح صلاحية الإشعارات", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val localFolderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            localTreeUri = uri.toString()
            viewModel.updatePreferenceString("backup_local_tree_uri", localTreeUri)
            com.example.utils.AutoBackupWorker.schedule(context)
            Toast.makeText(context, "تم تحديد مجلد الحفظ المحلي للنسخ اليومية", Toast.LENGTH_SHORT).show()
        }
    }

    val driveFolderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            driveTreeUri = uri.toString()
            viewModel.updatePreferenceString("backup_drive_tree_uri", driveTreeUri)
            com.example.utils.AutoBackupWorker.schedule(context)
            Toast.makeText(context, "تم ربط مجلد Google Drive للنسخ اليومية التلقائية", Toast.LENGTH_SHORT).show()
        }
    }

    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = com.example.utils.BackupHelper.backupDatabase(context, uri)
                if (success) {
                    android.widget.Toast.makeText(context, "تم إنشاء النسخة الاحتياطية بنجاح", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "فشل إنشاء النسخة الاحتياطية", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
            showRestoreDialog = true
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreDialog = false
                selectedRestoreUri = null
            },
            title = { Text("تحذير استعادة البيانات") },
            text = { Text("استعادة النسخة الاحتياطية سيؤدي إلى استبدال جميع البيانات الحالية بشكل نهائي. هل أنت متأكد من رغبتك في المتابعة؟") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    val uri = selectedRestoreUri
                    if (uri != null) {
                        coroutineScope.launch {
                            val success = com.example.utils.BackupHelper.restoreDatabase(context, uri)
                            if (success) {
                                android.widget.Toast.makeText(context, "تم استعادة البيانات بنجاح، يرجى إعادة تشغيل التطبيق", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "فشل استعادة البيانات", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    selectedRestoreUri = null
                }) {
                    Text("نعم، استعادة", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    selectedRestoreUri = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val currentAccessKey by viewModel.currentAccessKey.collectAsStateWithLifecycle()
    val isAdmin = currentAccessKey?.role == "ADMIN" || currentUserProfile.roleType == com.example.data.model.RoleType.SUPERVISOR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "إعدادات النظام",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Section 1: General Settings & Appearance
        SettingsGroupCard(title = "عام والمظهر") {
            // Dark Mode Toggle
            SettingsToggleRow(
                title = "الوضع الداكن (Dark Mode)",
                subtitle = "تفعيل أو إيقاف المظهر الداكن المريح للعين",
                checked = darkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
                testTag = "settings_dark_mode_switch"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Language Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("لغة التطبيق", style = MaterialTheme.typography.titleSmall)
                    Text("اختر واجهة اللغة للبرنامج", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                Box {
                    OutlinedButton(
                        onClick = { languageMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(language)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "اختر")
                    }

                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("العربية (RTL)") },
                            onClick = {
                                viewModel.updateSettingString("setting_language", "العربية", viewModel.language)
                                languageMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                viewModel.updateSettingString("setting_language", "English", viewModel.language)
                                languageMenuExpanded = false
                                Toast.makeText(context, "English is not fully supported yet", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Invoice & Report Settings
        SettingsGroupCard(title = "إعدادات الفواتير والتقارير") {
            OutlinedTextField(
                value = orgName,
                onValueChange = { viewModel.updateSettingString("setting_org_name", it, viewModel.orgName) },
                label = { Text("اسم المؤسسة / الشركة") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orgPhone,
                onValueChange = { viewModel.updateSettingString("setting_org_phone", it, viewModel.orgPhone) },
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = orgAddress,
                onValueChange = { viewModel.updateSettingString("setting_org_address", it, viewModel.orgAddress) },
                label = { Text("العنوان") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Currency Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("العملة المعتمدة", style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(
                        onClick = { currencyMenuExpanded = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(currency)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "اختر العملة")
                    }

                    DropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        listOf("ريال يمني", "ريال سعودي", "دولار أمريكي").forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    viewModel.updateSettingString("setting_currency", curr, viewModel.currency)
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Notification Settings
        SettingsGroupCard(title = "الإشعارات والتنبيهات") {
            SettingsToggleRow(
                title = "إشعارات الفواتير الجديدة",
                subtitle = "إرسال تنبيه فور إصدار أي فاتورة جديدة",
                checked = newBillNotification,
                onCheckedChange = { isChecked ->
                    if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.updateSettingBoolean("setting_new_bill_notif", isChecked, viewModel.newBillNotification)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsToggleRow(
                title = "إشعارات الدفعات والسداد",
                subtitle = "إشعارات عند سداد أي فاتورة",
                checked = paymentNotification,
                onCheckedChange = { isChecked ->
                    if (isChecked && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.updateSettingBoolean("setting_payment_notif", isChecked, viewModel.paymentNotification)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsToggleRow(
                title = "صوت الإشعارات",
                subtitle = "تفعيل الصوت التنبيهي للإشعارات",
                checked = notificationSound,
                onCheckedChange = { viewModel.updateSettingBoolean("setting_notif_sound", it, viewModel.notificationSound) }
            )
        }

        // Section 4: Security and Backup
        if (isAdmin) {
            SettingsGroupCard(title = "الأمان والنسخ الاحتياطي") {
            // RBAC Access
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRBAC() }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إدارة المفاتيح والصلاحيات", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                    Text("إدارة المفاتيح السرية وأذونات المستخدمين", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "RBAC", tint = ElectricBlue)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToSync() }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("مزامنة البيانات (Wi-Fi)", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                    }
                    Icon(Icons.Filled.Sync, contentDescription = "Sync", tint = ElectricBlue)
                }

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToReadingReminders() }.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("جدولة تذكير قراءة العدادات", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                    Text("حدد تاريخ ووقت التذكير لكل مشترك", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.Schedule, contentDescription = "التذكيرات", tint = ElectricBlue)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Backup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { backupLauncher.launch("العسل - " + java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) + ".zip") }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إنشاء نسخة احتياطية", style = MaterialTheme.typography.titleSmall)
                    Text("تصدير البيانات إلى ملف محلي", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.Backup, contentDescription = "نسخ احتياطي")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsToggleRow(
                title = "نسخة احتياطية يومية تلقائية",
                subtitle = "إنشاء نسخة يومياً الساعة 2 صباحاً وحفظها في المجلد المحدد، وGoogle Drive إذا تم ربطه",
                checked = autoBackupEnabled,
                onCheckedChange = { enabled ->
                    autoBackupEnabled = enabled
                    viewModel.updatePreferenceBoolean("backup_auto_enabled", enabled)
                    if (enabled) com.example.utils.AutoBackupWorker.schedule(context) else com.example.utils.AutoBackupWorker.cancel(context)
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth().clickable { localFolderLauncher.launch(null) }.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("مجلد حفظ النسخ الاحتياطية اليومية", style = MaterialTheme.typography.titleSmall)
                    Text(if (localTreeUri.isBlank()) "حدد مكان حفظ النسخ على الهاتف أو بطاقة الذاكرة" else "تم تحديد مجلد الحفظ المحلي", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.Folder, contentDescription = "مجلد الحفظ", tint = ElectricBlue)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth().clickable { driveFolderLauncher.launch(null) }.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("مجلد النسخ الاحتياطي في Google Drive", style = MaterialTheme.typography.titleSmall)
                    Text(if (driveTreeUri.isBlank()) "حدد مجلداً داخل حساب Google Drive مرة واحدة، وبعدها يتم الرفع تلقائياً" else "تم ربط Google Drive للنسخ اليومية التلقائية", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.CloudUpload, contentDescription = "Google Drive", tint = ElectricBlue)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Share Backup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            val backupFile = com.example.utils.BackupHelper.createBackupFileForSharing(context)
                            if (backupFile != null) {
                                val authority = "${context.packageName}.fileprovider"
                                val contentUri = androidx.core.content.FileProvider.getUriForFile(context, authority, backupFile)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "نسخة احتياطية لنظام فواتير الكهرباء")
                                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "لم يتم العثور على تطبيق مناسب للمشاركة", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "فشل إنشاء النسخة الاحتياطية", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مشاركة النسخة الاحتياطية", style = MaterialTheme.typography.titleSmall)
                    Text("مشاركة البيانات عبر تطبيقات أخرى", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.Share, contentDescription = "مشاركة")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Restore
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { restoreLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("استعادة البيانات", style = MaterialTheme.typography.titleSmall)
                    Text("استيراد البيانات من ملف", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Icon(Icons.Filled.Restore, contentDescription = "استعادة")
            }
        }
        }

        // Section 5: About Application
        SettingsGroupCard(title = "حول التطبيق") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("اسم التطبيق", style = MaterialTheme.typography.titleSmall)
                Text("نظام فواتير الكهرباء", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الإصدار", style = MaterialTheme.typography.titleSmall)
                Text("v1.0.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onLogoutAllDevices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = "خروج")
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
