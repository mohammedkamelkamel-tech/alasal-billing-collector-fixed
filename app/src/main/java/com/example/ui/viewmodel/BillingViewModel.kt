package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.model.AccessKey
import com.example.data.model.BillEntity
import com.example.data.model.MeterReadingEntity
import com.example.data.model.ReadingReminderEntity
import com.example.data.model.BillStatus
import com.example.data.model.PermissionCatalog
import com.example.data.model.PermissionKeys
import com.example.data.model.RolePermissionEntity
import com.example.data.model.RoleType
import com.example.data.model.UserEntity
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.BillingRepository
import com.example.data.repository.LocalAccessKeyRepository
import com.example.data.repository.KeyVerificationResult
import com.example.service.LocalNetworkSync
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BillingRepository(db.userDao(), db.billDao(), db.permissionDao(), db.meterReadingDao(), db.readingReminderDao(), db.paymentDao())
    private val accessKeyRepository = LocalAccessKeyRepository(application)
    private val authRepository = AuthRepository(accessKeyRepository)
    
    private val _discoveredDevices = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.model.DiscoveredDevice>>(emptyList())
    val discoveredDevices: kotlinx.coroutines.flow.StateFlow<List<com.example.data.model.DiscoveredDevice>> = _discoveredDevices

    private val _isDiscovering = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isDiscovering: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDiscovering

    private val _syncStatus = kotlinx.coroutines.flow.MutableStateFlow("")
    val syncStatus: kotlinx.coroutines.flow.StateFlow<String> = _syncStatus

    private val _syncHistory = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.data.model.SyncHistory>>(emptyList())
    val syncHistory: kotlinx.coroutines.flow.StateFlow<List<com.example.data.model.SyncHistory>> = _syncHistory

    fun startDiscovery() {
        viewModelScope.launch {
            _isDiscovering.value = true
            if (currentAccessKey.value?.role == "ADMIN") {
                // جهاز الإدارة لا يبحث عن أجهزة المحصلين ولا يزامن معهم مباشرة.
                _discoveredDevices.value = emptyList()
                _syncStatus.value = "جهاز الإدارة فعال. المحصلون يرسلون بياناتهم إلى الإدارة تلقائياً."
            } else {
                val admin = localNetworkSync.discoverAdminDevice()
                _discoveredDevices.value = admin?.let { listOf(it) } ?: emptyList()
                _syncStatus.value = if (admin != null) "تم العثور على جهاز الإدارة" else "لم يتم العثور على جهاز الإدارة"
            }
            _isDiscovering.value = false
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
    }

    fun syncWithDevice(device: com.example.data.model.DiscoveredDevice) {
        viewModelScope.launch {
            if (currentAccessKey.value?.role == "ADMIN") {
                _syncStatus.value = "المزامنة تبدأ من جهاز المحصل فقط؛ لا تتم مزامنة الإدارة مع محصل آخر."
                return@launch
            }
            _syncStatus.value = "جاري المزامنة مع جهاز الإدارة..."
            val ok = localNetworkSync.syncNowWithAdmin()
            _syncStatus.value = if (ok) "تمت المزامنة مع جهاز الإدارة بنجاح" else "فشل المزامنة مع جهاز الإدارة"
        }
    }

    private val localNetworkSync = LocalNetworkSync(application, db, accessKeyRepository)
    private val adminDeviceSecurity = com.example.service.AdminDeviceSecurity(application)

    // 👈 يُعلن عند فشل آخر عملية مزامنة عبر Wi‑Fi محلية (حفظ/حذف) حتى لا يبقى الفشل صامتاً
    // كما كان سابقاً (كانت الأخطاء تُطبع في Logcat فقط دون أي إشعار).
    val lastSyncError = MutableStateFlow<String?>(null)

    // Currently logged-in AccessKey session (Secret Key Auth)
    val currentAccessKey = MutableStateFlow<AccessKey?>(null)

    // Flow of all Access Keys stored locally and synchronized over Wi-Fi
    val allAccessKeys: StateFlow<List<AccessKey>> = accessKeyRepository.listenAllAccessKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), accessKeyRepository.getAllLocalAccessKeys())

    // Active logged in user profile (derived/mapped from currentAccessKey)
    val currentUserProfile = currentAccessKey.map { key ->
        if (key != null) {
            val roleType = if (key.role == "ADMIN" || key.role == "SUPERVISOR") RoleType.SUPERVISOR else RoleType.SUB_ACCOUNT
            val permMap = mutableMapOf<String, Boolean>()
            key.permissions.forEach { permMap[it] = true }
            // Legacy permission mapping
            permMap[PermissionKeys.CAN_ADD_BILL] = key.permissions.contains(PermissionCatalog.INVOICES_CREATE) || key.permissions.contains(PermissionCatalog.READINGS_ADD) || key.role == "ADMIN"
            permMap[PermissionKeys.CAN_PAY_BILL] = key.permissions.contains(PermissionCatalog.PAYMENTS_COLLECT) || key.role == "ADMIN"
            permMap[PermissionKeys.CAN_MANAGE_USERS] = key.permissions.contains(PermissionCatalog.KEYS_VIEW) || key.role == "ADMIN"
            permMap[PermissionKeys.CAN_VIEW_REPORTS] = key.permissions.contains(PermissionCatalog.REPORTS_VIEW) || key.role == "ADMIN"

            UserProfile(
                uid = key.id,
                name = key.username,
                email = "${key.secretKey.lowercase()}@electricity.billing",
                phone = key.phone,
                address = "صنعاء، اليمن",
                roleType = roleType,
                supervisorUid = key.createdBy,
                permissions = permMap,
                isActive = key.active,
                lastLogin = key.getFormattedLastLogin(),
                joinDate = key.getFormattedCreatedAt()
            )
        } else {
            // Default placeholder profile before login
            UserProfile(
                uid = "guest",
                name = "زائر",
                email = "guest@system",
                phone = "",
                address = "",
                roleType = RoleType.SUB_ACCOUNT,
                permissions = emptyMap()
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserProfile(uid = "guest", name = "زائر", email = "guest@system", phone = "", address = "", roleType = RoleType.SUB_ACCOUNT, permissions = emptyMap())
    )

    val currentRole = currentUserProfile.map { profile ->
        if (profile.roleType == RoleType.SUPERVISOR) UserRole.SUPERVISOR else UserRole.SUB_ACCOUNT
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.SUPERVISOR)

    val searchQuery = MutableStateFlow("")
    val billFilter = MutableStateFlow("ALL") // ALL, UNPAID, OVERDUE, PAID
    val userRoleFilter = MutableStateFlow("ALL") // ALL, SUPERVISOR, SUB_ACCOUNT
    val keyFilterStatus = MutableStateFlow("ALL") // ALL, ACTIVE, DISABLED, EXPIRED

    val selectedBill = MutableStateFlow<BillEntity?>(null)

    val allProfiles: StateFlow<List<UserProfile>> = allAccessKeys.map { keys ->
        keys.map { key ->
            val roleType = if (key.role == "ADMIN" || key.role == "SUPERVISOR") RoleType.SUPERVISOR else RoleType.SUB_ACCOUNT
            UserProfile(
                uid = key.id,
                name = key.username,
                email = "${key.secretKey.lowercase()}@electricity.billing",
                phone = key.phone,
                address = "",
                roleType = roleType,
                supervisorUid = key.createdBy,
                permissions = key.permissions.associateWith { true },
                isActive = key.active,
                lastLogin = key.getFormattedLastLogin(),
                joinDate = key.getFormattedCreatedAt()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subAccounts: StateFlow<List<UserProfile>> = allProfiles.map { list ->
        list.filter { it.roleType == RoleType.SUB_ACCOUNT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    val darkTheme = MutableStateFlow(prefs.getBoolean("setting_dark_theme", false))

    val language = MutableStateFlow(prefs.getString("setting_language", "العربية") ?: "العربية")
    val newBillNotification = MutableStateFlow(prefs.getBoolean("setting_new_bill_notif", true))
    val paymentNotification = MutableStateFlow(prefs.getBoolean("setting_payment_notif", true))
    val notificationSound = MutableStateFlow(prefs.getBoolean("setting_notif_sound", true))

    val orgName = MutableStateFlow(prefs.getString("setting_org_name", "شركة الكهرباء الأهلية") ?: "شركة الكهرباء الأهلية")
    val orgPhone = MutableStateFlow(prefs.getString("setting_org_phone", "") ?: "")
    val orgAddress = MutableStateFlow(prefs.getString("setting_org_address", "") ?: "")
    val currency = MutableStateFlow(prefs.getString("setting_currency", "ريال يمني") ?: "ريال يمني")

    fun updateSettingString(key: String, value: String, flow: MutableStateFlow<String>) {
        flow.value = value
        prefs.edit().putString(key, value).apply()
    }

    fun updateSettingBoolean(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        flow.value = value
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getPreferenceBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun getPreferenceString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun updatePreferenceBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun updatePreferenceString(key: String, value: String) { prefs.edit().putString(key, value).apply() }


    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _bills = MutableStateFlow<List<BillEntity>>(emptyList())
    val bills: StateFlow<List<BillEntity>> = _bills.asStateFlow()

    private val _meterReadings = MutableStateFlow<List<MeterReadingEntity>>(emptyList())
    val meterReadings: StateFlow<List<MeterReadingEntity>> = _meterReadings.asStateFlow()

    val readingReminders: StateFlow<List<ReadingReminderEntity>> = repository.activeReadingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<com.example.data.model.PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** مراقبة Room بشكل مستمر، مع تحديث الواجهة فور تغيّر البيانات. */
    private fun observeDatabaseChanges() {
        viewModelScope.launch {
            repository.allUsers.collect { _users.value = it }
        }
        viewModelScope.launch {
            repository.allBills.collect { _bills.value = it.sortedWith(compareByDescending<BillEntity> { it.createdAt }.thenByDescending { it.issueDate }) }
        }
        viewModelScope.launch {
            repository.allMeterReadings.collect { _meterReadings.value = it }
        }
    }

    /** إعادة تحميل فورية من قاعدة البيانات نفسها بعد أي عملية كتابة. */
    private suspend fun refreshDataNow() {
        _refreshUsersFromDb()
        _refreshBillsFromDb()
    }

    private suspend fun _refreshUsersFromDb() {
        _users.value = db.userDao().getAllUsers().first()
    }

    private suspend fun _refreshBillsFromDb() {
        _bills.value = db.billDao().getAllBills().first()
    }

    private suspend fun _refreshMeterReadingsFromDb() {
        _meterReadings.value = db.meterReadingDao().getAll().first()
    }

    /** تحديث صريح عند العودة للتطبيق أو تغيير التبويب، بدون الحاجة للخروج والدخول. */
    fun refreshNow() {
        viewModelScope.launch { refreshDataNow(); _refreshMeterReadingsFromDb() }
    }

    init {
        observeDatabaseChanges()
        viewModelScope.launch {
            DatabaseInitializer.seedIfNeeded(db)
            refreshDataNow()

            // استعادة جلسة المفتاح محلياً. لا يوجد اعتماد على الإنترنت.
            val savedKey = prefs.getString("saved_secret_key", null)
            if (savedKey != null) {
                val savedLocal = accessKeyRepository.getAccessKeyBySecret(savedKey)
                // حماية إضافية: أي جلسة ADMIN محفوظة من إصدار قديم تُمسح فوراً
                // على جهاز غير معتمد، قبل أن تظهر واجهة الإدارة ولو للحظات.
                if (savedLocal?.role.equals("ADMIN", ignoreCase = true) && !adminDeviceSecurity.isAdminDevice()) {
                    prefs.edit().remove("saved_secret_key").apply()
                    currentAccessKey.value = null
                    localNetworkSync.setSessionAccessKey(null)
                    localNetworkSync.stop()
                } else {
                    loginWithSecretKey(savedKey)
                }
            }
        }
    }

    /**
     * 👈 جديد: خريطة (معرّف المشترك -> إجمالي المستحقات).
     * تُشتق مباشرة من تدفّق الفواتير، لذلك تتحدث تلقائياً بعد إصدار فاتورة أو
     * دفع جزئي أو دفع كامل أو حذف فاتورة، دون أي استدعاء يدوي.
     */
    val subscriberDues: StateFlow<Map<String, Double>> = repository.allBills
        .map { list ->
            list.filter { BillStatus.isOutstanding(it.status) }
                .groupBy { it.userId }
                .mapValues { entry -> entry.value.sumOf { it.remainingAmount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** إجمالي مستحقات مشترك (0.0 إن لم توجد مستحقات). */
    fun duesForUser(userId: String): Double = subscriberDues.value[userId] ?: 0.0

    /** آخر قراءة عداد محفوظة للمشترك — تُملأ تلقائياً كقراءة سابقة. */
    fun lastReadingForUser(userId: String): Double =
        meterReadings.value.filter { it.userId == userId }.maxByOrNull { it.createdAt }?.currentReading
            ?: bills.value.filter { it.userId == userId }.maxByOrNull { it.createdAt }?.currentReading
            ?: 0.0

    /** آخر دفعة مسجّلة للمشترك (التاريخ + المبلغ)، أو null إن لم توجد. */
    fun lastPaymentForUser(userId: String): Pair<String, Double>? =
        bills.value
            .filter { it.userId == userId && it.paidAmount > 0.0 && it.paymentDate.isNotBlank() }
            .maxByOrNull { it.paymentDate }
            ?.let { it.paymentDate to it.paidAmount }

    /**
     * 👈 سياسة النظام: تعديل الفواتير ممنوع لجميع الحسابات ما عدا المسؤول (ADMIN).
     * تُستخدم في الواجهة لمنع فتح شاشة التعديل وإظهار رسالة الرفض.
     */
    fun canEditBills(): Boolean = currentAccessKey.value?.role == "ADMIN"

    val permissionsForCurrentRole: StateFlow<List<RolePermissionEntity>> = currentRole
        .flatMapLatest { role -> repository.getPermissionsByRole(role.name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 👈 دالة جديدة: تُحدّد معرّف "المؤسسة" (adminId) الذي يجب ربط أي فاتورة أو
     * مشترك جديد به، بغض النظر عمّن قام فعلياً بالإنشاء (أدمن أو حساب فرعي).
     *
     * القاعدة: إن كان المستخدم الحالي هو ADMIN، فمعرّفه هو نفسه معرّف المؤسسة.
     * إن كان حساباً فرعياً (SUPERVISOR / OPERATOR / ACCOUNTANT)، يتم إسناد
     * البيانات إلى أول حساب بدور ADMIN موجود في قائمة المفاتيح الحالية (allAccessKeys)
     * - أي "الأدمن الرئيسي للمؤسسة" - بدلاً من ترك الحقل فارغاً كما كان يحدث سابقاً.
     *
     * هذا يُكمل الحقل adminId الذي كان مُضافاً في BillEntity (وأضفناه أيضاً في
     * UserEntity) لكنه لم يكن يُملأ فعلياً عند الإنشاء.
     */
    private fun resolveOrganizationAdminId(): String {
        val current = currentAccessKey.value ?: return ""
        if (current.role == "ADMIN") return current.id
        val primaryAdmin = allAccessKeys.value.firstOrNull { it.role == "ADMIN" }
        return primaryAdmin?.id ?: current.id
    }

    /**
     * Secret Key Login Logic via AuthRepository (محلي + مزامنة Wi‑Fi)
     */
    suspend fun loginWithSecretKey(secretKeyInput: String): Pair<Boolean, String> {
        return when (val result = authRepository.verifySecretKey(secretKeyInput)) {
            is KeyVerificationResult.Success -> {
                if (result.accessKey.role.equals("ADMIN", ignoreCase = true)) {
                    // مفتاح ADMIN وحده لا يمنح أي جهاز جديد صلاحية الإدارة.
                    // جهاز الإدارة يجب أن يكون قد تم اعتماده مسبقاً على هذا الجهاز.
                    // لا نقوم أبداً بربط جهاز جديد تلقائياً، لأن ذلك يسمح لجوال المحصل
                    // بالدخول كإدارة بمجرد معرفة مفتاح ADMIN.
                    if (!adminDeviceSecurity.isAdminDevice()) {
                        return Pair(false, "تم رفض دخول الإدارة: هذا الجهاز غير معتمد كجهاز إدارة. يجب اعتماد الجهاز من جهاز الإدارة الأصلي أولاً.")
                    }
                }

                currentAccessKey.value = result.accessKey
                localNetworkSync.setSessionAccessKey(result.accessKey)
                prefs.edit().putString("saved_secret_key", secretKeyInput).apply()
                if (result.accessKey.role.equals("ADMIN", ignoreCase = true)) {
                    localNetworkSync.startAsAdmin()
                } else {
                    localNetworkSync.startAsClient()
                }
                Pair(true, "تم تسجيل الدخول بنجاح كـ (${result.accessKey.username}) [${result.role}]")
            }
            is KeyVerificationResult.KeyInactive -> Pair(false, result.message)
            is KeyVerificationResult.KeyExpired -> Pair(false, result.message)
            is KeyVerificationResult.InvalidKey -> Pair(false, result.message)
            is KeyVerificationResult.Error -> Pair(false, result.message)
        }
    }

    /**
     * Dynamic Permission Check for UI & Repository logic
     */
    fun hasPermission(permissionKey: String): Boolean {
        val key = currentAccessKey.value ?: return false
        if (key.role == "ADMIN") return true

        // Direct key check
        if (key.permissions.contains(permissionKey)) return true

        // Legacy key mappings fallback
        return when (permissionKey) {
            PermissionKeys.CAN_ADD_BILL -> key.permissions.contains(PermissionCatalog.INVOICES_CREATE) || key.permissions.contains(PermissionCatalog.READINGS_ADD)
            PermissionKeys.CAN_PAY_BILL -> key.permissions.contains(PermissionCatalog.PAYMENTS_COLLECT)
            PermissionKeys.CAN_MANAGE_USERS -> key.permissions.contains(PermissionCatalog.KEYS_VIEW) || key.permissions.contains(PermissionCatalog.KEYS_CREATE)
            PermissionKeys.CAN_VIEW_REPORTS -> key.permissions.contains(PermissionCatalog.REPORTS_VIEW)
            else -> false
        }
    }

    fun canPerformAction(permissionKey: String): Boolean {
        return hasPermission(permissionKey)
    }

    /**
     * Key Management Actions (ADMIN / SUPERVISOR)
     */
    fun saveAccessKey(
        id: String = "",
        secretKey: String,
        username: String,
        phone: String,
        role: String,
        permissions: List<String>,
        active: Boolean = true,
        expiresAt: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val keyToSave = AccessKey(
                id = id,
                secretKey = secretKey,
                username = username,
                phone = phone,
                role = role,
                permissions = permissions,
                active = active,
                expiresAt = expiresAt,
                createdAt = System.currentTimeMillis(),
                createdBy = currentAccessKey.value?.username ?: "ADMIN",
                notes = notes
            )
            accessKeyRepository.saveAccessKey(keyToSave)
        }
    }

    fun toggleAccessKeyActive(keyId: String, currentActive: Boolean) {
        viewModelScope.launch {
            accessKeyRepository.toggleAccessKeyActive(keyId, currentActive)
        }
    }

    fun deleteAccessKey(keyId: String) {
        viewModelScope.launch {
            accessKeyRepository.deleteAccessKey(keyId)
        }
    }

    fun regenerateSecretKey(keyId: String) {
        viewModelScope.launch {
            val randomSuffix = (1000..9999).random()
            val newKey = "SEC-${(1000..9999).random()}-$randomSuffix"
            accessKeyRepository.regenerateSecretKey(keyId, newKey)
        }
    }

    fun updateKeyExpiration(keyId: String, newExpiresAt: Long?) {
        viewModelScope.launch {
            accessKeyRepository.updateKeyExpiration(keyId, newExpiresAt)
        }
    }

    /**
     * Switch current user profile / access key (for quick role switcher demo pill)
     */
    fun setCurrentUserProfile(profile: UserProfile) {
        val foundKey = allAccessKeys.value.find { it.id == profile.uid }
        if (foundKey != null) {
            currentAccessKey.value = foundKey
        }
    }

    fun setRole(role: UserRole) {
        val targetRole = if (role == UserRole.SUPERVISOR) "ADMIN" else "OPERATOR"
        val found = allAccessKeys.value.firstOrNull { it.role == targetRole && it.isValid() }
        if (found != null) {
            currentAccessKey.value = found
        }
    }

    fun updateSubAccountPermissions(subAccountUid: String, newPermissions: Map<String, Boolean>) {
        viewModelScope.launch {
            val permList = newPermissions.filterValues { it }.keys.toList()
            val key = allAccessKeys.value.find { it.id == subAccountUid }
            if (key != null) {
                accessKeyRepository.saveAccessKey(key.copy(permissions = permList))
            }
        }
    }

    fun logout() {
        currentAccessKey.value = null
        localNetworkSync.stop()
        prefs.edit().remove("saved_secret_key").apply()
    }

    fun toggleDarkTheme() {
        updateSettingBoolean("setting_dark_theme", !darkTheme.value, darkTheme)
    }

    fun setDarkTheme(enabled: Boolean) {
        updateSettingBoolean("setting_dark_theme", enabled, darkTheme)
    }

    fun setBillFilter(filter: String) {
        billFilter.value = filter
    }

    fun setUserRoleFilter(filter: String) {
        userRoleFilter.value = filter
    }

    fun setKeyFilterStatus(status: String) {
        keyFilterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectBill(bill: BillEntity) {
        selectedBill.value = bill
    }

    /**
     * إصدار فاتورة جديدة.
     *
     * التعديلات:
     * - إلغاء ضريبة 10% نهائياً: الإجمالي = قيمة الاستهلاك + المتأخرات.
     * - جلب المتأخرات السابقة للمشترك تلقائياً وترحيلها إلى الفاتورة الجديدة،
     *   مع تعليم الفواتير القديمة بأنها "مُرحّلة" لمنع ازدواج احتساب المستحقات.
     * - إن لم تُمرَّر قراءة سابقة، تُجلب آخر قراءة محفوظة للمشترك تلقائياً.
     * - تاريخ الإصدار وتاريخ الاستحقاق يُحسبان تلقائياً (الاستحقاق بعد 15 يوماً).
     */
    fun scheduleReadingReminder(userId: String, userName: String, reminderAt: Long, note: String = "") {
        viewModelScope.launch {
            val reminder = ReadingReminderEntity(
                adminId = resolveOrganizationAdminId(),
                userId = userId,
                userName = userName,
                reminderAt = reminderAt,
                note = note
            )
            repository.insertReadingReminder(reminder)
            com.example.utils.ReadingReminderWorker.schedule(getApplication(), reminder.id, userName, reminderAt, note)
        }
    }

    fun deleteReadingReminder(id: String) {
        viewModelScope.launch {
            repository.deleteReadingReminder(id)
            androidx.work.WorkManager.getInstance(getApplication()).cancelUniqueWork("reading_reminder_$id")
        }
    }

    fun addMeterReading(
        userId: String,
        userName: String,
        currentReading: Double,
        readingDate: String,
        notes: String = "",
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val previous = lastReadingForUser(userId)
            val reading = MeterReadingEntity(
                adminId = resolveOrganizationAdminId(),
                userId = userId,
                userName = userName,
                previousReading = previous,
                currentReading = currentReading,
                readingDate = readingDate,
                notes = notes,
                readerName = currentAccessKey.value?.username.orEmpty(),
                imageUri = imageUri,
                createdAt = System.currentTimeMillis()
            )
            repository.insertMeterReading(reading)
            _refreshMeterReadingsFromDb()
            val synced = localNetworkSync.saveMeterReading(reading)
            lastSyncError.value = if (synced) null else "تعذّرت مزامنة قراءة العداد عبر شبكة Wi‑Fi المحلية"
        }
    }

    fun addBill(
        userId: String,
        userName: String,
        userPhone: String,
        userAddress: String,
        prevReading: Double,
        currentReading: Double,
        readingDate: String,
        notes: String,
        unitPrice: Double = 170.0,
        readingImageUri: String? = null
    ) {
        viewModelScope.launch {
            val effectivePrev = if (prevReading > 0.0) prevReading else repository.getLastReadingForUser(userId)
            // السعر يؤخذ من بطاقة المشترك نفسها، ولا يعتمد على إدخال المحصل.
            val subscriber = repository.getUserById(userId)
            val effectiveUnitPrice = subscriber?.unitPrice?.takeIf { it > 0.0 } ?: unitPrice
            val consumption = (currentReading - effectivePrev).coerceAtLeast(0.0)
            val subtotal = consumption * effectiveUnitPrice

            // المتأخرات الموجبة + الرصيد السالب الناتج عن الدفع الزائد.
            // مثال: فاتورة 200 ودفع 1000 => رصيد للمشترك 800، فيُخصم تلقائياً من القراءة القادمة.
            val allPreviousBills = repository.getBillsForUserOnce(userId)
            val outstanding = allPreviousBills.filter {
                it.status !in listOf(BillStatus.PAID.name, BillStatus.CARRIED.name) && it.remainingAmount > 0.0
            }
            val arrears = outstanding.sumOf { it.remainingAmount }
            val availableCredit = allPreviousBills
                .filter { it.remainingAmount < 0.0 }
                .sumOf { -it.remainingAmount }

            // الرصيد الزائد يُخصم من قيمة القراءة الحالية. لا نسمح بإجمالي سالب؛
            // إذا كان الرصيد أكبر من الفاتورة، يُرحّل الفرق كرصيد سالب للفاتورة التالية.
            val netTotal = subtotal + arrears - availableCredit
            val total = netTotal.coerceAtLeast(0.0)
            val carriedCredit = (-netTotal).coerceAtLeast(0.0)
            val invNum = "#${(12346..99999).random()}"

            val issueDate = readingDate.ifBlank { todayFormatted() }
            val dueDate = dueDateFrom(issueDate)

            val bill = BillEntity(
                adminId = resolveOrganizationAdminId(),
                invoiceNumber = invNum,
                userId = userId,
                userName = userName,
                userPhone = userPhone,
                userAddress = userAddress,
                prevReading = effectivePrev,
                currentReading = currentReading,
                consumptionKwh = consumption,
                unitPrice = effectiveUnitPrice,
                subtotalAmount = subtotal,
                previousDebt = arrears - availableCredit,
                totalAmount = total,
                paidAmount = 0.0,
                remainingAmount = if (carriedCredit > 0.0) -carriedCredit else total,
                issueDate = issueDate,
                dueDate = dueDate,
                status = if (total <= 0.0) BillStatus.PAID.name else BillStatus.UNPAID.name,
                readingDate = issueDate,
                notes = notes,
                readingImageUri = readingImageUri,
                createdAt = System.currentTimeMillis()
            )
            repository.insertBill(bill)
            refreshDataNow()

            // ترحيل الفواتير السابقة حتى لا تُحتسب مستحقاتها أو أرصدتها مرتين.
            // الرصيد الزائد يُنقل إلى الفاتورة الجديدة عبر previousDebt/remainingAmount.
            outstanding.forEach { old ->
                repository.markBillCarriedForward(old.id)
                repository.getBillById(old.id)?.let { localNetworkSync.saveBill(it) }
            }
            allPreviousBills.filter { it.remainingAmount < 0.0 }.forEach { old ->
                repository.markBillCarriedForward(old.id)
                repository.getBillById(old.id)?.let { localNetworkSync.saveBill(it) }
            }
            refreshDataNow()

            val synced = localNetworkSync.saveBill(bill)
            lastSyncError.value = if (synced) null else "تعذّرت مزامنة الفاتورة عبر شبكة Wi‑Fi المحلية، ستتم إعادة المحاولة عند توفر الشبكة"
            try {
                com.example.utils.NotificationHelper.sendNewBillNotification(
                    context = getApplication(),
                    subscriberName = userName,
                    amount = total,
                    billId = bill.id
                )
            } catch (e: Exception) {
                android.util.Log.e("BillingViewModel", "Notification send error: ${e.message}")
            }
        }
    }

    /** تاريخ اليوم بصيغة dd/MM/yyyy — يُستخدم كتاريخ إصدار افتراضي. */
    private fun todayFormatted(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())

    /** تاريخ الاستحقاق = تاريخ الإصدار + 15 يوماً. */
    private fun dueDateFrom(issueDate: String): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val base = try {
            fmt.parse(issueDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val cal = java.util.Calendar.getInstance().apply {
            time = base
            add(java.util.Calendar.DAY_OF_MONTH, 15)
        }
        return fmt.format(cal.time)
    }

    fun checkAndTriggerDueDateNotifications() {
        viewModelScope.launch {
            val currentBills = bills.value
            com.example.utils.NotificationHelper.checkAndNotifyApproachingDueDates(getApplication(), currentBills)
        }
    }

    /**
     * تسجيل دفعة (كاملة أو جزئية) على الفاتورة.
     *
     * لم تعد الفاتورة تُعتبر مدفوعة بمجرد الضغط على زر الدفع؛ تُمرَّر قيمة
     * المبلغ المدفوع فعلياً من نافذة الدفع، ثم:
     *   المتبقي = الإجمالي - المدفوع
     *   المتبقي = 0  -> مدفوعة
     *   المتبقي > 0  -> مدفوعة جزئياً (يُحفظ المتبقي كمتأخرات)
     */
    fun payBill(billId: String, amountPaid: Double, method: String = "نقدي") {
        viewModelScope.launch {
            if (amountPaid <= 0.0) return@launch
            val beforeBill = repository.getBillById(billId) ?: return@launch
            val paymentId = java.util.UUID.randomUUID().toString()
            val dateStr = todayFormatted()
            val collector = currentAccessKey.value
            val collectorName = collector?.username.orEmpty()

            // تُحفظ الدفعة محلياً أولاً لعمل التطبيق بدون انتظار الشبكة، ثم تُرسل
            // كعملية تحصيل مستقلة إلى جهاز الإدارة. لا نرسل فاتورة معدلة كاملة؛
            // لأن ذلك كان سبباً رئيسياً لاختلاط/ضياع تحصيلات المحصلين.
            val updatedBill = repository.registerPayment(billId, amountPaid, dateStr, method, collectorName)
            if (updatedBill != null) {
                val payment = com.example.data.model.PaymentEntity(
                    id = paymentId,
                    adminId = updatedBill.adminId,
                    billId = updatedBill.id,
                    userId = updatedBill.userId,
                    invoiceNumber = updatedBill.invoiceNumber,
                    userName = updatedBill.userName,
                    amount = amountPaid,
                    method = method,
                    collectorId = collector?.id.orEmpty(),
                    collectorName = collectorName,
                    paymentDate = dateStr,
                    paymentAt = System.currentTimeMillis(),
                    beforeRemaining = beforeBill.remainingAmount.takeIf { it > 0 } ?: beforeBill.totalAmount,
                    afterRemaining = updatedBill.remainingAmount,
                    receiptNumber = 0L
                )
                repository.insertPayment(payment)
                selectedBill.value = updatedBill
                refreshDataNow()

                // مزامنة العملية نفسها، وليس حالة فاتورة قديمة من جهاز المحصل.
                val synced = localNetworkSync.registerPayment(payment)
                lastSyncError.value = if (synced) null else "تعذّرت مزامنة إيصال التحصيل مع الإدارة؛ سيبقى محفوظاً على الجهاز حتى تتوفر المزامنة"
            }
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            refreshDataNow()
            localNetworkSync.deleteBill(bill)
            selectedBill.value = null
        }
    }

    fun addUser(
        name: String,
        email: String,
        role: String,
        phone: String,
        address: String,
        meterNumber: String,
        unitPrice: Double
    ) {
        viewModelScope.launch {
            val code = "USER-2026-0${(10..99).random()}"
            val user = UserEntity(
                // 👈 نفس الإصلاح المطبّق على الفواتير: ربط كل مشترك جديد بالمؤسسة/الأدمن
                // الرئيسي الحالي فور إنشائه، بدل ترك الحقل فارغاً كما كان الحال سابقاً.
                adminId = resolveOrganizationAdminId(),
                userIdCode = code,
                name = name,
                email = email,
                role = role,
                phone = phone,
                address = address,
                meterNumber = meterNumber,
                unitPrice = unitPrice.coerceAtLeast(0.0),
                isActive = true
            )
            repository.insertUser(user)
            refreshDataNow()
            val synced = localNetworkSync.saveUser(user)
            lastSyncError.value = if (synced) null else "تعذّرت مزامنة بيانات المشترك عبر شبكة Wi‑Fi المحلية، ستتم إعادة المحاولة عند توفر الشبكة"
        }
    }

    fun toggleUserStatus(userId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.updateUserStatus(userId, !currentStatus)
            refreshDataNow()
            val updatedUser = repository.getUserById(userId)
            if (updatedUser != null) localNetworkSync.saveUser(updatedUser)
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            refreshDataNow()
            localNetworkSync.deleteUser(user)
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            refreshDataNow()
            localNetworkSync.saveUser(user)
        }
    }
    override fun onCleared() {
        localNetworkSync.stop()
        super.onCleared()
    }
}
