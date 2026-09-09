package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AccessKey
import com.example.data.model.BillEntity
import com.example.data.model.UserEntity
import com.example.data.model.MeterReadingEntity
import com.example.data.model.PaymentEntity
import com.example.data.repository.LocalAccessKeyRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import androidx.room.withTransaction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

/**
 * مزامنة محلية داخل شبكة Wi‑Fi.
 *
 * جهاز ADMIN يعمل كخادم داخل الشبكة، والأجهزة الأخرى تكتشفه عبر UDP
 * ثم تستخدم TCP لإرسال التغييرات وطلب نسخة حديثة من البيانات.
 *
 * لا توجد خدمة سحابية؛ الاتصال بين الأجهزة محلي فقط.
 */
class LocalNetworkSync(
    private val context: Context,
    private val db: AppDatabase,
    private val accessKeys: LocalAccessKeyRepository
) {
    companion object {
        private const val TAG = "LocalNetworkSync"
        private const val TCP_PORT = 47821
        private const val UDP_PORT = 47822
        private const val DISCOVERY_PREFIX = "ELECTRICITY_BILLING_ADMIN:"
    }

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val billListAdapter = moshi.adapter<List<BillEntity>>(
        Types.newParameterizedType(List::class.java, BillEntity::class.java)
    )
    private val readingListAdapter = moshi.adapter<List<MeterReadingEntity>>(
        Types.newParameterizedType(List::class.java, MeterReadingEntity::class.java)
    )
    private val userListAdapter = moshi.adapter<List<UserEntity>>(
        Types.newParameterizedType(List::class.java, UserEntity::class.java)
    )
    private val paymentListAdapter = moshi.adapter<List<PaymentEntity>>(
        Types.newParameterizedType(List::class.java, PaymentEntity::class.java)
    )
    private val keyListAdapter = moshi.adapter<List<AccessKey>>(
        Types.newParameterizedType(List::class.java, AccessKey::class.java)
    )

    @Volatile private var adminMode = false
    @Volatile private var adminHost: String? = null
    private var server: ServerSocket? = null
    private val paymentMutex = kotlinx.coroutines.sync.Mutex()
    private val adminDeviceSecurity = AdminDeviceSecurity(context)
    @Volatile private var sessionAccessKey: AccessKey? = null

    fun setSessionAccessKey(key: AccessKey?) {
        sessionAccessKey = key
    }

    suspend fun adminPresentOnNetwork(): Boolean = discoverAdmin() != null

    /** اكتشاف جهاز الإدارة فقط؛ لا نعرض أجهزة المحصلين لبعضهم. */
    suspend fun discoverAdminDevice(): com.example.data.model.DiscoveredDevice? {
        val host = discoverAdmin() ?: return null
        return com.example.data.model.DiscoveredDevice(
            id = host,
            name = "جهاز الإدارة",
            ipAddress = host,
            port = TCP_PORT
        )
    }

    /** مزامنة يدوية مع الإدارة فقط. */
    suspend fun syncNowWithAdmin(): Boolean {
        val host = adminHost ?: discoverAdmin().also { adminHost = it } ?: return false
        return requestSnapshot(host)
    }

    fun startAsAdmin() {
        if (adminMode) return
        adminMode = true
        scope.launch { runServer() }
        scope.launch { broadcastAdminPresence() }
    }

    fun startAsClient() {
        adminMode = false
        scope.launch { clientDiscoveryLoop() }
    }

    fun stop() {
        sessionAccessKey = null
        adminMode = false
        server?.close()
        server = null
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    suspend fun saveBill(bill: BillEntity): Boolean {
        if (adminMode) return true
        return sendOperation(
            JSONObjectPayload.upsertBill(billListAdapter.toJson(listOf(bill)))
        )
    }

    suspend fun deleteBill(bill: BillEntity): Boolean {
        if (adminMode) return true
        return sendOperation(JSONObjectPayload.delete("bill", bill.id))
    }

    suspend fun saveMeterReading(reading: MeterReadingEntity): Boolean {
        if (adminMode) return true
        return sendOperation(JSONObjectPayload.upsertReading(readingListAdapter.toJson(listOf(reading))))
    }

    /**
     * إرسال إيصال تحصيل مستقل إلى الإدارة. لا نرسل BillEntity المعدل، لأن ذلك
     * يسمح لبيانات محصل قديمة باستبدال تحصيل محصل آخر. الإدارة هي التي تحسب
     * الرصيد الحالي وتصدر رقم الإيصال النهائي.
     */
    suspend fun registerPayment(payment: PaymentEntity): Boolean {
        if (adminMode) return true
        return sendOperation(JSONObjectPayload.registerPayment(payment))
    }

    suspend fun saveUser(user: UserEntity): Boolean {
        if (adminMode) return true
        return sendOperation(
            JSONObjectPayload.upsertUser(userListAdapter.toJson(listOf(user)))
        )
    }

    suspend fun deleteUser(user: UserEntity): Boolean {
        if (adminMode) return true
        return sendOperation(JSONObjectPayload.delete("user", user.id))
    }

    private suspend fun runServer() {
        try {
            server = ServerSocket(TCP_PORT)
            while (scope.isActive) {
                val socket = server!!.accept()
                scope.launch {
                    socket.use {
                        val reader = BufferedReader(InputStreamReader(it.getInputStream()))
                        val writer = PrintWriter(it.getOutputStream(), true)
                        val request = reader.readLine() ?: return@launch
                        val response = handleRequest(request)
                        writer.println(response)
                    }
                }
            }
        } catch (e: Exception) {
            if (scope.isActive) Log.e(TAG, "Server stopped: ${e.message}")
        }
    }

    private suspend fun handleRequest(request: String): String {
        return try {
            val o = org.json.JSONObject(request)
            if (!adminMode) return org.json.JSONObject().put("ok", false).put("error", "الجهاز غير متاح كإدارة").toString()

            // كل طلب شبكي يجب أن يحمل مفتاح جلسة صحيحاً. صلاحية ADMIN على الشبكة
            // مرتبطة بجهاز الإدارة المعتمد، فلا يكفي معرفة كلمة مرور ADMIN وحدها.
            val authSecret = o.optString("authSecret").trim()
            val clientDeviceId = o.optString("deviceId").trim()
            val key = accessKeys.getAccessKeyBySecret(authSecret)
                ?: return org.json.JSONObject().put("ok", false).put("error", "الجهاز أو مفتاح الدخول غير مصرح به").toString()
            if (!key.isValid()) return org.json.JSONObject().put("ok", false).put("error", "مفتاح الدخول غير فعال").toString()
            if (key.role.equals("ADMIN", ignoreCase = true) && clientDeviceId != adminDeviceSecurity.deviceId) {
                return org.json.JSONObject().put("ok", false).put("error", "هذا الجهاز ليس جهاز الإدارة المعتمد").toString()
            }

            when (o.optString("type")) {
                "GET_SNAPSHOT" -> {
                    syncSnapshotFromJson(
                        users = emptyList(),
                        bills = emptyList(),
                        keys = emptyList(),
                        readings = emptyList()
                    )
                    snapshotJson()
                }
                "UPSERT_BILL" -> {
                    val list = billListAdapter.fromJson(o.optString("data")).orEmpty()
                    list.forEach { incoming ->
                        // لا تسمح بمزامنة فاتورة قديمة باستبدال فاتورة موجودة،
                        // خصوصاً حقول المدفوع/المتبقي التي أصبحت تُدار عبر REGISTER_PAYMENT.
                        if (db.billDao().getBillById(incoming.id) == null) {
                            db.billDao().insertBill(incoming)
                        }
                    }
                    snapshotJson()
                }
                "REGISTER_PAYMENT" -> registerPaymentOnAdmin(o)
                "UPSERT_READING" -> {
                    val list = readingListAdapter.fromJson(o.optString("data")).orEmpty()
                    list.forEach { db.meterReadingDao().insert(it) }
                    snapshotJson()
                }
                "UPSERT_USER" -> {
                    val list = userListAdapter.fromJson(o.optString("data")).orEmpty()
                    list.forEach { db.userDao().insertUser(it) }
                    snapshotJson()
                }
                "DELETE" -> {
                    when (o.optString("entity")) {
                        "bill" -> db.billDao().getBillById(o.optString("id"))?.let { db.billDao().deleteBill(it) }
                        "user" -> db.userDao().getUserById(o.optString("id"))?.let { db.userDao().deleteUser(it) }
                    }
                    snapshotJson()
                }
                else -> org.json.JSONObject().put("ok", false).toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error: ${e.message}")
            org.json.JSONObject().put("ok", false).put("error", e.message ?: "sync error").toString()
        }
    }

    private suspend fun syncSnapshotFromJson(
        users: List<UserEntity>,
        bills: List<BillEntity>,
        keys: List<AccessKey>,
        readings: List<MeterReadingEntity>
    ) {
        // Reserved for future authenticated full snapshot import.
    }

    /** تنفيذ التحصيل على جهاز الإدارة بصورة ذرّية وبترتيب واحد، حتى لو دفع محصلان
     * لنفس الفاتورة في نفس اللحظة. */
    private suspend fun registerPaymentOnAdmin(o: org.json.JSONObject): String = paymentMutex.withLock {
        try {
            val paymentId = o.optString("paymentId")
            if (paymentId.isBlank()) return@withLock org.json.JSONObject().put("ok", false).put("error", "معرف الإيصال مفقود").toString()

            // إعادة إرسال نفس العملية آمن: لا نخصمها مرتين.
            val existingPayment = db.paymentDao().getById(paymentId)
            if (existingPayment != null) return@withLock snapshotJson()

            val billId = o.optString("billId")
            val amount = o.optDouble("amount", 0.0)
            if (billId.isBlank() || amount <= 0.0) {
                return@withLock org.json.JSONObject().put("ok", false).put("error", "بيانات التحصيل غير صحيحة").toString()
            }

            db.withTransaction {
                val bill = db.billDao().getBillById(billId)
                    ?: throw IllegalStateException("الفاتورة غير موجودة في الإدارة")
                val due = if (bill.remainingAmount > 0.0) bill.remainingAmount else bill.totalAmount
                if (due <= 0.0) throw IllegalStateException("الفاتورة مسددة بالكامل")

                val before = due
                val newPaid = bill.paidAmount + amount
                val newRemaining = bill.totalAmount - newPaid
                val status = when {
                    newRemaining <= 0.0 -> com.example.data.model.BillStatus.PAID.name
                    newPaid > 0.0 -> com.example.data.model.BillStatus.PARTIAL.name
                    else -> com.example.data.model.BillStatus.UNPAID.name
                }
                val paymentAt = o.optLong("paymentAt", System.currentTimeMillis())
                val receiptNumber = db.paymentDao().nextReceiptNumber()
                val payment = PaymentEntity(
                    id = paymentId,
                    adminId = bill.adminId,
                    billId = bill.id,
                    userId = bill.userId,
                    invoiceNumber = bill.invoiceNumber,
                    userName = bill.userName,
                    amount = amount,
                    method = o.optString("method", "نقدي"),
                    collectorId = o.optString("collectorId", ""),
                    collectorName = o.optString("collectorName", ""),
                    paymentDate = o.optString("paymentDate", ""),
                    paymentAt = paymentAt,
                    beforeRemaining = before,
                    afterRemaining = newRemaining,
                    receiptNumber = receiptNumber
                )
                db.paymentDao().insert(payment)
                db.billDao().updateBillPayment(
                    bill.id, status, newPaid, newRemaining,
                    payment.paymentDate, payment.method, payment.collectorName, paymentAt
                )
            }
            snapshotJson()
        } catch (e: Exception) {
            Log.e(TAG, "Register payment failed", e)
            org.json.JSONObject().put("ok", false).put("error", e.message ?: "تعذر تسجيل التحصيل").toString()
        }
    }

    private suspend fun snapshotJson(): String {
        val users = db.userDao().getAllUsers().first()
        val bills = db.billDao().getAllBills().first()
        val sessionIsAdmin = sessionAccessKey?.role.equals("ADMIN", ignoreCase = true)
        val keys = accessKeys.getAllLocalAccessKeys()
            // لا نرسل مفتاح ADMIN إلى جهاز محصل إطلاقاً.
            // المحصل لا يحتاجه ولا يجب أن يستطيع تخزينه أو استخدامه.
            .filterNot { it.role.equals("ADMIN", ignoreCase = true) && !sessionIsAdmin }
            .map { key ->
                if (key.role.equals("ADMIN", ignoreCase = true) && key.id != sessionAccessKey?.id) key.copy(secretKey = "") else key
            }
        val readings = db.meterReadingDao().getAll().first()
        val payments = db.paymentDao().getAll().first()
        return org.json.JSONObject().apply {
            put("ok", true)
            put("users", org.json.JSONArray(userListAdapter.toJson(users)))
            put("bills", org.json.JSONArray(billListAdapter.toJson(bills)))
            put("keys", org.json.JSONArray(keyListAdapter.toJson(keys)))
            put("readings", org.json.JSONArray(readingListAdapter.toJson(readings)))
            put("payments", org.json.JSONArray(paymentListAdapter.toJson(payments)))
        }.toString()
    }

    private suspend fun clientDiscoveryLoop() {
        while (scope.isActive) {
            try {
                val host = discoverAdmin()
                if (host != null) {
                    adminHost = host
                    requestSnapshot(host)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Discovery/sync: ${e.message}")
            }
            delay(15_000)
        }
    }

    private suspend fun discoverAdmin(): String? = withContext(Dispatchers.IO) {
        DatagramSocket(UDP_PORT).use { socket ->
            socket.reuseAddress = true
            socket.broadcast = true
            socket.soTimeout = 1500
            val data = ByteArray(512)
            val packet = DatagramPacket(data, data.size)
            try {
                socket.receive(packet)
                val msg = String(packet.data, 0, packet.length)
                if (msg.startsWith(DISCOVERY_PREFIX)) packet.address.hostAddress else null
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun broadcastAdminPresence() {
        while (scope.isActive && adminMode) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val bytes = "$DISCOVERY_PREFIX$TCP_PORT".toByteArray()
                    val packet = DatagramPacket(
                        bytes, bytes.size,
                        InetAddress.getByName("255.255.255.255"), UDP_PORT
                    )
                    socket.send(packet)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Broadcast: ${e.message}")
            }
            delay(3000)
        }
    }

    private suspend fun sendOperation(payload: String): Boolean {
        val host = adminHost ?: discoverAdmin().also { adminHost = it } ?: return false
        val key = sessionAccessKey ?: return false
        return try {
            val securedPayload = org.json.JSONObject(payload).apply {
                put("authSecret", key.secretKey)
                put("deviceId", adminDeviceSecurity.deviceId)
            }.toString()
            val response = request(host, securedPayload)
            val ok = org.json.JSONObject(response).optBoolean("ok", false)
            if (!ok) return false
            applySnapshot(response)
            true
        } catch (e: Exception) {
            Log.d(TAG, "Operation failed: ${e.message}")
            false
        }
    }

    private suspend fun request(host: String, payload: String): String =
        withContext(Dispatchers.IO) {
            java.net.Socket(host, TCP_PORT).use { socket ->
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(payload)
                reader.readLine() ?: ""
            }
        }

    private suspend fun requestSnapshot(host: String): Boolean {
        try {
            val key = sessionAccessKey ?: return false
            val response = request(host, org.json.JSONObject()
                .put("type", "GET_SNAPSHOT")
                .put("authSecret", key.secretKey)
                .put("deviceId", adminDeviceSecurity.deviceId)
                .toString())
            val remote = org.json.JSONObject(response)
            if (!remote.optBoolean("ok")) return false

            // إذا كان هناك إيصال محفوظ على جهاز المحصل ولم يصل للإدارة بعد،
            // أرسله كعملية مستقلة قبل اعتماد snapshot الإدارة. هذا يمنع ضياع
            // التحصيل عند انقطاع الشبكة مباشرة بعد الدفع.
            val remoteIds = paymentListAdapter
                .fromJson(remote.optJSONArray("payments")?.toString() ?: "[]")
                .orEmpty()
                .mapTo(hashSetOf()) { it.id }
            val pending = db.paymentDao().getAll().first().filter { it.receiptNumber == 0L && it.id !in remoteIds }
            if (pending.isNotEmpty()) {
                pending.forEach { payment ->
                    if (!registerPayment(payment)) return@forEach
                }
            } else {
                applySnapshot(response)
            }

            // بعد استلام بيانات الإدارة، أعد إرسال السجلات الموجودة محلياً
            // والتي قد تكون أُنشئت أثناء انقطاع الشبكة. بهذه الطريقة لا يعتمد
            // النظام على نجاح عملية الحفظ الأولى فقط.
            val remoteUsers = userListAdapter.fromJson(remote.optJSONArray("users")?.toString() ?: "[]").orEmpty()
            val remoteBills = billListAdapter.fromJson(remote.optJSONArray("bills")?.toString() ?: "[]").orEmpty()
            val remoteReadings = readingListAdapter.fromJson(remote.optJSONArray("readings")?.toString() ?: "[]").orEmpty()

            val localUsers = db.userDao().getAllUsers().first()
            val missingUsers = localUsers.filter { local -> remoteUsers.none { it.id == local.id } }
            missingUsers.forEach { saveUser(it) }

            val localBills = db.billDao().getAllBills().first()
            val missingBills = localBills.filter { local -> remoteBills.none { it.id == local.id } }
            missingBills.forEach { saveBill(it) }

            val localReadings = db.meterReadingDao().getAll().first()
            val missingReadings = localReadings.filter { local -> remoteReadings.none { it.id == local.id } }
            missingReadings.forEach { saveMeterReading(it) }
        } catch (e: Exception) {
            Log.d(TAG, "Snapshot failed: ${e.message}")
            return false
        }
        return true
    }

    private suspend fun applySnapshot(response: String) {
        val o = org.json.JSONObject(response)
        if (!o.optBoolean("ok")) return

        val users = userListAdapter.fromJson(o.optJSONArray("users")?.toString() ?: "[]").orEmpty()
        val bills = billListAdapter.fromJson(o.optJSONArray("bills")?.toString() ?: "[]").orEmpty()
        val keys = keyListAdapter.fromJson(o.optJSONArray("keys")?.toString() ?: "[]").orEmpty()
        val readings = readingListAdapter.fromJson(o.optJSONArray("readings")?.toString() ?: "[]").orEmpty()
        val payments = paymentListAdapter.fromJson(o.optJSONArray("payments")?.toString() ?: "[]").orEmpty()

        // مهم: لا نستبدل قاعدة بيانات المحصل بالكامل بالـsnapshot.
        // الحذف الجماعي كان سبباً في اختفاء بيانات أُنشئت على المحصل أثناء
        // انقطاع الشبكة قبل أن تصل إلى الإدارة. نستخدم Merge آمن بدلاً منه.
        users.forEach { remote ->
            val local = db.userDao().getUserById(remote.id)
            if (local == null) {
                db.userDao().insertUser(remote)
            }
            // إذا كان السجل موجوداً، تبقى نسخة المحصل المحلية؛ تعديلات المشترك
            // تُرسل للإدارة عبر UPSERT_USER عند دورة المزامنة التالية.
        }

        bills.forEach { remote ->
            val local = db.billDao().getBillById(remote.id)
            when {
                local == null -> db.billDao().insertBill(remote)
                remote.paymentAt > local.paymentAt ||
                    (remote.paymentAt == local.paymentAt && remote.paidAmount > local.paidAmount) -> {
                    // الدفع مصدره الإدارة، لذلك نأخذ حالة السداد الأحدث فقط.
                    db.billDao().insertBill(remote)
                }
                remote.createdAt > local.createdAt && remote.paymentAt <= local.paymentAt -> {
                    // بيانات فاتورة أحدث دون الرجوع للخلف في السداد.
                    db.billDao().insertBill(remote.copy(
                        paidAmount = local.paidAmount,
                        remainingAmount = local.remainingAmount,
                        status = local.status,
                        paymentDate = local.paymentDate,
                        paymentMethod = local.paymentMethod,
                        paymentCollector = local.paymentCollector,
                        paymentAt = local.paymentAt
                    ))
                }
            }
        }

        readings.forEach { remote ->
            if (db.meterReadingDao().getAll().first().none { it.id == remote.id }) {
                db.meterReadingDao().insert(remote)
            }
        }

        // سجل الإيصالات append-only: لا نحذف إيصالات محلية معلقة.
        payments.forEach { remote -> db.paymentDao().insert(remote) }

        // لا نحذف مفاتيح محلية تلقائياً؛ قد يكون هناك مفتاح جديد أُنشئ محلياً
        // أثناء انقطاع الشبكة وسيتم إرساله للإدارة.
        keys.forEach { remote ->
            // لا تسمح أبداً بإدخال مفتاح ADMIN إلى مخزن مفاتيح جهاز غير ADMIN.
            if (remote.role.equals("ADMIN", ignoreCase = true) && !adminDeviceSecurity.isAdminDevice()) return@forEach
            val local = accessKeys.getAllLocalAccessKeys().firstOrNull { it.id == remote.id }
            if (local == null) {
                accessKeys.saveAccessKey(remote)
            } else if (remote.secretKey.isNotBlank() && remote.secretKey != local.secretKey) {
                accessKeys.saveAccessKey(remote)
            }
        }
    }

    private object JSONObjectPayload {
        fun upsertBill(data: String) = org.json.JSONObject()
            .put("type", "UPSERT_BILL").put("data", data).toString()

        fun upsertReading(data: String) = org.json.JSONObject()
            .put("type", "UPSERT_READING").put("data", data).toString()

        fun registerPayment(payment: PaymentEntity) = org.json.JSONObject()
            .put("type", "REGISTER_PAYMENT")
            .put("paymentId", payment.id)
            .put("adminId", payment.adminId)
            .put("billId", payment.billId)
            .put("userId", payment.userId)
            .put("invoiceNumber", payment.invoiceNumber)
            .put("userName", payment.userName)
            .put("amount", payment.amount)
            .put("method", payment.method)
            .put("collectorId", payment.collectorId)
            .put("collectorName", payment.collectorName)
            .put("paymentDate", payment.paymentDate)
            .put("paymentAt", payment.paymentAt)
            .toString()

        fun upsertUser(data: String) = org.json.JSONObject()
            .put("type", "UPSERT_USER").put("data", data).toString()

        fun delete(entity: String, id: String) = org.json.JSONObject()
            .put("type", "DELETE").put("entity", entity).put("id", id).toString()
    }
}
