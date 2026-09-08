package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** سجل مستقل لكل عملية تحصيل؛ لا يتم استبدال عملية سابقة عند تحصيل دفعة جديدة. */
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val adminId: String = "",
    val billId: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val userName: String = "",
    val amount: Double = 0.0,
    val method: String = "نقدي",
    val collectorId: String = "",
    val collectorName: String = "",
    val paymentDate: String = "",
    val paymentAt: Long = System.currentTimeMillis(),
    val beforeRemaining: Double = 0.0,
    val afterRemaining: Double = 0.0,
    val receiptNumber: Long = 0L
)
