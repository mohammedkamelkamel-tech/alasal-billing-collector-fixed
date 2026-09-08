package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "role_permissions")
data class RolePermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // ADMIN, COLLECTOR, READER, MONITOR
    val category: String, // فواتير, مستخدمون, إعدادات, تقارير, نظام
    val permissionKey: String,
    val permissionName: String,
    val isGranted: Boolean
)
