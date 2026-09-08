package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class RoleType(val titleAr: String, val descriptionAr: String, val color: Color) {
    SUPERVISOR("مشرف (SUPERVISOR)", "يمتلك كافة الصلاحيات الكاملة وإدارة الحسابات الفرعية", Color(0xFF9C27B0)),
    SUB_ACCOUNT("حساب فرعي (SUB_ACCOUNT)", "حساب موظف تابع للمشرف مخصص بخريطة صلاحيات", Color(0xFF0288D1));

    companion object {
        fun fromString(str: String?): RoleType {
            if (str.isNullOrBlank()) return SUPERVISOR
            return entries.find {
                it.name.equals(str, ignoreCase = true) ||
                it.titleAr.contains(str, ignoreCase = true)
            } ?: SUPERVISOR
        }
    }
}

object PermissionKeys {
    const val CAN_ADD_BILL = "canAddBill"
    const val CAN_PAY_BILL = "canPayBill"
    const val CAN_MANAGE_USERS = "canManageUsers"
    const val CAN_VIEW_REPORTS = "canViewReports"

    val allPermissions = listOf(
        CAN_ADD_BILL to "إضافة وتعديل الفواتير وقراءة العداد",
        CAN_PAY_BILL to "تسديد وتحصيل المبالغ",
        CAN_MANAGE_USERS to "إدارة وإنشاء الحسابات الفرعية",
        CAN_VIEW_REPORTS to "عرض التقارير والتقارير المالية"
    )
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val roleType: RoleType = RoleType.SUPERVISOR,
    val supervisorUid: String = "",
    val permissions: Map<String, Boolean> = mapOf(
        PermissionKeys.CAN_ADD_BILL to true,
        PermissionKeys.CAN_PAY_BILL to true,
        PermissionKeys.CAN_MANAGE_USERS to true,
        PermissionKeys.CAN_VIEW_REPORTS to true
    ),
    val isActive: Boolean = true,
    val lastLogin: String = "01/08/2026 10:30 ص",
    val joinDate: String = "15/01/2025"
)
