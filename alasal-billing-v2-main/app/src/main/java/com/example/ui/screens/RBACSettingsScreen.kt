package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import com.example.data.model.PermissionKeys
import com.example.data.model.RoleType
import com.example.data.model.UserProfile
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.VibrantGreen

@Composable
fun RBACSettingsScreen(
    currentUserProfile: UserProfile,
    subAccounts: List<UserProfile>,
    onUpdateSubAccountPermissions: (String, Map<String, Boolean>) -> Unit
) {
    val context = LocalContext.current
    val isSupervisor = currentUserProfile.roleType == RoleType.SUPERVISOR

    var selectedSubAccountUid by remember(subAccounts) {
        mutableStateOf(subAccounts.firstOrNull()?.uid ?: "")
    }

    val activeSubAccount = remember(subAccounts, selectedSubAccountUid) {
        subAccounts.find { it.uid == selectedSubAccountUid } ?: subAccounts.firstOrNull()
    }

    // Editable map state for selected sub-account permissions
    var localPermissionsMap by remember(activeSubAccount) {
        mutableStateOf(activeSubAccount?.permissions ?: emptyMap())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "إدارة الصلاحيات (RBAC محلي)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "نظام المشرف والحسابات الفرعية - تخصيص خريطة الصلاحيات وحفظها محلياً ومزامنتها عبر Wi‑Fi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Role Type Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = currentUserProfile.roleType.color.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSupervisor) Icons.Filled.AdminPanelSettings else Icons.Filled.Person,
                    contentDescription = "دورك الحسابي",
                    tint = currentUserProfile.roleType.color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "الحساب الحالي: ${currentUserProfile.name}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = currentUserProfile.roleType.color
                    )
                    Text(
                        text = "النوع: ${currentUserProfile.roleType.titleAr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isSupervisor) {
            // Sub Account View: Read-only permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "تنبيه: أنت مسجل بحساب فرعي. المشرف التابع له هو المسؤول عن تعديل صلاحياتك عبر شبكة Wi‑Fi المحلية.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "صلاحياتك الحالية المعينة:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PermissionKeys.allPermissions) { (key, label) ->
                    val isGranted = currentUserProfile.permissions[key] ?: false
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isGranted, onCheckedChange = null, enabled = false)
                        }
                    }
                }
            }
        } else {
            // Supervisor View: Select sub account employee and edit permissions map
            Text(
                text = "اختر الموظف (الحساب الفرعي) للتحكم في صلاحياته:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sub Accounts Row Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                subAccounts.forEach { subAcc ->
                    val isSelected = activeSubAccount?.uid == subAcc.uid
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedSubAccountUid = subAcc.uid
                                localPermissionsMap = subAcc.permissions
                            }
                            .testTag("sub_account_tab_${subAcc.uid}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ElectricBlue else ElectricBlue.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = subAcc.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else ElectricBlue,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            activeSubAccount?.let { targetUser ->
                Text(
                    text = "خريطة الصلاحيات لـ ${targetUser.name}:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(PermissionKeys.allPermissions) { (key, label) ->
                        val isChecked = localPermissionsMap[key] ?: false
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "المفتاح: $key",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val updated = localPermissionsMap.toMutableMap()
                                        updated[key] = checked
                                        localPermissionsMap = updated
                                    },
                                    modifier = Modifier.testTag("switch_perm_$key")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onUpdateSubAccountPermissions(targetUser.uid, localPermissionsMap)
                        Toast.makeText(context, "تم حفظ وتحديث صلاحيات ${targetUser.name} بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_sub_account_permissions_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
                ) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = "حفظ")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تحديث الصلاحيات عبر شبكة Wi‑Fi المحلية",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
