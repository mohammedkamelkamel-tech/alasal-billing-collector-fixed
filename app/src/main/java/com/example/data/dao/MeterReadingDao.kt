package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MeterReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getForUser(userId: String): List<MeterReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: MeterReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<MeterReadingEntity>)

    @Query("DELETE FROM meter_readings")
    suspend fun deleteAll()
}
