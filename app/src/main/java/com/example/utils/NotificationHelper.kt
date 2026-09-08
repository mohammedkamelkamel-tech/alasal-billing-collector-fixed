package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.BillEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppNotificationItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType
)

enum class NotificationType {
    NEW_BILL,
    DUE_DATE_APPROACHING,
    PAYMENT_CONFIRMATION,
    METER_READING_REMINDER,
    SYSTEM_ALERT
}

object NotificationHelper {

    const val CHANNEL_ID = "electricity_billing_notifications"
    const val CHANNEL_NAME = "إشعارات فواتير الكهرباء"
    const val CHANNEL_DESC = "تنبيهات عند إصدار فواتير جديدة واقتراب مواعيد السداد"

    private val notificationList = mutableListOf<AppNotificationItem>()

    fun getNotifications(): List<AppNotificationItem> = notificationList.toList()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }


    }

    fun sendNewBillNotification(
        context: Context,
        subscriberName: String,
        amount: Double,
        billId: String
    ) {
        createNotificationChannel(context)

        val title = "⚡ إصدار فاتورة كهرباء جديدة"
        val message = "تم إصدار فاتورة جديدة للمشترك ($subscriberName) بمبلغ ${CurrencyFormatter.riyalFull(amount)}."

        addNotificationRecord(title, message, NotificationType.NEW_BILL)

        showSystemNotification(context, title, message, billId.hashCode())
    }

    fun sendDueDateApproachingNotification(
        context: Context,
        subscriberName: String,
        amount: Double,
        dueDate: String,
        billId: String
    ) {
        createNotificationChannel(context)

        val title = "⏰ اقتراب موعد سداد الفاتورة"
        val message = "تذكير: الفاتورة الخاصة بـ ($subscriberName) بمبلغ ${CurrencyFormatter.riyal(amount)} تستحق السداد بتاريخ $dueDate."

        addNotificationRecord(title, message, NotificationType.DUE_DATE_APPROACHING)

        showSystemNotification(context, title, message, (billId + "_due").hashCode())
    }

    fun sendMeterReadingReminder(context: Context, subscriberName: String, note: String, notificationId: Int) {
        createNotificationChannel(context)
        val title = "📋 تذكير بأخذ قراءة العداد"
        val message = if (note.isBlank()) "حان موعد أخذ قراءة عداد ($subscriberName)." else "حان موعد أخذ قراءة عداد ($subscriberName): $note"
        addNotificationRecord(title, message, NotificationType.METER_READING_REMINDER)
        showSystemNotification(context, title, message, notificationId)
    }

    fun checkAndNotifyApproachingDueDates(context: Context, bills: List<BillEntity>) {
        val unpaidBills = bills.filter { it.status != "PAID" }
        var count = 0
        unpaidBills.forEach { bill ->
            // Trigger due date approaching notification for unpaid bills
            sendDueDateApproachingNotification(
                context,
                subscriberName = bill.userName,
                amount = bill.totalAmount,
                dueDate = bill.dueDate,
                billId = bill.id
            )
            count++
        }
    }

    private fun addNotificationRecord(title: String, message: String, type: NotificationType) {
        notificationList.add(0, AppNotificationItem(title = title, message = message, type = type))
    }

    private fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(notificationId, builder.build())
                }
            } else {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Permission missing for notification: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to show notification: ${e.message}")
        }
    }
}
