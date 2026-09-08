package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val adminId: String = "", // 👈 يربط المشترك بالأدمن الرئيسي للمؤسسة (نفس فكرة adminId في BillEntity)
    val userIdCode: String = "", // e.g. USER-2026-001
    val name: String = "",
    val email: String = "",
    val role: String = "", // UserRole enum name
    val phone: String = "",
    val address: String = "",
    val isActive: Boolean = true,
    val lastLogin: String = "01/08/2026 10:30 ص",
    val joinDate: String = "15/01/2025",
    val meterNumber: String = "",
    /** سعر الكيلوواط الثابت لهذا المشترك، ويُستخدم تلقائياً عند إصدار الفاتورة. */
    val unitPrice: Double = 170.0
)
