package com.example.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * ترحيل قاعدة البيانات من الإصدار 4 إلى 5.
 *
 * سبب الترحيل:
 * - حذف العمود taxAmount نهائياً (إلغاء الضريبة).
 * - إضافة الأعمدة: previousDebt, paidAmount, remainingAmount, issueDate.
 *
 * بما أن SQLite في إصدارات أندرويد القديمة لا يدعم DROP COLUMN، يتم إنشاء جدول
 * جديد ونقل البيانات إليه ثم استبدال الجدول القديم — مع الحفاظ على كل البيانات.
 *
 * القيم المحسوبة أثناء النقل:
 * - totalAmount الجديد = subtotalAmount (بدون ضريبة).
 * - paidAmount = totalAmount الجديد للفواتير المدفوعة، و0 لغيرها.
 * - remainingAmount = المتبقي وفق ذلك.
 * - issueDate = readingDate إن وُجد.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bills_new (
                id TEXT NOT NULL PRIMARY KEY,
                adminId TEXT NOT NULL,
                invoiceNumber TEXT NOT NULL,
                userId TEXT NOT NULL,
                userName TEXT NOT NULL,
                userPhone TEXT NOT NULL,
                userAddress TEXT NOT NULL,
                prevReading REAL NOT NULL,
                currentReading REAL NOT NULL,
                consumptionKwh REAL NOT NULL,
                unitPrice REAL NOT NULL,
                subtotalAmount REAL NOT NULL,
                previousDebt REAL NOT NULL DEFAULT 0,
                totalAmount REAL NOT NULL,
                paidAmount REAL NOT NULL DEFAULT 0,
                remainingAmount REAL NOT NULL DEFAULT 0,
                issueDate TEXT NOT NULL DEFAULT '',
                dueDate TEXT NOT NULL,
                status TEXT NOT NULL,
                readingDate TEXT NOT NULL,
                notes TEXT NOT NULL,
                paymentDate TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                readingImageUri TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO bills_new (
                id, adminId, invoiceNumber, userId, userName, userPhone, userAddress,
                prevReading, currentReading, consumptionKwh, unitPrice, subtotalAmount,
                previousDebt, totalAmount, paidAmount, remainingAmount, issueDate,
                dueDate, status, readingDate, notes, paymentDate, paymentMethod, readingImageUri
            )
            SELECT
                id, adminId, invoiceNumber, userId, userName, userPhone, userAddress,
                prevReading, currentReading, consumptionKwh, unitPrice, subtotalAmount,
                0,
                subtotalAmount,
                CASE WHEN status = 'PAID' THEN subtotalAmount ELSE 0 END,
                CASE WHEN status = 'PAID' THEN 0 ELSE subtotalAmount END,
                readingDate,
                dueDate, status, readingDate, notes, paymentDate, paymentMethod, readingImageUri
            FROM bills
            """.trimIndent()
        )

        db.execSQL("DROP TABLE bills")
        db.execSQL("ALTER TABLE bills_new RENAME TO bills")
    }
}


/** إضافة اسم المستخدم الذي قام بآخر عملية تحصيل على الفاتورة. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN paymentCollector TEXT NOT NULL DEFAULT ''")
    }
}


/** إضافة جدول مستقل لقراءات العدادات. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS meter_readings (
                id TEXT NOT NULL PRIMARY KEY,
                adminId TEXT NOT NULL,
                userId TEXT NOT NULL,
                userName TEXT NOT NULL,
                previousReading REAL NOT NULL,
                currentReading REAL NOT NULL,
                readingDate TEXT NOT NULL,
                notes TEXT NOT NULL,
                readerName TEXT NOT NULL,
                imageUri TEXT,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}


/** إضافة وقت إنشاء الفاتورة لترتيب الأحدث أولاً. */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE bills SET createdAt = CASE WHEN issueDate != '' THEN COALESCE(strftime('%s', substr(issueDate,7,4)||'-'||substr(issueDate,4,2)||'-'||substr(issueDate,1,2))*1000, 0) ELSE 0 END")
    }
}


/** إضافة جدول تذكيرات قراءة العدادات المجدولة. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reading_reminders (
                id TEXT NOT NULL PRIMARY KEY,
                adminId TEXT NOT NULL,
                userId TEXT NOT NULL,
                userName TEXT NOT NULL,
                reminderAt INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}


/** إضافة وقت آخر عملية تحصيل لترتيب المقبوضات بدقة. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN paymentAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE bills SET paymentAt = CASE WHEN paymentDate != '' THEN COALESCE(strftime('%s', substr(paymentDate,7,4)||'-'||substr(paymentDate,4,2)||'-'||substr(paymentDate,1,2))*1000, 0) ELSE 0 END")
    }
}


/** إضافة سعر الكيلوواط الثابت لكل مشترك. الفواتير القديمة تحتفظ بسعرها داخل BillEntity. */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN unitPrice REAL NOT NULL DEFAULT 170.0")
    }
}


/** سجل مستقل لكل عملية تحصيل لمنع اختلاط/استبدال إيصالات المحصلين. */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS payments (
                id TEXT NOT NULL PRIMARY KEY,
                adminId TEXT NOT NULL,
                billId TEXT NOT NULL,
                userId TEXT NOT NULL,
                invoiceNumber TEXT NOT NULL,
                userName TEXT NOT NULL,
                amount REAL NOT NULL,
                method TEXT NOT NULL,
                collectorId TEXT NOT NULL,
                collectorName TEXT NOT NULL,
                paymentDate TEXT NOT NULL,
                paymentAt INTEGER NOT NULL,
                beforeRemaining REAL NOT NULL,
                afterRemaining REAL NOT NULL,
                receiptNumber INTEGER NOT NULL
            )
        """.trimIndent())

        // تحويل آخر عملية دفع معروفة لكل فاتورة قديمة إلى سجل تاريخي.
        db.execSQL("""
            INSERT OR IGNORE INTO payments (
                id, adminId, billId, userId, invoiceNumber, userName, amount, method,
                collectorId, collectorName, paymentDate, paymentAt, beforeRemaining,
                afterRemaining, receiptNumber
            )
            SELECT
                'legacy-' || id, adminId, id, userId, invoiceNumber, userName, paidAmount,
                CASE WHEN paymentMethod = '' THEN 'نقدي' ELSE paymentMethod END,
                '', paymentCollector, paymentDate,
                paymentAt, remainingAmount + paidAmount, remainingAmount, 0
            FROM bills
            WHERE paidAmount > 0
        """.trimIndent())
    }
}
