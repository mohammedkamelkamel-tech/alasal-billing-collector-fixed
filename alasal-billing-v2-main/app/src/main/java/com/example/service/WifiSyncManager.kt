package com.example.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AccessKey
import com.example.data.model.BillEntity
import com.example.data.model.DiscoveredDevice
import com.example.data.model.SyncHistory
import com.example.data.model.UserEntity
import com.example.data.model.PaymentEntity
import com.example.data.repository.LocalAccessKeyRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiSyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val accessKeys: LocalAccessKeyRepository
) {
    companion object {
        private const val TAG = "WifiSyncManager"
        private const val SERVICE_TYPE = "_elecbill._tcp."
        private const val PORT = 47823
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var serverSocket: ServerSocket? = null
    private var isAdvertising = false
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val billListAdapter = moshi.adapter<List<BillEntity>>(Types.newParameterizedType(List::class.java, BillEntity::class.java))
    private val userListAdapter = moshi.adapter<List<UserEntity>>(Types.newParameterizedType(List::class.java, UserEntity::class.java))
    private val keyListAdapter = moshi.adapter<List<AccessKey>>(Types.newParameterizedType(List::class.java, AccessKey::class.java))
    private val paymentListAdapter = moshi.adapter<List<PaymentEntity>>(Types.newParameterizedType(List::class.java, PaymentEntity::class.java))

    private val discoveredDevicesList = mutableListOf<DiscoveredDevice>()
    private var onDevicesUpdated: ((List<DiscoveredDevice>) -> Unit)? = null

    init {
        startServer()
        startAdvertising()
    }

    private fun startServer() {
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                while (true) {
                    val socket = serverSocket!!.accept()
                    handleIncomingConnection(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }.start()
    }

    private fun handleIncomingConnection(socket: Socket) {
        Thread {
            try {
                socket.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                    val writer = PrintWriter(s.getOutputStream(), true)
                    
                    val request = reader.readLine()
                    if (request != null) {
                        kotlinx.coroutines.runBlocking { 
                            val response = processSyncRequest(request)
                            writer.println(response)
                        }
                    }
                    if (request != null) {
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
            }
        }.start()
    }

    private suspend fun processSyncRequest(request: String): String {
        return try {
            val json = org.json.JSONObject(request)
            
            // Extract remote data
            val remoteUsers = userListAdapter.fromJson(json.optJSONArray("users")?.toString() ?: "[]").orEmpty()
            val remoteBills = billListAdapter.fromJson(json.optJSONArray("bills")?.toString() ?: "[]").orEmpty()
            val remoteKeys = keyListAdapter.fromJson(json.optJSONArray("keys")?.toString() ?: "[]").orEmpty()
            val remotePayments = paymentListAdapter.fromJson(json.optJSONArray("payments")?.toString() ?: "[]").orEmpty()

            // Merge local and remote
            mergeData(remoteUsers, remoteBills, remoteKeys, remotePayments)

            // Send back our data
            val localUsers = db.userDao().getAllUsers().first()
            val localBills = db.billDao().getAllBills().first()
            val localKeys = accessKeys.getAllLocalAccessKeys()
            val localPayments = db.paymentDao().getAll().first()
            
            org.json.JSONObject().apply {
                put("status", "success")
                put("users", org.json.JSONArray(userListAdapter.toJson(localUsers)))
                put("bills", org.json.JSONArray(billListAdapter.toJson(localBills)))
                put("keys", org.json.JSONArray(keyListAdapter.toJson(localKeys)))
                put("payments", org.json.JSONArray(paymentListAdapter.toJson(localPayments)))
            }.toString()
            
        } catch (e: Exception) {
            org.json.JSONObject().put("status", "error").put("message", e.message).toString()
        }
    }

    private suspend fun mergeData(users: List<UserEntity>, bills: List<BillEntity>, keys: List<AccessKey>, payments: List<PaymentEntity> = emptyList()) {
        users.forEach { u ->
            val existing = db.userDao().getUserById(u.id)
            if (existing == null) {
                db.userDao().insertUser(u)
            } else {
                // simple conflict resolution: we could check timestamps if we had them. For now, replace.
                db.userDao().insertUser(u)
            }
        }
        
        bills.forEach { b ->
            val existing = db.billDao().getBillById(b.id)
            if (existing == null) {
                db.billDao().insertBill(b)
            } else {
                // لا تستبدل فاتورة محلية أحدث/مدفوعة ببيانات قديمة من جهاز آخر.
                // سجل التحصيل المستقل هو المرجع لكل دفعة.
                val remoteIsNewerPayment = b.paymentAt > existing.paymentAt
                val samePaymentTimeHigherAmount = b.paymentAt == existing.paymentAt && b.paidAmount > existing.paidAmount
                val remoteIsNewerBill = b.createdAt > existing.createdAt
                if (remoteIsNewerPayment || samePaymentTimeHigherAmount || remoteIsNewerBill) {
                    db.billDao().insertBill(b)
                }
            }
        }
        
        payments.forEach { db.paymentDao().insert(it) }

        val currentKeys = accessKeys.getAllLocalAccessKeys()
        keys.forEach { k ->
            val existing = currentKeys.find { it.id == k.id }
            if (existing == null) {
                accessKeys.saveAccessKey(k)
            }
        }
    }

    fun startAdvertising() {
        if (isAdvertising) return
        val deviceName = "جهاز المحصل ${android.os.Build.MODEL}"
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            port = PORT
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                isAdvertising = true
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                isAdvertising = false
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service", e)
        }
    }

    fun startDiscovery(onUpdate: (List<DiscoveredDevice>) -> Unit) {
        onDevicesUpdated = onUpdate
        discoveredDevicesList.clear()
        onUpdate(discoveredDevicesList)

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val device = DiscoveredDevice(
                                id = serviceInfo.host.hostAddress ?: "",
                                name = serviceInfo.serviceName,
                                ipAddress = serviceInfo.host.hostAddress ?: "",
                                port = serviceInfo.port
                            )
                            if (discoveredDevicesList.none { it.ipAddress == device.ipAddress }) {
                                discoveredDevicesList.add(device)
                                onDevicesUpdated?.invoke(discoveredDevicesList.toList())
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                discoveredDevicesList.removeAll { it.name == service.serviceName }
                onDevicesUpdated?.invoke(discoveredDevicesList.toList())
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
        }
    }

    fun stopDiscovery() {
        try {
            if (discoveryListener != null) {
                nsdManager.stopServiceDiscovery(discoveryListener)
                discoveryListener = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stop discovery error", e)
        }
    }

    data class SyncResult(val statusMessage: String, val history: SyncHistory?)

    suspend fun sync(device: DiscoveredDevice): SyncResult = withContext(Dispatchers.IO) {
        try {
            val localUsers = db.userDao().getAllUsers().first()
            val localBills = db.billDao().getAllBills().first()
            val localKeys = accessKeys.getAllLocalAccessKeys()
            val localPayments = db.paymentDao().getAll().first()

            val requestJson = org.json.JSONObject().apply {
                put("users", org.json.JSONArray(userListAdapter.toJson(localUsers)))
                put("bills", org.json.JSONArray(billListAdapter.toJson(localBills)))
                put("keys", org.json.JSONArray(keyListAdapter.toJson(localKeys)))
                put("payments", org.json.JSONArray(paymentListAdapter.toJson(localPayments)))
            }.toString()

            val response = Socket(device.ipAddress, device.port).use { socket ->
                socket.soTimeout = 15000
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer.println(requestJson)
                reader.readLine() ?: ""
            }

            val responseObj = org.json.JSONObject(response)
            if (responseObj.optString("status") == "success") {
                val remoteUsers = userListAdapter.fromJson(responseObj.optJSONArray("users")?.toString() ?: "[]").orEmpty()
                val remoteBills = billListAdapter.fromJson(responseObj.optJSONArray("bills")?.toString() ?: "[]").orEmpty()
                val remoteKeys = keyListAdapter.fromJson(responseObj.optJSONArray("keys")?.toString() ?: "[]").orEmpty()
                val remotePayments = paymentListAdapter.fromJson(responseObj.optJSONArray("payments")?.toString() ?: "[]").orEmpty()

                mergeData(remoteUsers, remoteBills, remoteKeys, remotePayments)

                val sent = localUsers.size + localBills.size
                val received = remoteUsers.size + remoteBills.size
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
                
                val history = SyncHistory(
                    date = sdf.format(Date()),
                    deviceName = device.name,
                    status = "تمت المزامنة بنجاح",
                    sentRecords = sent,
                    receivedRecords = received
                )
                
                SyncResult("تمت المزامنة بنجاح. إرسال: $sent، استقبال: $received", history)
            } else {
                SyncResult("فشل المزامنة: خطأ في الاستجابة", null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            SyncResult("فشل المزامنة: ${e.message}", null)
        }
    }
}
