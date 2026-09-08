package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * النسخ الاحتياطي اليومي.
 * يحفظ النسخة في المجلد المحلي الذي حدده المستخدم، ويمكنه أيضاً نسخها تلقائياً
 * إلى مجلد Google Drive الذي حدده المستخدم من داخل التطبيق.
 */
class AutoBackupWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, true)) return Result.success()

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val fileName = "العسل - $date.zip"

        // إنشاء النسخة أولاً في ملف مؤقت داخل مساحة التطبيق، ثم نسخها إلى الوجهات.
        val tempFile = File(context.cacheDir, fileName)
        val created = BackupHelper.createBackupFile(context, tempFile)
        if (!created) return Result.retry()

        var localSaved = false
        var driveSaved = false

        val localUri = prefs.getString(KEY_LOCAL_TREE_URI, null)
        if (!localUri.isNullOrBlank()) {
            localSaved = saveToTree(context, Uri.parse(localUri), tempFile, fileName)
        }

        val driveUri = prefs.getString(KEY_DRIVE_TREE_URI, null)
        if (!driveUri.isNullOrBlank()) {
            driveSaved = saveToTree(context, Uri.parse(driveUri), tempFile, fileName)
        }

        // لا نحتفظ بالنسخ اليومية داخل مساحة التطبيق؛ هذا كان سبب عدم معرفة المستخدم بمكانها.
        tempFile.delete()

        // إذا اختار المستخدم وجهة واحدة على الأقل، نعتبر العملية ناجحة.
        // إذا لم يحدد أي وجهة، نعيد المحاولة حتى لا يضيع النسخ بصمت.
        return if (localSaved || driveSaved) Result.success() else Result.retry()
    }

    private fun saveToTree(context: Context, treeUri: Uri, source: File, fileName: String): Boolean {
        return try {
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            if (!root.canWrite()) return false

            // إذا وجدت نسخة بنفس التاريخ، استبدلها حتى لا تتكرر الملفات.
            root.findFile(fileName)?.delete()
            val target = root.createFile("application/zip", fileName) ?: return false

            context.contentResolver.openOutputStream(target.uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val UNIQUE_NAME = "daily_alasal_backup"
        const val PREFS = "app_prefs"
        const val KEY_ENABLED = "backup_auto_enabled"
        const val KEY_LOCAL_TREE_URI = "backup_local_tree_uri"
        const val KEY_DRIVE_TREE_URI = "backup_drive_tree_uri"

        fun schedule(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_ENABLED, true)) return

            val now = Calendar.getInstance()
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
