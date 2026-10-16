package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessKey
import com.example.data.model.PermissionCatalog
import com.example.data.model.PermissionKeys
import com.example.data.model.RoleType
import com.example.data.model.UserProfile
import com.example.ui.theme.SoftShadow

enum class ScreenTab(
    val route: String,
    val titleAr: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    HOME("home", "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    BILLS("bills", "الفواتير", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    COLLECTION("collection", "التحصيل", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    USERS("users", "المشتركين", Icons.Filled.People, Icons.Outlined.People),
    REPORTS("reports", "التقارير", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    currentUserProfile: UserProfile,
    currentAccessKey: AccessKey?,
    allAccessKeys: List<AccessKey>,
    onSelectAccessKey: (AccessKey) -> Unit,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onProfileClick: () -> Unit,
    onCheckDueDatesClick: () -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // صلاحية إظهار قائمة تبديل الحسابات: فقط لمدير النظام (ADMIN) أو المشرف الرئيسية
    val isAdmin: Boolean = currentAccessKey?.role == "ADMIN" || currentUserProfile.roleType == RoleType.SUPERVISOR
    var showNotificationsDialog by remember { mutableStateOf(false) }
    val notificationsList = remember(showNotificationsDialog) {
        com.example.utils.NotificationHelper.getNotifications()
    }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (currentAccessKey != null) {
                        "المستخدم: ${currentAccessKey.username}"
                    } else {
                        "نوع الحساب: ${currentUserProfile.roleType.titleAr}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Filled.ElectricBolt,
                        contentDescription = "شعار التطبيق",
                        tint = Color.Yellow
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = { showNotificationsDialog = true },
                modifier = Modifier.testTag("notifications_bell_btn")
            ) {
                BadgedBox(
                    badge = {
                        if (notificationsList.isNotEmpty()) {
                            Badge { Text("${notificationsList.size}") }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "الإشعارات",
                        tint = Color.White
                    )
                }
            }

            IconButton(
                onClick = onThemeToggle,
                modifier = Modifier.testTag("theme_toggle_btn")
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "الوضع الداكن",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.testTag("settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "الإعدادات",
                    tint = Color.White
                )
            }

            // القائمة المنسدلة تظهر فقط لمدير النظام (ADMIN)
            if (isAdmin) {
                val roleMenuExpanded = remember { mutableStateOf(false) }
                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = currentAccessKey?.getRoleColor() ?: currentUserProfile.roleType.color,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { roleMenuExpanded.value = true }
                            .testTag("role_selector_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentAccessKey?.role ?: "المفتاح",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "تغيير المفتاح",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = roleMenuExpanded.value,
                        onDismissRequest = { roleMenuExpanded.value = false }
                    ) {
                        allAccessKeys.forEach { keyItem ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(keyItem.getRoleColor())
                                        )
                                        Column {
                                            Text(
                                                text = keyItem.username,
                                                fontWeight = if (keyItem.id == currentAccessKey?.id) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "(${keyItem.secretKey}) (${keyItem.getRoleTitleAr()})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectAccessKey(keyItem)
                                    roleMenuExpanded.value = false
                                }
                            )
                        }
                    }
                }
            } else {
                // للمستخدمين الفرعيين: إظهار اسمه فقط بدون إمكانية الضغط للتبديل
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = currentAccessKey?.getRoleColor() ?: currentUserProfile.roleType.color,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = currentAccessKey?.getRoleTitleAr() ?: "حساب فرعي",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )

    if (showNotificationsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مركز الإشعارات والتنبيهات", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onCheckDueDatesClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("فحص مواعيد السداد وإرسال التذكيرات المحلية")
                    }

                    if (notificationsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا توجد إشعارات سابقة حتى الآن", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(notificationsList.size) { idx ->
                                val item = notificationsList[idx]
                                androidx.compose.material3.Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(item.message, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("ar")).format(java.util.Date(item.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigateTo: (ScreenTab) -> Unit,
    currentUserProfile: UserProfile,
    canPerformAction: (String) -> Boolean
) {
    val visibleTabs: List<ScreenTab> = ScreenTab.entries.filter { tab ->
        when (tab) {
            ScreenTab.COLLECTION -> {
                canPerformAction(PermissionCatalog.PAYMENTS_COLLECT) ||
                    canPerformAction(PermissionKeys.CAN_PAY_BILL) ||
                    currentUserProfile.roleType == RoleType.SUPERVISOR
            }
            ScreenTab.USERS -> {
                canPerformAction(PermissionCatalog.CUSTOMERS_VIEW) ||
                    canPerformAction(PermissionKeys.CAN_MANAGE_USERS) ||
                    currentUserProfile.roleType == RoleType.SUPERVISOR
            }
            ScreenTab.REPORTS -> {
                canPerformAction(PermissionCatalog.REPORTS_VIEW) ||
                    canPerformAction(PermissionKeys.CAN_VIEW_REPORTS) ||
                    currentUserProfile.roleType == RoleType.SUPERVISOR
            }
            else -> true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = SoftShadow,
                spotColor = SoftShadow
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .height(64.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
            visibleTabs.forEach { tab ->
                val isSelected: Boolean = currentRoute == tab.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigateTo(tab) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                            contentDescription = tab.titleAr,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.titleAr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                )
            }
        }
    }
}