package com.example.data.model

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Access Key Data Model for Secret Key Authentication
 */
data class AccessKey(
    val id: String = "",
    val secretKey: String = "",
    val username: String = "",
    val phone: String = "",
    val role: String = "OPERATOR", // ADMIN, SUPERVISOR, OPERATOR, ACCOUNTANT
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
    val expiresAt: Long? = null, // Epoch ms, null means never expires
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "ADMIN",
    val lastLogin: Long? = null,
    val notes: String = ""
) {
    fun isExpired(): Boolean {
        val exp = expiresAt ?: return false
        return System.currentTimeMillis() > exp
    }

    fun isValid(): Boolean {
        return active && !isExpired()
    }

    fun getRoleTitleAr(): String {
        return when (role.uppercase()) {
            "ADMIN" -> "مدير النظام (ADMIN)"
            "SUPERVISOR" -> "مشرف (SUPERVISOR)"
            "ACCOUNTANT" -> "محاسب (ACCOUNTANT)"
            "OPERATOR" -> "محصل / قارئ (OPERATOR)"
            else -> "حساب فرعي"
        }
    }

    fun getRoleColor(): Color {
        return when (role.uppercase()) {
            "ADMIN" -> Color(0xFFD32F2F) // Red
            "SUPERVISOR" -> Color(0xFF9C27B0) // Purple
            "ACCOUNTANT" -> Color(0xFF00897B) // Teal
            "OPERATOR" -> Color(0xFF0288D1) // Blue
            else -> Color(0xFF757575)
        }
    }

    fun getFormattedLastLogin(): String {
        val ts = lastLogin ?: return "لم يسجل دخول بعد"
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
        return sdf.format(Date(ts))
    }

    fun getFormattedExpiresAt(): String {
        val exp = expiresAt ?: return "لا ينتهي (دائم)"
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        return sdf.format(Date(exp))
    }

    fun getFormattedCreatedAt(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        return sdf.format(Date(createdAt))
    }
}
