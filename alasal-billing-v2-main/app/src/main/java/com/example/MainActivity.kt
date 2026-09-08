package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTopBar
import com.example.ui.screens.*
import com.example.ui.theme.ElectricityBillingTheme
import com.example.ui.viewmodel.BillingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create Notification Channel for local notifications
        com.example.utils.NotificationHelper.createNotificationChannel(this)
        com.example.utils.AutoBackupWorker.schedule(this)

        setContent {
            val viewModel: BillingViewModel = viewModel()

            val currentAccessKey by viewModel.currentAccessKey.collectAsStateWithLifecycle()
            val allAccessKeys by viewModel.allAccessKeys.collectAsStateWithLifecycle()
            val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()

            val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()

            val users by viewModel.users.collectAsStateWithLifecycle()
            val bills by viewModel.bills.collectAsStateWithLifecycle()
            val payments by viewModel.payments.collectAsStateWithLifecycle()

            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val billFilter by viewModel.billFilter.collectAsStateWithLifecycle()
            val userRoleFilter by viewModel.userRoleFilter.collectAsStateWithLifecycle()
            val keyFilterStatus by viewModel.keyFilterStatus.collectAsStateWithLifecycle()
            val selectedBill by viewModel.selectedBill.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "login"
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshNow()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            ElectricityBillingTheme(darkTheme = darkTheme) {
                val showTopBar = currentAccessKey != null && currentRoute != "login"
                val showBottomNav = currentAccessKey != null && currentRoute in listOf("home", "bills", "collection", "users", "reports")
                val showBackButton = currentRoute in listOf("bill_details", "add_edit_bill", "meter_reading", "reading_reminders", "profile", "settings")

                val screenTitle = when (currentRoute) {
                    "home" -> "الرئيسية"
                    "bills" -> "إدارة الفواتير"
                    "keys" -> "إدارة المفاتيح السرية (ADMIN)"
                    "collection" -> "التحصيل والمقبوضات"
                    "users" -> "إدارة المشتركين"
                    "reports" -> "التقارير والإحصائيات"
                    "settings" -> "إعدادات الصلاحيات"
                    "bill_details" -> "تفاصيل الفاتورة"
                    "add_edit_bill" -> "إضافة / تعديل فاتورة"
                    "meter_reading" -> "قراءة العداد"
                    "reading_reminders" -> "تذكير قراءة العدادات"
                    "profile" -> "الملف الشخصي للجلسة"
                    else -> "نظام فواتير الكهرباء"
                }

                // معالجة زر الرجوع (Back Button Handling):
                // إذا كان المستخدم في شاشة فرعية أو تبويب آخر، فإن الرجوع ينقله إلى "الرئيسية"
                // أما إذا كان في الرئيسية، فيُترك الخيار للنظام لإغلاق التطبيق مباشرة
                if (currentRoute != "login" && currentRoute != "home") {
                    BackHandler {
                        if (currentRoute in listOf("bills", "collection", "keys", "users", "reports", "profile", "settings")) {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            if (!navController.popBackStack()) {
                                navController.navigate("home") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(currentAccessKey) {
                    if (currentAccessKey != null && currentRoute == "login") {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (showTopBar) {
                            AppTopBar(
                                title = screenTitle,
                                currentUserProfile = currentUserProfile,
                                currentAccessKey = currentAccessKey,
                                allAccessKeys = allAccessKeys,
                                onSelectAccessKey = { keyItem -> viewModel.currentAccessKey.value = keyItem },
                                darkTheme = darkTheme,
                                onThemeToggle = { viewModel.toggleDarkTheme() },
                                onProfileClick = { navController.navigate("profile") },
                                onCheckDueDatesClick = { viewModel.checkAndTriggerDueDateNotifications() },
                                showBackButton = showBackButton,
                                onBackClick = { navController.popBackStack() },
                                onSettingsClick = { navController.navigate("settings") }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBottomNav) {
                            AppBottomNavigation(
                                currentRoute = currentRoute,
                                onNavigateTo = { tab ->
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                currentUserProfile = currentUserProfile,
                                canPerformAction = { key -> viewModel.canPerformAction(key) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (currentAccessKey == null) "login" else "home",
                        modifier = Modifier.padding(
                            if (showTopBar || showBottomNav) innerPadding else PaddingValues(0.dp)
                        )
                    ) {
                        composable("login") {
                            LoginScreen(
                                onLoginWithKey = { keyInput -> viewModel.loginWithSecretKey(keyInput) },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            DashboardScreen(
                                currentUserProfile = currentUserProfile,
                                canPerformAction = { key -> viewModel.canPerformAction(key) },
                                bills = bills,
                                users = users,
                                onAddBillClick = { navController.navigate("add_edit_bill") },
                                onBillClick = { bill ->
                                    viewModel.selectBill(bill)
                                    navController.navigate("bill_details")
                                },
                                onMeterReadingClick = { navController.navigate("meter_reading") }
                            )
                        }

                            composable("bills") {
                                BillingScreen(
                                    bills = bills,
                                    users = users,
                                    canPerformAction = { key -> viewModel.canPerformAction(key) },
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    filter = billFilter,
                                    onFilterChange = { viewModel.setBillFilter(it) },
                                    onBillClick = { bill ->
                                        viewModel.selectBill(bill)
                                        navController.navigate("bill_details")
                                    },
                                    onAddBillClick = { navController.navigate("add_edit_bill") },
                                    // الدفع الجزئي: يُمرَّر المبلغ المدفوع فعلياً من نافذة الدفع
                                    onPayClick = { bill, amount, method ->
                                        viewModel.payBill(bill.id, amount, method)
                                        val remaining = (bill.remainingAmount.takeIf { it > 0.0 } ?: bill.totalAmount) - amount
                                        val msg = when {
                                            remaining > 0.0 -> "تم تسجيل دفعة جزئية للفاتورة ${bill.invoiceNumber}، المتبقي ${com.example.utils.CurrencyFormatter.riyal(remaining)}"
                                            remaining < 0.0 -> "تم قبول الدفع الزائد، أصبح لك رصيد ${com.example.utils.CurrencyFormatter.riyal(kotlin.math.abs(remaining))} يُخصم تلقائياً من الفاتورة القادمة"
                                            else -> "تم تسديد الفاتورة ${bill.invoiceNumber} بالكامل"
                                        }
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            composable("collection") {
                                CollectionScreen(
                                    bills = bills,
                                    payments = payments,
                                    canPerformAction = { key -> viewModel.canPerformAction(key) },
                                    onPayClick = { bill, amount, method ->
                                        viewModel.payBill(bill.id, amount, method)
                                        val remaining = (bill.remainingAmount.takeIf { it > 0.0 } ?: bill.totalAmount) - amount
                                        val msg = when {
                                            remaining > 0.0 -> "تم تسجيل تحصيل جزئي، المتبقي ${com.example.utils.CurrencyFormatter.riyal(remaining)}"
                                            remaining < 0.0 -> "تم قبول المبلغ الزائد وأصبح رصيداً للمشترك"
                                            else -> "تم تحصيل الفاتورة ${bill.invoiceNumber} بالكامل"
                                        }
                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            composable("keys") {
                                KeyManagementScreen(
                                    accessKeys = allAccessKeys,
                                    currentAccessKey = currentAccessKey,
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    filterStatus = keyFilterStatus,
                                    onFilterStatusChange = { viewModel.setKeyFilterStatus(it) },
                                    onSaveKey = { id, secret, name, phone, role, perms, active, expiresAt, notes ->
                                        viewModel.saveAccessKey(id, secret, name, phone, role, perms, active, expiresAt, notes)
                                    },
                                    onToggleActive = { keyId, curr -> viewModel.toggleAccessKeyActive(keyId, curr) },
                                    onDeleteKey = { keyId -> viewModel.deleteAccessKey(keyId) },
                                    onRegenerateKey = { keyId -> viewModel.regenerateSecretKey(keyId) },
                                    onUpdateExpiration = { keyId, exp -> viewModel.updateKeyExpiration(keyId, exp) }
                                )
                            }

                            composable("users") {
                                UserManagementScreen(
                                    users = users,
                                    canPerformAction = { key -> viewModel.canPerformAction(key) },
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    roleFilter = userRoleFilter,
                                    onRoleFilterChange = { viewModel.setUserRoleFilter(it) },
                                    onAddUser = { name, email, role, phone, address, meterNumber, unitPrice ->
                                        viewModel.addUser(name, email, role, phone, address, meterNumber, unitPrice)
                                    },
                                    onToggleUserStatus = { id, currentStatus ->
                                        viewModel.toggleUserStatus(id, currentStatus)
                                    },
                                    onDeleteUser = { user ->
                                        viewModel.deleteUser(user)
                                        Toast.makeText(this@MainActivity, "تم حذف المستخدم ${user.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    onUpdateUser = { updatedUser ->
                                        viewModel.updateUser(updatedUser)
                                    },
                                    bills = bills
                                )
                            }

                            composable("reports") {
                                ReportsScreen(bills = bills)
                            }

                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToRBAC = { navController.navigate("keys") },
                                    onNavigateToSync = { navController.navigate("sync") },
                                    onNavigateToReadingReminders = { navController.navigate("reading_reminders") },
                                    onLogoutAllDevices = {
                                        viewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("reading_reminders") {
                                ReadingRemindersScreen(
                                    users = users,
                                    reminders = viewModel.readingReminders.collectAsStateWithLifecycle().value,
                                    onSchedule = { uid, name, at, note -> viewModel.scheduleReadingReminder(uid, name, at, note) },
                                    onDelete = { id -> viewModel.deleteReadingReminder(id) },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("sync") {
                                com.example.ui.screens.SyncScreen(
                                    viewModel = viewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable("bill_details") {
                                selectedBill?.let { bill ->
                                    BillDetailsScreen(
                                        bill = bill,
                                        onBackClick = { navController.popBackStack() },
                                        onPayClick = { b, amount, method ->
                                            viewModel.payBill(b.id, amount, method)
                                            val remaining = (b.remainingAmount.takeIf { it > 0.0 } ?: b.totalAmount) - amount
                                            val msg = when {
                                                remaining > 0.0 -> "تم تسجيل دفعة جزئية، المتبقي ${com.example.utils.CurrencyFormatter.riyal(remaining)}"
                                                remaining < 0.0 -> "تم قبول الدفع الزائد، أصبح لك رصيد ${com.example.utils.CurrencyFormatter.riyal(kotlin.math.abs(remaining))} يُخصم تلقائياً من الفاتورة القادمة"
                                                else -> "تم السداد بالكامل"
                                            }
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        // تعديل الفاتورة متاح للمسؤول فقط، وغير ذلك تظهر رسالة "ليس لديك صلاحية"
                                        canEdit = viewModel.canEditBills(),
                                        onEditClick = { navController.navigate("add_edit_bill") },
                                        onDeleteClick = { b ->
                                            viewModel.deleteBill(b)
                                            Toast.makeText(this@MainActivity, "تم حذف الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    )
                                } ?: run {
                                    navController.popBackStack()
                                }
                            }

    
                        composable("meter_reading") {
                            MeterReadingScreen(
                                users = users,
                                lastReadingFor = { uid -> viewModel.lastReadingForUser(uid) },
                                onSaveReading = { uid, name, current, date, notes, image ->
                                    viewModel.addMeterReading(uid, name, current, date, notes, image)
                                    Toast.makeText(this@MainActivity, "تم حفظ قراءة العداد", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        composable("add_edit_bill") {
                                AddEditBillScreen(
                                    users = users,
                                    lastReadingFor = { uid -> viewModel.lastReadingForUser(uid) },
                                    arrearsFor = { uid -> viewModel.duesForUser(uid) },
                                    onSaveBill = { uId, uName, uPhone, uAddress, prevR, currR, date, notes, unitPrice, readingImageUri ->
                                        viewModel.addBill(uId, uName, uPhone, uAddress, prevR, currR, date, notes, unitPrice, readingImageUri)
                                        navController.popBackStack()
                                    },
                                    onCancel = { navController.popBackStack() }
                                )
                            }

                            composable("profile") {
                                ProfileScreen(
                                    currentAccessKey = currentAccessKey,
                                    allAccessKeys = allAccessKeys,
                                    onSelectAccessKey = { keyItem -> viewModel.currentAccessKey.value = keyItem },
                                    onLogout = {
                                        viewModel.logout()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
