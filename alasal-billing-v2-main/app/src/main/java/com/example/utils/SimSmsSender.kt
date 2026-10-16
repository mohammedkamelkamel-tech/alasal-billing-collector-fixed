package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Sends an SMS using the phone's active SIM.
 * Caller must request SEND_SMS at runtime and only send after the operator has explicitly enabled this feature.
 */
object SimSmsSender {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun send(context: Context, phoneNumber: String, message: String) {
        require(phoneNumber.isNotBlank()) { "رقم الهاتف مطلوب" }
        require(message.isNotBlank()) { "نص الرسالة مطلوب" }
        check(hasPermission(context)) { "يجب منح إذن إرسال الرسائل النصية أولاً" }
        val manager = context.getSystemService(SmsManager::class.java)
            ?: throw IllegalStateException("خدمة الرسائل غير متاحة")
        val parts = manager.divideMessage(message)
        if (parts.size > 1) manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        else manager.sendTextMessage(phoneNumber, null, message, null, null)
    }

    fun invoiceMessage(invoiceNumber: String, customerName: String, amount: Double, currency: String, dueDate: String): String =
        "محطة العسل - فاتورة كهرباء\nالمشترك: $customerName\nرقم الفاتورة: $invoiceNumber\nالمبلغ: ${CurrencyFormatter.format(amount, currency)}\nتاريخ الاستحقاق: $dueDate"
}
