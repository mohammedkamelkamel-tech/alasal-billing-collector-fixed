package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.data.model.BillEntity
import com.example.data.model.BillStatus
import com.example.data.model.PermissionKeys
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftShadow
import com.example.ui.theme.VibrantGreen
import com.example.utils.CurrencyFormatter

@Composable
fun UserManagementScreen(
    users: List<UserEntity>,
    canPerformAction: (String) -> Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    roleFilter: String,
    onRoleFilterChange: (String) -> Unit,
    onAddUser: (String, String, String, String, String, String, Double) -> Unit,
    onToggleUserStatus: (String, Boolean) -> Unit,
    onDeleteUser: (UserEntity) -> Unit,
    onUpdateUser: (UserEntity) -> Unit = {},
    // 👈 فواتير النظام: تُستخدم لحساب المستحقات لكل مشترك وتحديثها تلقائياً
    bills: List<BillEntity> = emptyList()
) {
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var detailsUser by remember { mutableStateOf<UserEntity?>(null) }
    val context = LocalContext.current

    /**
     * إحصائيات كل مشترك مشتقة من تدفّق الفواتير:
     * تتحدث تلقائياً عند إصدار فاتورة أو تسجيل دفعة أو حذف فاتورة.
     */
    val statsByUser = remember(bills) {
        bills.groupBy { it.userId }.mapValues { (_, userBills) ->
            SubscriberStats(
                totalDue = userBills.filter { BillStatus.isOutstanding(it.status) }
                    .sumOf { it.remainingAmount },
                billsCount = userBills.count { it.status != BillStatus.CARRIED.name },
                unpaidCount = userBills.count { BillStatus.isOutstanding(it.status) },
                totalPaid = userBills.sumOf { it.paidAmount },
                lastPaymentDate = userBills.filter { it.paymentDate.isNotBlank() }
                    .maxByOrNull { it.paymentDate }?.paymentDate.orEmpty()
            )
        }
    }
    val canManageUsers = canPerformAction(PermissionKeys.CAN_MANAGE_USERS)
    // صلاحية التعديل: فقط للمشرف
    val isSupervisor = canPerformAction(PermissionKeys.CAN_MANAGE_USERS)

    val filteredUsers = remember(users, searchQuery, roleFilter) {
        users.filter { user ->
            val matchesSearch = searchQuery.isEmpty() ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true)

            val matchesRole = when (roleFilter) {
                "SUB_ACCOUNT" -> user.role.equals("SUB_ACCOUNT", ignoreCase = true) ||
                        user.role.equals("OPERATOR", ignoreCase = true) ||
                        user.role.equals("COLLECTOR", ignoreCase = true) ||
                        user.role.equals("READER", ignoreCase = true) ||
                        user.role.equals("ACCOUNTANT", ignoreCase = true)
                else -> true
            }
            matchesSearch && matchesRole
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("بحث بالاسم أو البريد الإلكتروني...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("users_search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // شريط فلاتر أفقي بدون خيار "مشرف"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = roleFilter == "ALL",
                    onClick = { onRoleFilterChange("ALL") },
                    label = { Text("الكل", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = RoundedCornerShape(20.dp)
                )
                FilterChip(
                    selected = roleFilter == "SUB_ACCOUNT",
                    onClick = { onRoleFilterChange("SUB_ACCOUNT") },
                    label = { Text("حساب فرعي", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا يوجد مشتركين مطابقون",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers) { user ->
                        UserCard(
                            user = user,
                            canManage = canManageUsers,
                            isSupervisor = isSupervisor,
                            stats = statsByUser[user.id] ?: SubscriberStats(),
                            onCardClick = { detailsUser = user },
                            onStatusToggle = { onToggleUserStatus(user.id, user.isActive) },
                            onEditClick = {
                                editingUser = user
                                showEditUserDialog = true
                            },
                            onDeleteClick = { onDeleteUser(user) }
                        )
                    }
                }
            }
        }

        if (canManageUsers) {
            FloatingActionButton(
                onClick = { showAddUserDialog = true },
                containerColor = VibrantGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_user_fab")
            ) {
                Icon(imageVector = Icons.Filled.PersonAdd, contentDescription = "إضافة مشترك")
            }
        }

        detailsUser?.let { u ->
            SubscriberDetailsDialog(
                user = u,
                stats = statsByUser[u.id] ?: SubscriberStats(),
                onDismiss = { detailsUser = null }
            )
        }

        if (showAddUserDialog) {
            AddUserDialog(
                onDismiss = { showAddUserDialog = false },
                onConfirm = { name, email, role, phone, address, meterNumber, unitPrice ->
                    onAddUser(name, email, role, phone, address, meterNumber, unitPrice)
                    showAddUserDialog = false
                    Toast.makeText(context, "تمت إضافة المشترك بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showEditUserDialog && editingUser != null) {
            EditUserDialog(
                user = editingUser!!,
                onDismiss = {
                    showEditUserDialog = false
                    editingUser = null
                },
                onConfirm = { updatedUser ->
                    onUpdateUser(updatedUser)
                    showEditUserDialog = false
                    editingUser = null
                    Toast.makeText(context, "تم تحديث بيانات المشترك ${updatedUser.name} بنجاح", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun UserCard(
    user: UserEntity,
    canManage: Boolean,
    isSupervisor: Boolean,
    stats: SubscriberStats = SubscriberStats(),
    onCardClick: () -> Unit = {},
    onStatusToggle: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            )
            .clickable { onCardClick() }
            .testTag("user_item_${user.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "الهاتف: ${user.phone}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (user.meterNumber.isNotEmpty()) {
                        Text(
                            text = "رقم العداد: ${user.meterNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // 👈 المبلغ المستحق المختصر بجانب كل مشترك
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "المستحق",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.amount(stats.totalDue),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (stats.totalDue > 0.0) ErrorRed else VibrantGreen,
                        maxLines = 1,
                        modifier = Modifier.testTag("user_due_${user.id}")
                    )
                }
            }

            if (canManage) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (user.isActive) "نشط" else "غير نشط",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isActive) VibrantGreen else ErrorRed,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = user.isActive,
                            onCheckedChange = { onStatusToggle() },
                            modifier = Modifier.testTag("user_status_switch_${user.id}")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // زر التعديل يظهر فقط للمشرف
                        if (isSupervisor) {
                            IconButton(onClick = onEditClick) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "تعديل",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف",
                                tint = ErrorRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, role: String, phone: String, address: String, meterNumber: String, unitPrice: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var meterNumber by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("170") }
    var selectedRole by remember { mutableStateOf(UserRole.SUB_ACCOUNT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مشترك جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / الفرع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("رقم العداد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("سعر الكيلوواط للمشترك") },
                    suffix = { Text("ريال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("نوع الحساب:", style = MaterialTheme.typography.labelSmall)
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("حساب فرعي", fontSize = 12.sp, maxLines = 1) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onConfirm(name, email, selectedRole.name, phone, address, meterNumber, unitPrice.toDoubleOrNull() ?: 170.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("حفظ المشترك", maxLines = 1)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء", maxLines = 1)
            }
        }
    )
}

@Composable
fun EditUserDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onConfirm: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(user.address) }
    var meterNumber by remember { mutableStateOf(user.meterNumber) }
    var unitPrice by remember { mutableStateOf(user.unitPrice.toString().removeSuffix(".0")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات المشترك", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / الفرع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meterNumber,
                    onValueChange = { meterNumber = it },
                    label = { Text("رقم العداد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("سعر الكيلوواط للمشترك") },
                    suffix = { Text("ريال") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        val updated = user.copy(
                            name = name,
                            email = email,
                            phone = phone,
                            address = address,
                            meterNumber = meterNumber,
                            unitPrice = unitPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: user.unitPrice
                        )
                        onConfirm(updated)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("حفظ التعديلات", maxLines = 1)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء", maxLines = 1)
            }
        }
    )
}

/** ملخّص مالي لمشترك واحد، مُشتق من فواتيره. */
data class SubscriberStats(
    val totalDue: Double = 0.0,
    val billsCount: Int = 0,
    val unpaidCount: Int = 0,
    val totalPaid: Double = 0.0,
    val lastPaymentDate: String = ""
)

/**
 * نافذة تفاصيل المشترك: إجمالي المستحقات، عدد الفواتير، وحالة السداد.
 * كل القيم مشتقة من قاعدة البيانات وتتحدث تلقائياً.
 */
@Composable
fun SubscriberDetailsDialog(
    user: UserEntity,
    stats: SubscriberStats,
    onDismiss: () -> Unit
) {
    val paymentState = when {
        stats.billsCount == 0 -> "لا توجد فواتير"
        stats.totalDue <= 0.0 -> "مسدَّد بالكامل"
        stats.totalPaid > 0.0 -> "سداد جزئي"
        else -> "غير مسدَّد"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تفاصيل المشترك - ${user.name}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SubscriberStatRow("رقم الهاتف", user.phone.ifBlank { "-" })
                SubscriberStatRow("العنوان", user.address.ifBlank { "-" })
                SubscriberStatRow("رقم العداد", user.meterNumber.ifBlank { "-" })
                HorizontalDivider()
                SubscriberStatRow("إجمالي المستحقات", CurrencyFormatter.riyal(stats.totalDue))
                SubscriberStatRow("عدد الفواتير", "${stats.billsCount} فاتورة")
                SubscriberStatRow("فواتير غير مسددة", "${stats.unpaidCount} فاتورة")
                SubscriberStatRow("إجمالي المدفوع", CurrencyFormatter.riyal(stats.totalPaid))
                SubscriberStatRow("آخر دفعة", stats.lastPaymentDate.ifBlank { "-" })
                SubscriberStatRow("حالة السداد", paymentState)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
private fun SubscriberStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
