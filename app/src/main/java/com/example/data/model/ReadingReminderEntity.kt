package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_reminders")
data class ReadingReminderEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val adminId: String = "",
    val userId: String = "",
    val userName: String = "",
    val reminderAt: Long = 0L,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)
