package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BillEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC, id DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: String): BillEntity?

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY createdAt DESC, id DESC")
    fun getBillsByUserId(userId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE adminId = :adminId ORDER BY createdAt DESC, id DESC")
    fun getBillsByAdminId(adminId: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE status = :status ORDER BY createdAt DESC, id DESC")
    fun getBillsByStatus(status: String): Flow<List<BillEntity>>

    /** جميع فواتير مشترك معيّن (استعلام لحظي غير متدفق) لحساب المتأخرات وآخر قراءة. */
    @Query("SELECT * FROM bills WHERE userId = :userId")
    suspend fun getBillsForUserOnce(userId: String): List<BillEntity>

    /** الفواتير التي ما زال عليها مبلغ مستحق (لترحيل المتأخرات إلى الفاتورة الجديدة). */
    @Query("SELECT * FROM bills WHERE userId = :userId AND status NOT IN ('PAID','CARRIED') AND remainingAmount > 0")
    suspend fun getOutstandingBillsForUser(userId: String): List<BillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillEntity>)

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    /**
     * تحديث حالة السداد مع دعم الدفع الجزئي:
     * تُخزَّن قيمة المدفوع والمتبقي إلى جانب الحالة، بدلاً من اعتبار الفاتورة
     * مدفوعة بالكامل بمجرد الضغط على زر الدفع كما كان سابقاً.
     */
    @Query(
        "UPDATE bills SET status = :status, paidAmount = :paidAmount, remainingAmount = :remainingAmount, " +
            "paymentDate = :paymentDate, paymentMethod = :paymentMethod, paymentCollector = :paymentCollector, paymentAt = :paymentAt WHERE id = :id"
    )
    suspend fun updateBillPayment(
        id: String,
        status: String,
        paidAmount: Double,
        remainingAmount: Double,
        paymentDate: String,
        paymentMethod: String,
        paymentCollector: String,
        paymentAt: Long
    )

    /** تعليم فاتورة بأنها مُرحّلة بعد إدراج متبقّيها كمتأخرات في فاتورة أحدث. */
    @Query("UPDATE bills SET status = 'CARRIED', remainingAmount = 0 WHERE id = :id")
    suspend fun markBillCarriedForward(id: String)

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}
