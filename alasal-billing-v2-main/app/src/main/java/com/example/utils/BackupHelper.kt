package com.example.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {
    private const val DB_NAME = "electricity_billing_db"

    suspend fun backupDatabase(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.parent, "$DB_NAME-wal")
            val shmFile = File(dbFile.parent, "$DB_NAME-shm")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    val filesToBackup = listOf(dbFile, walFile, shmFile)
                    for (file in filesToBackup) {
                        if (file.exists()) {
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createBackupFile(context: Context, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            targetFile.parentFile?.mkdirs()
            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.parent, "$DB_NAME-wal")
            val shmFile = File(dbFile.parent, "$DB_NAME-shm")
            FileOutputStream(targetFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    listOf(dbFile, walFile, shmFile).forEach { file ->
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry(file.name))
                            FileInputStream(file).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createBackupFileForSharing(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.parent, "$DB_NAME-wal")
            val shmFile = File(dbFile.parent, "$DB_NAME-shm")

            val backupFile = File(context.cacheDir, "electricity_billing_backup_${System.currentTimeMillis()}.zip")
            FileOutputStream(backupFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val filesToBackup = listOf(dbFile, walFile, shmFile)
                    for (file in filesToBackup) {
                        if (file.exists()) {
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun restoreDatabase(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            
            // Temporary directory for extracting
            val tempDir = File(context.cacheDir, "db_restore_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // If we successfully extracted, replace the actual database files
            val extractedDb = File(tempDir, DB_NAME)
            if (extractedDb.exists()) {
                // Delete existing db files to prevent WAL conflicts
                dbFile.delete()
                File(dbFile.parent, "$DB_NAME-wal").delete()
                File(dbFile.parent, "$DB_NAME-shm").delete()

                // Move extracted files
                val filesToMove = listOf(DB_NAME, "$DB_NAME-wal", "$DB_NAME-shm")
                for (fileName in filesToMove) {
                    val extractedFile = File(tempDir, fileName)
                    if (extractedFile.exists()) {
                        extractedFile.copyTo(File(dbFile.parent, fileName), overwrite = true)
                    }
                }
                tempDir.deleteRecursively()
                return@withContext true
            }
            tempDir.deleteRecursively()
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
