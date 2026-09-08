package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.AccessKey
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed

@Composable
fun ProfileScreen(
    currentAccessKey: AccessKey?,
    allAccessKeys: List<AccessKey>,
    onSelectAccessKey: (AccessKey) -> Unit,
    onLogout: () -> Unit
) {
    val key = currentAccessKey

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(key?.getRoleColor() ?: ElectricBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VpnKey,
                contentDescription = "الملف الشخصي",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key?.username ?: "المستخدم",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "المفتاح السري: ${key?.secretKey ?: "غير محدد"}",
                style = MaterialTheme.typography.bodySmall,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = key?.getRoleColor() ?: ElectricBlue
            ) {
                Text(
                    text = key?.getRoleTitleAr() ?: "حساب فرعي",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }

        // Key Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "بيانات المفتاح والجلسة الحالية (محلي)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailRow(label = "معرف المستند (Doc ID)", value = key?.id ?: "N/A")
                DetailRow(label = "المفتاح السري (Secret Key)", value = key?.secretKey ?: "N/A")
                DetailRow(label = "نوع الحساب (Role)", value = key?.role ?: "N/A")
                DetailRow(label = "رقم الهاتف", value = key?.phone?.ifBlank { "غير محدد" } ?: "N/A")
                DetailRow(label = "تاريخ الانتهاء", value = key?.getFormattedExpiresAt() ?: "دائم")
                DetailRow(label = "آخر تسجيل دخول", value = key?.getFormattedLastLogin() ?: "الآن")
                DetailRow(label = "إجمالي الصلاحيات", value = "${key?.permissions?.size ?: 0} صلاحية تخصصية")
            }
        }

        // Quick Key Switcher Card (فقط لمدير النظام Admin)
        if (key?.role == "ADMIN") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "تبديل المفتاح التجريبي لاختبار الصلاحيات مباشرة (وضع المدير):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAccessKeys.take(4).forEach { keyItem ->
                            val isSelected = keyItem.id == key.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) keyItem.getRoleColor() else keyItem.getRoleColor().copy(alpha = 0.2f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectAccessKey(keyItem) }
                            ) {
                                Text(
                                    text = keyItem.username.split(" ").firstOrNull() ?: keyItem.username,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else keyItem.getRoleColor(),
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Icon(imageVector = Icons.Filled.Logout, contentDescription = "خروج")
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل الخروج بالمفتاح", fontWeight = FontWeight.Bold)
            }
        }
    }
}
