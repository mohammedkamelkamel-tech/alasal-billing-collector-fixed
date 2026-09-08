package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReadingReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingReminderDao {
    @Query("SELECT * FROM reading_reminders WHERE completed = 0 ORDER BY reminderAt ASC")
    fun getActive(): Flow<List<ReadingReminderEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReadingReminderEntity)
    @Query("UPDATE reading_reminders SET completed = 1 WHERE id = :id")
    suspend fun complete(id: String)
    @Query("DELETE FROM reading_reminders WHERE id = :id")
    suspend fun delete(id: String)
}
