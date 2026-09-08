package com.example.data.repository

import com.example.data.model.AccessKey
import com.example.data.model.PermissionCatalog

sealed class KeyVerificationResult {
    data class Success(
        val accessKey: AccessKey,
        val role: String,
        val isOfflineFallback: Boolean = true
    ) : KeyVerificationResult()
    data class InvalidKey(val message: String = "المفتاح السري غير صحيح") : KeyVerificationResult()
    data class KeyInactive(val message: String = "تم إيقاف هذا المفتاح من قبل الإدارة") : KeyVerificationResult()
    data class KeyExpired(val message: String = "انتهت صلاحية هذا المفتاح السري") : KeyVerificationResult()
    data class Error(val exception: Exception, val message: String) : KeyVerificationResult()
}

/**
 * مصادقة محلية بالكامل. المفاتيح والصلاحيات محفوظة على الجهاز.
 */
class AuthRepository(private val accessKeys: LocalAccessKeyRepository) {

    suspend fun ensureAnonymousAuth() = Unit

    suspend fun verifySecretKey(rawSecretKey: String): KeyVerificationResult {
        val trimmed = rawSecretKey.trim()
        if (trimmed.isBlank()) return KeyVerificationResult.InvalidKey("الرجاء إدخال المفتاح السري")

        val key = accessKeys.getAccessKeyBySecret(trimmed)
            ?: return KeyVerificationResult.InvalidKey("المفتاح السري غير صحيح أو غير موجود في هذا الجهاز")

        if (!key.active) return KeyVerificationResult.KeyInactive()
        if (key.isExpired()) return KeyVerificationResult.KeyExpired("عذراً، انتهت صلاحية هذا المفتاح السري (${key.getFormattedExpiresAt()})")

        accessKeys.updateLastLogin(key.id)
        return KeyVerificationResult.Success(
            accessKey = key.copy(lastLogin = System.currentTimeMillis()),
            role = key.role,
            isOfflineFallback = true
        )
    }

    fun getTestKeys(): List<AccessKey> = accessKeys.getAllLocalAccessKeys()
}
