package com.example.data.database

import com.example.data.model.BillEntity
import com.example.data.model.RolePermissionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.first

object DatabaseInitializer {

    suspend fun seedIfNeeded(database: AppDatabase) {
        val userDao = database.userDao()
        val billDao = database.billDao()
        val permissionDao = database.permissionDao()

        val existingPermissions = permissionDao.getPermissionsByRole("ADMIN").first()
        if (existingPermissions.isEmpty()) {
            val permissions = mutableListOf<RolePermissionEntity>()
            val roles = listOf("ADMIN", "COLLECTOR", "READER", "MONITOR")
            val categoriesWithPermissions = listOf(
                "فواتير" to listOf(
                    "bill_read" to "قراءة الفواتير",
                    "bill_add" to "إضافة فاتورة",
                    "bill_edit" to "تعديل فاتورة",
                    "bill_delete" to "حذف فاتورة",
                    "bill_print" to "طباعة فاتورة"
                ),
                "مستخدمون" to listOf(
                    "user_read" to "قراءة المستخدمين",
                    "user_add" to "إضافة مستخدم",
                    "user_edit" to "تعديل مستخدم",
                    "user_delete" to "حذف مستخدم",
                    "user_disable" to "تعطيل مستخدم"
                ),
                "إعدادات" to listOf(
                    "role_manage" to "إدارة الأدوار",
                    "perm_manage" to "إدارة الصلاحيات",
                    "settings_manage" to "إدارة الإعدادات"
                ),
                "تقارير" to listOf(
                    "report_view" to "عرض التقارير",
                    "report_export" to "تصدير التقارير",
                    "report_print" to "طباعة التقارير"
                ),
                "نظام" to listOf(
                    "system_backup" to "النسخ الاحتياطي",
                    "system_restore" to "استعادة البيانات",
                    "system_impersonate" to "تسجيل الدخول كشخص آخر"
                )
            )

            roles.forEach { role ->
                categoriesWithPermissions.forEach { (category, permList) ->
                    permList.forEach { (key, name) ->
                        val isGranted = when (role) {
                            "ADMIN" -> true
                            "COLLECTOR" -> key in listOf("bill_read", "bill_add", "bill_edit", "bill_print", "user_read", "report_view")
                            "READER" -> key in listOf("bill_read", "report_view")
                            "MONITOR" -> key in listOf("bill_read", "user_read", "report_view", "report_export", "report_print")
                            else -> false
                        }
                        permissions.add(
                            RolePermissionEntity(
                                role = role,
                                category = category,
                                permissionKey = key,
                                permissionName = name,
                                isGranted = isGranted
                            )
                        )
                    }
                }
            }
            permissionDao.insertPermissions(permissions)
        }
    }
}
