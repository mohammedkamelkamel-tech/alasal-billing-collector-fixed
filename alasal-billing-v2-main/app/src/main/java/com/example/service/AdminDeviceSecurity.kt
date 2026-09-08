package com.example.service

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * يربط صلاحية ADMIN بجهاز الإدارة نفسه.
 * لا يُستخدم ANDROID_ID كسرّ أمني؛ هو فقط هوية جهاز محلية مستقرة نسبياً.
 */
class AdminDeviceSecurity(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("admin_device_security", Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        androidId?.takeIf { it.isNotBlank() } ?: prefs.getString("generated_device_id", null).orEmpty().ifBlank {
            UUID.randomUUID().toString().also { prefs.edit().putString("generated_device_id", it).apply() }
        }
    }

    fun isAdminDevice(): Boolean = prefs.getString("admin_device_id", null) == deviceId

    fun bindAsAdminDevice(): Boolean = prefs.edit().putString("admin_device_id", deviceId).commit()

    fun unbindAdminDevice() {
        prefs.edit().remove("admin_device_id").apply()
    }
}
