package com.example.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class ReadingReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("user_name") ?: "المشترك"
        val note = inputData.getString("note").orEmpty()
        NotificationHelper.sendMeterReadingReminder(applicationContext, name, note, id.hashCode())
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, reminderId: String, userName: String, atMillis: Long, note: String) {
            val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val input = Data.Builder()
                .putString("reminder_id", reminderId)
                .putString("user_name", userName)
                .putString("note", note)
                .build()
            val request = OneTimeWorkRequestBuilder<ReadingReminderWorker>()
                .setInputData(input)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reading_reminder_$reminderId",
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
