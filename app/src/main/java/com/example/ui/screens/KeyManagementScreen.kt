package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccessKey
import com.example.data.model.PermissionCatalog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.VibrantGreen
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagementScreen(
    accessKeys: List<AccessKey>,
    currentAccessKey: AccessKey?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    onSaveKey: (id: String, secretKey: String, username: String, phone: String, role: String, permissions: List<String>, active: Boolean, expiresAt: Long?, notes: String) -> Unit,
    onToggleActive: (keyId: String, currentActive: Boolean) -> Unit,
    onDeleteKey: (keyId: String) -> Unit,
    onRegenerateKey: (keyId: String) -> Unit,
    onUpdateExpiration: (keyId: String, expiresAt: Long?) -> Unit
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<AccessKey?>(null) }

    // Stat counts
    val totalCount = accessKeys.size
    val activeCount = accessKeys.count { it.active && !it.isExpired() }
    val disabledCount = accessKeys.count { !it.active }
    val expiredCount = accessKeys.count { it.active && it.isExpired() }

    // Filter keys
    val filteredKeys = accessKeys.filter { key ->
        val matchesSearch = searchQuery.isBlank() ||
                key.username.contains(searchQuery, ignoreCase = true) ||
                key.secretKey.contains(searchQuery, ignoreCase = true) ||
                key.phone.contains(searchQuery, ignoreCase = true) ||
                key.role.contains(searchQuery, ignoreCase = true)

        val matchesStatus = when (filterStatus) {
            "ACTIVE" -> key.active && !key.isExpired()
            "DISABLED" -> !key.active
            "EXPIRED" -> key.isExpired()
            else -> true
        }

        matchesSearch && matchesStatus
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingKey = null
                    showCreateDialog = true
                },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = "مفتاح جديد") },
                text = { Text("إنشاء مفتاح سري", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_key_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "إجمالي المفاتيح",
                    count = totalCount,
                    color = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "نشطة",
                    count = activeCount,
                    color = VibrantGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "معطلة",
                    count = disabledCount,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "منتهية",
                    count = expiredCount,
                    color = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar & Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث باسم المستخدم، المفتاح السري، أو الهاتف...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("key_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(title = "الكل ($totalCount)", selected = filterStatus == "ALL", onClick = { onFilterStatusChange("ALL") })
                FilterChipItem(title = "النشطة ($activeCount)", selected = filterStatus == "ACTIVE", onClick = { onFilterStatusChange("ACTIVE") })
                FilterChipItem(title = "المعطلة ($disabledCount)", selected = filterStatus == "DISABLED", onClick = { onFilterStatusChange("DISABLED") })
                FilterChipItem(title = "المنتهية ($expiredCount)", selected = filterStatus == "EXPIRED", onClick = { onFilterStatusChange("EXPIRED") })
            }

            // Keys List
            if (filteredKeys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.KeyOff,
                            contentDescription = "لا توجد نتائج",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد مفاتيح سرية تطابق البحث",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredKeys, key = { it.id }) { keyItem ->
                        KeyCard(
                            keyItem = keyItem,
                            onEdit = {
                                editingKey = keyItem
                                showCreateDialog = true
                            },
                            onToggleActive = { onToggleActive(keyItem.id, keyItem.active) },
                            onDelete = { onDeleteKey(keyItem.id) },
                            onRegenerate = { onRegenerateKey(keyItem.id) },
                            onUpdateExpiration = { newExp -> onUpdateExpiration(keyItem.id, newExp) }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Create / Edit Key
    if (showCreateDialog) {
        KeyEditorDialog(
            initialKey = editingKey,
            onDismiss = { showCreateDialog = false },
            onSave = { id, secret, name, phone, role, perms, active, expiresAt, notes ->
                onSaveKey(id, secret, name, phone, role, perms, active, expiresAt, notes)
                showCreateDialog = false
                Toast.makeText(context, if (id.isBlank()) "تم إنشاء المفتاح السري بنجاح" else "تم تحديث المفتاح السري", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun KeyCard(
    keyItem: AccessKey,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onUpdateExpiration: (Long?) -> Unit
) {
    val context = LocalContext.current
    var keyRevealed by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val statusText: String
    val statusColor: Color
    if (!keyItem.active) {
        statusText = "معطل"
        statusColor = ErrorRed
    } else if (keyItem.isExpired()) {
        statusText = "منتهي"
        statusColor = Color(0xFFFF9800)
    } else {
        statusText = "نشط"
        statusColor = VibrantGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Name, Role Badge, Status, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(keyItem.getRoleColor().copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = "مفتاح",
                            tint = keyItem.getRoleColor(),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = keyItem.username,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = keyItem.getRoleTitleAr(),
                            style = MaterialTheme.typography.labelSmall,
                            color = keyItem.getRoleColor()
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "خيارات")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تعديل المفتاح والصلاحيات") },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (keyItem.active) "تعطيل المفتاح" else "تفعيل المفتاح") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (keyItem.active) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = if (keyItem.active) ErrorRed else VibrantGreen
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleActive()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("إعادة توليد المفتاح السري") },
                                leadingIcon = { Icon(Icons.Filled.Autorenew, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRegenerate()
                                    Toast.makeText(context, "تم إعادة توليد المفتاح السري بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف المفتاح نهائياً") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                    Toast.makeText(context, "تم حذف المفتاح السري", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Secret Key Box with Copy/Share
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "المفتاح السري:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (keyRevealed) keyItem.secretKey else "••••••••••••",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ElectricBlue
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { keyRevealed = !keyRevealed },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (keyRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "إظهار/إخفاء",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("SecretKey", keyItem.secretKey)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ المفتاح السري للحافظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "نسخ",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "المفتاح السري الخاص بك لنظام فواتير الكهرباء:\nاسم المستخدم: ${keyItem.username}\nالمفتاح السري: ${keyItem.secretKey}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة المفتاح السري")
                                try {
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "لم يتم العثور على تطبيق مناسب للمشاركة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "مشاركة",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Key Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("الصلاحية: ${keyItem.getFormattedExpiresAt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("عدد الصلاحيات: ${keyItem.permissions.size} صلاحية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column {
                    Text("آخر دخول: ${keyItem.getFormattedLastLogin()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("الهاتف: ${keyItem.phone.ifBlank { "غير محدد" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyEditorDialog(
    initialKey: AccessKey?,
    onDismiss: () -> Unit,
    onSave: (id: String, secretKey: String, username: String, phone: String, role: String, permissions: List<String>, active: Boolean, expiresAt: Long?, notes: String) -> Unit
) {
    var username by remember { mutableStateOf(initialKey?.username ?: "") }
    var phone by remember { mutableStateOf(initialKey?.phone ?: "") }
    var role by remember {
        mutableStateOf(
            if (initialKey?.role == "ADMIN") "ADMIN" else "OPERATOR"
        )
    }
    var secretKey by remember { mutableStateOf(initialKey?.secretKey ?: "SEC-${(1000..9999).random()}-${(1000..9999).random()}") }
    var active by remember { mutableStateOf(initialKey?.active ?: true) }
    var notes by remember { mutableStateOf(initialKey?.notes ?: "") }

    var selectedPermissions by remember { mutableStateOf(initialKey?.permissions?.toSet() ?: PermissionCatalog.getDefaultCollectorPermissions().toSet()) }

    var expiryOption by remember {
        mutableStateOf(
            if (initialKey?.expiresAt == null) "NEVER" else "CUSTOM"
        )
    }

    var customExpiresAt by remember { mutableStateOf(initialKey?.expiresAt) }

    val rolesList = listOf("ADMIN", "OPERATOR")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialKey == null) "إنشاء مفتاح سري جديد" else "تعديل بيانات المفتاح والصلاحيات",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم الموظف / المستخدم *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Role Selector: Admin or Sub Account (فرعي) only
                Text("نوع الحساب / الدور:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rolesList.forEach { r ->
                        val selected = role == r
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    role = r
                                    if (r == "ADMIN") {
                                        selectedPermissions = PermissionCatalog.getDefaultAdminPermissions().toSet()
                                    } else {
                                        selectedPermissions = PermissionCatalog.getDefaultCollectorPermissions().toSet()
                                    }
                                }
                        ) {
                            Text(
                                text = when(r) {
                                    "ADMIN" -> "مدير (Admin)"
                                    else -> "حساب فرعي (Sub)"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Secret Key Field & Auto-generate Button
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = { secretKey = it },
                    label = { Text("المفتاح السري (Secret Key)") },
                    trailingIcon = {
                        IconButton(onClick = {
                            secretKey = "SEC-${(1000..9999).random()}-${(1000..9999).random()}"
                        }) {
                            Icon(Icons.Filled.Autorenew, contentDescription = "توليد تلقائي")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Expiration Picker Buttons
                Text("تاريخ الانتهاء:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "NEVER" to "دائم (بدون)",
                        "30_DAYS" to "بعد شهر",
                        "1_YEAR" to "بعد سنة"
                    ).forEach { (opt, label) ->
                        val selected = expiryOption == opt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    expiryOption = opt
                                    customExpiresAt = when (opt) {
                                        "30_DAYS" -> System.currentTimeMillis() + 30L * 24 * 3600 * 1000
                                        "1_YEAR" -> System.currentTimeMillis() + 365L * 24 * 3600 * 1000
                                        else -> null
                                    }
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Preset Action Bar for Permissions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جدول الصلاحيات الدقيقة (${selectedPermissions.size}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricBlue
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { selectedPermissions = PermissionCatalog.getAllPermissionKeys().toSet() },
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("تحديد الكل", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { selectedPermissions = emptySet() },
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("إلغاء الكل", fontSize = 11.sp, color = ErrorRed)
                        }
                    }
                }

                // Permissions List with Accordion Checkboxes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PermissionCatalog.allCategories) { category ->
                        var categoryExpanded by remember { mutableStateOf(true) }

                        val categoryKeys = category.items.map { it.key }
                        val isAllCategorySelected = categoryKeys.all { selectedPermissions.contains(it) }

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    .clickable { categoryExpanded = !categoryExpanded }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isAllCategorySelected,
                                        onCheckedChange = { checked ->
                                            selectedPermissions = if (checked) {
                                                selectedPermissions + categoryKeys
                                            } else {
                                                selectedPermissions - categoryKeys.toSet()
                                            }
                                        }
                                    )
                                    Text(
                                        text = category.titleAr,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "توسيع"
                                )
                            }

                            AnimatedVisibility(visible = categoryExpanded) {
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    category.items.forEach { item ->
                                        val checked = selectedPermissions.contains(item.key)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedPermissions = if (checked) {
                                                        selectedPermissions - item.key
                                                    } else {
                                                        selectedPermissions + item.key
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { isChecked ->
                                                    selectedPermissions = if (isChecked) {
                                                        selectedPermissions + item.key
                                                    } else {
                                                        selectedPermissions - item.key
                                                    }
                                                }
                                            )
                                            Column {
                                                Text(
                                                    text = item.titleAr,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                                if (item.descriptionAr.isNotEmpty()) {
                                                    Text(
                                                        text = item.descriptionAr,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank()) return@Button
                    onSave(
                        initialKey?.id ?: "",
                        secretKey,
                        username,
                        phone,
                        role,
                        selectedPermissions.toList(),
                        active,
                        customExpiresAt,
                        notes
                    )
                },
                enabled = username.isNotBlank() && secretKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("حفظ المفتاح والصلاحيات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
