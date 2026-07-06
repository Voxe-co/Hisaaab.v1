package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    // Borrowers
    @Query("SELECT * FROM borrowers ORDER BY name ASC")
    fun getAllBorrowers(): Flow<List<BorrowerEntity>>

    @Query("SELECT * FROM borrowers WHERE TRIM(LOWER(name)) = TRIM(LOWER(:name)) LIMIT 1")
    suspend fun getBorrowerByName(name: String): BorrowerEntity?

    @Query("SELECT * FROM borrowers WHERE id = :id LIMIT 1")
    suspend fun getBorrowerById(id: Long): BorrowerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrower(borrower: BorrowerEntity): Long

    @Delete
    suspend fun deleteBorrower(borrower: BorrowerEntity)

    @Query("DELETE FROM loans WHERE borrowerId = :borrowerId")
    suspend fun deleteLoansForBorrower(borrowerId: Long)

    // Loans
    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Long): LoanEntity?

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansSync(): List<LoanEntity>

    @Query("SELECT * FROM loans ORDER BY loanDate DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE borrowerId = :borrowerId ORDER BY loanDate DESC")
    fun getLoansForBorrower(borrowerId: Long): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Delete
    suspend fun deleteLoan(loan: LoanEntity)

    // Payments
    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE loanId = :loanId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForLoanSync(loanId: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    suspend fun getAllPaymentsSync(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): PaymentEntity?

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    // Monthly Interest Records
    @Query("SELECT * FROM monthly_interest_records WHERE loanId = :loanId ORDER BY monthNumber ASC")
    fun getInterestRecordsForLoan(loanId: Long): Flow<List<MonthlyInterestRecord>>

    @Query("SELECT * FROM monthly_interest_records WHERE loanId = :loanId ORDER BY monthNumber ASC")
    suspend fun getInterestRecordsForLoanSync(loanId: Long): List<MonthlyInterestRecord>

    @Query("SELECT * FROM monthly_interest_records ORDER BY dueDate ASC")
    fun getAllInterestRecords(): Flow<List<MonthlyInterestRecord>>

    @Query("SELECT * FROM monthly_interest_records ORDER BY dueDate ASC")
    suspend fun getAllInterestRecordsSync(): List<MonthlyInterestRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestRecord(record: MonthlyInterestRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterestRecords(records: List<MonthlyInterestRecord>)

    @Update
    suspend fun updateInterestRecord(record: MonthlyInterestRecord)

    @Query("DELETE FROM monthly_interest_records WHERE loanId = :loanId")
    suspend fun deleteInterestRecordsForLoan(loanId: Long)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllActivityLogsFlow(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivityLogs()
}
