package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** قراءة عداد مستقلة عن الفاتورة، حتى يمكن تسجيل القراءة أولاً وإصدار الفاتورة لاحقاً. */
@Entity(tableName = "meter_readings")
data class MeterReadingEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val adminId: String = "",
    val userId: String = "",
    val userName: String = "",
    val previousReading: Double = 0.0,
    val currentReading: Double = 0.0,
    val readingDate: String = "",
    val notes: String = "",
    val readerName: String = "",
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
