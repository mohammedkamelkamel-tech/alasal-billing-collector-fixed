package com.example.data.repository

import android.content.Context
import com.example.data.model.AccessKey
import com.example.data.model.PermissionCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * تخزين مفاتيح الدخول محلياً. لا يعتمد على الإنترنت أو خدمة سحابية.
 */
class LocalAccessKeyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("local_access_keys", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    init {
        if (!prefs.contains("keys_json")) saveAll(state.value)
    }

    private fun defaults(): List<AccessKey> = listOf(
        AccessKey(
            id = "key_admin_001",
            secretKey = "SEC-ADMIN-1234",
            username = "مدير النظام",
            role = "ADMIN",
            permissions = PermissionCatalog.getDefaultAdminPermissions(),
            active = true,
            createdBy = "SYSTEM"
        ),
        AccessKey(
            id = "key_coll_003",
            secretKey = "SEC-COLLECTOR-5678",
            username = "محصل الميدان",
            role = "OPERATOR",
            permissions = PermissionCatalog.getDefaultCollectorPermissions(),
            active = true,
            createdBy = "key_admin_001"
        ),
        AccessKey(
            id = "key_acc_004",
            secretKey = "SEC-ACCOUNTANT-9900",
            username = "المحاسب",
            role = "ACCOUNTANT",
            permissions = PermissionCatalog.getDefaultAccountantPermissions(),
            active = true,
            createdBy = "key_admin_001"
        ),
        AccessKey(
            id = "key_sup_002",
            secretKey = "SEC-SUPERVISOR-7788",
            username = "المشرف",
            role = "SUPERVISOR",
            permissions = PermissionCatalog.getDefaultAdminPermissions().filterNot { it == PermissionCatalog.DATA_WIPE },
            active = true,
            createdBy = "key_admin_001"
        ),
        AccessKey(
            id = "key_exp_005",
            secretKey = "SEC-EXPIRED-0000",
            username = "مفتاح تجريبي منتهي الصلاحية",
            role = "OPERATOR",
            permissions = PermissionCatalog.getDefaultCollectorPermissions(),
            active = true,
            expiresAt = System.currentTimeMillis() - 3600000L,
            createdBy = "key_admin_001"
        )
    )

    private fun load(): List<AccessKey> {
        val raw = prefs.getString("keys_json", null) ?: return defaults()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            defaults()
        }
    }

    private fun fromJson(o: JSONObject): AccessKey {
        val permissions = mutableListOf<String>()
        val p = o.optJSONArray("permissions")
        if (p != null) for (i in 0 until p.length()) permissions += p.optString(i)
        return AccessKey(
            id = o.optString("id"),
            secretKey = o.optString("secretKey"),
            username = o.optString("username"),
            phone = o.optString("phone"),
            role = o.optString("role", "OPERATOR"),
            permissions = permissions,
            active = o.optBoolean("active", true),
            expiresAt = if (o.isNull("expiresAt")) null else o.optLong("expiresAt"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            createdBy = o.optString("createdBy", "ADMIN"),
            lastLogin = if (o.isNull("lastLogin")) null else o.optLong("lastLogin"),
            notes = o.optString("notes")
        )
    }

    private fun toJson(key: AccessKey): JSONObject = JSONObject().apply {
        put("id", key.id)
        put("secretKey", key.secretKey)
        put("username", key.username)
        put("phone", key.phone)
        put("role", key.role)
        put("permissions", JSONArray(key.permissions))
        put("active", key.active)
        if (key.expiresAt == null) put("expiresAt", JSONObject.NULL) else put("expiresAt", key.expiresAt)
        put("createdAt", key.createdAt)
        put("createdBy", key.createdBy)
        if (key.lastLogin == null) put("lastLogin", JSONObject.NULL) else put("lastLogin", key.lastLogin)
        put("notes", key.notes)
    }

    private fun saveAll(keys: List<AccessKey>) {
        prefs.edit().putString("keys_json", JSONArray().apply { keys.forEach { put(toJson(it)) } }.toString()).apply()
        state.value = keys
    }

    fun listenAllAccessKeys(): Flow<List<AccessKey>> = state

    fun getAllLocalAccessKeys(): List<AccessKey> = state.value

    suspend fun getAccessKeyBySecret(secretKey: String): AccessKey? =
        state.value.firstOrNull { it.secretKey.equals(secretKey.trim(), ignoreCase = true) }

    suspend fun saveAccessKey(key: AccessKey) {
        val id = key.id.ifBlank { java.util.UUID.randomUUID().toString() }
        saveAll(state.value.filterNot { it.id == id } + key.copy(id = id))
    }

    suspend fun deleteAccessKey(keyId: String) {
        saveAll(state.value.filterNot { it.id == keyId })
    }

    suspend fun toggleAccessKeyActive(keyId: String, currentActive: Boolean) {
        saveAll(state.value.map { if (it.id == keyId) it.copy(active = !currentActive) else it })
    }

    suspend fun updateLastLogin(keyId: String) {
        val now = System.currentTimeMillis()
        saveAll(state.value.map { if (it.id == keyId) it.copy(lastLogin = now) else it })
    }

    suspend fun regenerateSecretKey(keyId: String, newSecretKey: String) {
        saveAll(state.value.map { if (it.id == keyId) it.copy(secretKey = newSecretKey) else it })
    }

    suspend fun updateKeyExpiration(keyId: String, expiresAt: Long?) {
        saveAll(state.value.map { if (it.id == keyId) it.copy(expiresAt = expiresAt) else it })
    }

    fun getAllLocalProfiles() = emptyList<com.example.data.model.UserProfile>()
    fun listenAllProfiles(): Flow<List<com.example.data.model.UserProfile>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
}
