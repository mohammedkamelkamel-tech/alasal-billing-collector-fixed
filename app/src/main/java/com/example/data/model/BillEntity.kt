package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان الفاتورة.
 *
 * التعديلات:
 * - حذف الحقل taxAmount نهائياً (إلغاء ضريبة 10%).
 * - إضافة issueDate: تاريخ إصدار الفاتورة (يُملأ تلقائياً بتاريخ اليوم).
 * - إضافة previousDebt: المتأخرات المُرحّلة من الفواتير السابقة للمشترك.
 * - إضافة paidAmount / remainingAmount: لدعم الدفع الجزئي.
 *
 * قاعدة الحساب الجديدة:
 *   consumptionKwh = currentReading - prevReading
 *   subtotalAmount = consumptionKwh * unitPrice        (قيمة الاستهلاك)
 *   totalAmount    = subtotalAmount + previousDebt     (الإجمالي النهائي)
 *   remainingAmount = totalAmount - paidAmount
 */
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val adminId: String = "",
    val invoiceNumber: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userAddress: String = "",
    val prevReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val consumptionKwh: Double = 0.0,
    val unitPrice: Double = 170.0,
    val subtotalAmount: Double = 0.0,
    val previousDebt: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val issueDate: String = "",
    val dueDate: String = "",
    val status: String = BillStatus.UNPAID.name,
    val readingDate: String = "",
    val notes: String = "",
    val paymentDate: String = "",
    val paymentMethod: String = "",
    val paymentCollector: String = "",
    val paymentAt: Long = 0L,
    val readingImageUri: String? = null,
    /** وقت إنشاء الفاتورة الفعلي، لضمان ترتيب الأحدث أولاً حتى لو كانت أرقام الفواتير عشوائية. */
    val createdAt: Long = System.currentTimeMillis()
)
