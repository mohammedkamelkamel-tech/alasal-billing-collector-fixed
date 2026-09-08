package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentAt DESC, id DESC")
    fun getAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE billId = :billId ORDER BY paymentAt ASC, id ASC")
    suspend fun getForBill(billId: String): List<PaymentEntity>

    @Query("SELECT COALESCE(MAX(receiptNumber), 0) + 1 FROM payments")
    suspend fun nextReceiptNumber(): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}
