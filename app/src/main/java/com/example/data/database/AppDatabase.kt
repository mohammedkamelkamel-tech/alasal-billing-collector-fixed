package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BillDao
import com.example.data.dao.PermissionDao
import com.example.data.dao.MeterReadingDao
import com.example.data.dao.ReadingReminderDao
import com.example.data.dao.UserDao
import com.example.data.model.BillEntity
import com.example.data.model.MeterReadingEntity
import com.example.data.model.ReadingReminderEntity
import com.example.data.model.RolePermissionEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.UserEntity

@Database(
    entities = [UserEntity::class, BillEntity::class, RolePermissionEntity::class, MeterReadingEntity::class, ReadingReminderEntity::class, PaymentEntity::class],
    version = 12, // إضافة اسم المحصل لكل عملية تحصيل
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun billDao(): BillDao
    abstract fun permissionDao(): PermissionDao
    abstract fun meterReadingDao(): MeterReadingDao
    abstract fun readingReminderDao(): ReadingReminderDao
    abstract fun paymentDao(): com.example.data.dao.PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "electricity_billing_db"
                )
                    // 👈 ترحيل حقيقي يحافظ على بيانات الفواتير القديمة بدل حذفها
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    // يبقى كشبكة أمان فقط للإصدارات القديمة جداً غير المُغطّاة بترحيل
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
