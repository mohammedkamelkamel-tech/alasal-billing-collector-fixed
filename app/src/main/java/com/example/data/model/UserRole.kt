package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class UserRole(
    val titleAr: String,
    val descriptionAr: String,
    val color: Color,
    val roleType: RoleType
) {
    SUPERVISOR("مشرف", "إدارة كافة الصلاحيات الحسابات الفرعية بالنظام", Color(0xFF9C27B0), RoleType.SUPERVISOR),
    SUB_ACCOUNT("حساب فرعي", "حساب موظف مخصص بصلاحيات محددة من المشرف", Color(0xFF0288D1), RoleType.SUB_ACCOUNT);

    companion object {
        fun fromString(roleStr: String): UserRole {
            return entries.find {
                it.name.equals(roleStr, ignoreCase = true) ||
                it.titleAr.contains(roleStr, ignoreCase = true)
            } ?: SUPERVISOR
        }
    }
}
