package com.example.repository

import com.example.database.BorrowerEntity
import com.example.database.LoanDao
import com.example.database.LoanEntity
import com.example.database.PaymentEntity
import com.example.database.MonthlyInterestRecord
import com.example.database.ActivityLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class LoanRepository(private val loanDao: LoanDao) {

    val allBorrowers: Flow<List<BorrowerEntity>> = loanDao.getAllBorrowers()
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    val allInterestRecords: Flow<List<MonthlyInterestRecord>> = loanDao.getAllInterestRecords()
    val allActivityLogs: Flow<List<ActivityLogEntity>> = loanDao.getAllActivityLogsFlow()

    fun getLoansForBorrower(borrowerId: Long): Flow<List<LoanEntity>> {
        return loanDao.getLoansForBorrower(borrowerId)
    }

    fun getPaymentsForLoan(loanId: Long): Flow<List<PaymentEntity>> {
        return loanDao.getPaymentsForLoan(loanId)
    }

    suspend fun getBorrowerByName(name: String): BorrowerEntity? {
        return loanDao.getBorrowerByName(name)
    }

    suspend fun getBorrowerById(id: Long): BorrowerEntity? {
        return loanDao.getBorrowerById(id)
    }

    suspend fun insertBorrower(borrower: BorrowerEntity): Long {
        return loanDao.insertBorrower(borrower)
    }

    suspend fun deleteBorrower(borrower: BorrowerEntity) {
        loanDao.deleteBorrower(borrower)
    }

    suspend fun deleteLoansForBorrower(borrowerId: Long) {
        loanDao.deleteLoansForBorrower(borrowerId)
    }

    suspend fun getLoanById(id: Long): LoanEntity? {
        return loanDao.getLoanById(id)
    }

    suspend fun insertLoan(loan: LoanEntity): Long {
        val loanId = loanDao.insertLoan(loan)
        recalculateAndSyncInterestRecords(loanId)
        loanDao.insertActivityLog(
            ActivityLogEntity(
                borrowerName = loan.borrowerName,
                type = "NEW_LOAN",
                amount = loan.amount,
                timestamp = System.currentTimeMillis()
            )
        )
        return loanId
    }

    suspend fun updateLoan(loan: LoanEntity) {
        loanDao.updateLoan(loan)
        recalculateAndSyncInterestRecords(loan.id)
    }

    // Support Undo Loan Deletion
    private var lastDeletedLoan: LoanEntity? = null
    private var lastDeletedPayments: List<PaymentEntity> = emptyList()
    private var lastDeletedInterestRecords: List<MonthlyInterestRecord> = emptyList()
    val lastDeletedLoanNameFlow = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    fun saveDeletedLoanState(loan: LoanEntity, payments: List<PaymentEntity>, records: List<MonthlyInterestRecord>) {
        lastDeletedLoan = loan
        lastDeletedPayments = payments
        lastDeletedInterestRecords = records
    }

    suspend fun undoLastDeletedLoan(): String? {
        val loan = lastDeletedLoan ?: return null
        val payments = lastDeletedPayments
        val records = lastDeletedInterestRecords
        
        restoreLoanWithPayments(loan, payments, records)
        
        lastDeletedLoan = null
        lastDeletedPayments = emptyList()
        lastDeletedInterestRecords = emptyList()
        
        return loan.borrowerName
    }

    suspend fun deleteLoan(loan: LoanEntity) {
        loanDao.deleteLoan(loan)
        loanDao.deleteInterestRecordsForLoan(loan.id)
    }

    suspend fun deleteLoanWithPayments(loanId: Long): Triple<LoanEntity, List<PaymentEntity>, List<MonthlyInterestRecord>>? {
        val loan = loanDao.getLoanById(loanId) ?: return null
        val payments = loanDao.getPaymentsForLoanSync(loanId)
        val interestRecords = loanDao.getInterestRecordsForLoanSync(loanId)
        
        loanDao.deleteLoan(loan)
        loanDao.deleteInterestRecordsForLoan(loanId)
        for (payment in payments) {
            loanDao.deletePayment(payment)
        }
        
        saveDeletedLoanState(loan, payments, interestRecords)
        lastDeletedLoanNameFlow.tryEmit(loan.borrowerName)
        
        return Triple(loan, payments, interestRecords)
    }

    suspend fun restoreLoanWithPayments(loan: LoanEntity, payments: List<PaymentEntity>, interestRecords: List<MonthlyInterestRecord>) {
        loanDao.insertLoan(loan)
        if (interestRecords.isNotEmpty()) {
            loanDao.insertInterestRecords(interestRecords)
        }
        for (payment in payments) {
            loanDao.insertPayment(payment)
        }
        recalculateAndSyncInterestRecords(loan.id)
    }

    suspend fun insertPayment(payment: PaymentEntity): Long {
        val id = loanDao.insertPayment(payment)
        recalculateAndSyncInterestRecords(payment.loanId)
        val loan = loanDao.getLoanById(payment.loanId)
        if (loan != null) {
            val logType = when (payment.paymentType) {
                "Interest" -> "INTEREST_RECEIVED"
                "Principal" -> "PRINCIPAL_RECEIVED"
                else -> "PAYMENT_RECEIVED"
            }
            loanDao.insertActivityLog(
                ActivityLogEntity(
                    borrowerName = loan.borrowerName,
                    type = logType,
                    amount = payment.totalPaid,
                    timestamp = payment.paymentDate
                )
            )
        }
        return id
    }

    suspend fun deletePayment(payment: PaymentEntity) {
        loanDao.deletePayment(payment)
        recalculateAndSyncInterestRecords(payment.loanId)
        val loan = loanDao.getLoanById(payment.loanId)
        if (loan != null) {
            loanDao.insertActivityLog(
                ActivityLogEntity(
                    borrowerName = loan.borrowerName,
                    type = "PAYMENT_REVERSED",
                    amount = payment.totalPaid,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun getPaymentsForLoanSync(loanId: Long): List<PaymentEntity> {
        return loanDao.getPaymentsForLoanSync(loanId)
    }

    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>> {
        return loanDao.getAllPaymentsFlow()
    }

    suspend fun getAllPaymentsSync(): List<PaymentEntity> {
        return loanDao.getAllPaymentsSync()
    }

    suspend fun getPaymentById(id: Long): PaymentEntity? {
        return loanDao.getPaymentById(id)
    }

    suspend fun deletePaymentById(id: Long) {
        val payment = loanDao.getPaymentById(id)
        if (payment != null) {
            loanDao.deletePaymentById(id)
            recalculateAndSyncInterestRecords(payment.loanId)
            val loan = loanDao.getLoanById(payment.loanId)
            if (loan != null) {
                loanDao.insertActivityLog(
                    ActivityLogEntity(
                        borrowerName = loan.borrowerName,
                        type = "PAYMENT_REVERSED",
                        amount = payment.totalPaid,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun seedActivityLogsIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val logs = loanDao.getAllActivityLogsFlow().first()
            if (logs.isEmpty()) {
                val loans = loanDao.getAllLoansSync()
                val payments = loanDao.getAllPaymentsSync()
                
                loans.forEach { loan ->
                    loanDao.insertActivityLog(
                        ActivityLogEntity(
                            borrowerName = loan.borrowerName,
                            type = "NEW_LOAN",
                            amount = loan.amount,
                            timestamp = loan.loanDate
                        )
                    )
                }
                
                payments.forEach { p ->
                    val loan = loans.find { it.id == p.loanId }
                    if (loan != null) {
                        val logType = when (p.paymentType) {
                            "Interest" -> "INTEREST_RECEIVED"
                            "Principal" -> "PRINCIPAL_RECEIVED"
                            else -> "PAYMENT_RECEIVED"
                        }
                        loanDao.insertActivityLog(
                            ActivityLogEntity(
                                borrowerName = loan.borrowerName,
                                type = logType,
                                amount = p.totalPaid,
                                timestamp = p.paymentDate
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getInterestRecordsForLoan(loanId: Long): Flow<List<MonthlyInterestRecord>> {
        return loanDao.getInterestRecordsForLoan(loanId)
    }

    suspend fun getInterestRecordsForLoanSync(loanId: Long): List<MonthlyInterestRecord> {
        return loanDao.getInterestRecordsForLoanSync(loanId)
    }

    suspend fun getAllInterestRecordsSync(): List<MonthlyInterestRecord> {
        return loanDao.getAllInterestRecordsSync()
    }

    suspend fun updateInterestRecord(record: MonthlyInterestRecord) {
        loanDao.updateInterestRecord(record)
    }

    suspend fun autoGenerateMissingRecords() = withContext(Dispatchers.IO) {
        val activeLoans = loanDao.getAllLoansSync().filter { it.status == "ACTIVE" }
        for (loan in activeLoans) {
            recalculateAndSyncInterestRecords(loan.id)
        }
    }

    suspend fun recalculateAndSyncInterestRecords(loanId: Long) = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanById(loanId) ?: return@withContext
        val payments = loanDao.getPaymentsForLoanSync(loanId)

        // Calculate total principal paid and update loan status
        val totalPrincipalPaid = payments.sumOf { it.principalPaid }
        val isFullyPaid = totalPrincipalPaid >= loan.amount
        val expectedStatus = if (isFullyPaid) "PAID" else "ACTIVE"
        if (loan.status != expectedStatus) {
            loanDao.updateLoan(loan.copy(status = expectedStatus))
        }

        // 1. Calculate how many months need to exist up to the first future month
        val todayMs = System.currentTimeMillis()
        val endPointMs = if (isFullyPaid) (payments.map { it.paymentDate }.maxOrNull() ?: todayMs) else todayMs
        var monthNumber = 1
        var hasFutureRecord = false
        val totalMonthsList = mutableListOf<Int>()

        while (!hasFutureRecord) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = loan.loanDate
            cal.add(Calendar.MONTH, monthNumber)
            val dueDate = cal.timeInMillis

            totalMonthsList.add(monthNumber)

            if (dueDate > endPointMs) {
                hasFutureRecord = true
            }
            monthNumber++
        }

        // 2. Generate blank records for these months with proper interestAmount based on principal reduction
        val blankRecords = totalMonthsList.map { m ->
            val calStart = Calendar.getInstance()
            calStart.timeInMillis = loan.loanDate
            calStart.add(Calendar.MONTH, m - 1)
            val monthStart = calStart.timeInMillis

            val calDue = Calendar.getInstance()
            calDue.timeInMillis = loan.loanDate
            calDue.add(Calendar.MONTH, m)
            val dueDate = calDue.timeInMillis

            // Sum of all principalPaid before or on monthStart
            val principalPaidBefore = payments
                .filter { it.paymentDate <= monthStart }
                .sumOf { it.principalPaid }

            val currentPrincipal = (loan.amount - principalPaidBefore).coerceAtLeast(0.0)
            val expectedInterest = currentPrincipal * (loan.interestRate / 100.0)

            MonthlyInterestRecord(
                loanId = loanId,
                monthNumber = m,
                dueDate = dueDate,
                interestAmount = expectedInterest,
                interestPaid = 0.0,
                status = "UPCOMING",
                paidDate = null,
                createdAt = System.currentTimeMillis()
            )
        }

        val mutableRecords = blankRecords.toMutableList()

        // 3. Chronologically apply interest payments to these records
        val interestPaymentsSorted = payments
            .filter { it.paymentType == "Interest" || it.paymentType == "Both" }
            .sortedBy { it.paymentDate }

        for (payment in interestPaymentsSorted) {
            var remainingPayment = payment.interestPaid
            for (i in mutableRecords.indices) {
                if (remainingPayment <= 0.0) break
                val record = mutableRecords[i]
                val needed = record.interestAmount - record.interestPaid
                if (needed > 0.0) {
                    if (remainingPayment >= needed) {
                        mutableRecords[i] = record.copy(
                            interestPaid = record.interestAmount,
                            status = "PAID",
                            paidDate = payment.paymentDate
                        )
                        remainingPayment -= needed
                    } else {
                        mutableRecords[i] = record.copy(
                            interestPaid = record.interestPaid + remainingPayment,
                            status = "PARTIAL",
                            paidDate = null
                        )
                        remainingPayment = 0.0
                    }
                }
            }
        }

        // 4. For any records that are NOT paid/partial, set their status dynamically based on current date
        for (i in mutableRecords.indices) {
            val record = mutableRecords[i]
            if (record.status != "PAID" && record.status != "PARTIAL") {
                val nextCal = Calendar.getInstance()
                nextCal.timeInMillis = loan.loanDate
                nextCal.add(Calendar.MONTH, record.monthNumber + 1)
                val nextDueDate = nextCal.timeInMillis

                val computedStatus = when {
                    todayMs < record.dueDate -> "UPCOMING"
                    todayMs >= record.dueDate && todayMs < nextDueDate -> "PENDING"
                    else -> "OVERDUE"
                }
                mutableRecords[i] = record.copy(status = computedStatus)
            }
        }

        // 5. Delete and replace
        loanDao.deleteInterestRecordsForLoan(loanId)
        loanDao.insertInterestRecords(mutableRecords)
    }

    suspend fun createParsedLoan(name: String, amount: Double, interestRate: Double) {
        var borrower = getBorrowerByName(name)
        if (borrower == null) {
            val borrowerId = insertBorrower(BorrowerEntity(name = name, phone = "", tag = "Friend"))
            borrower = getBorrowerById(borrowerId)
        }
        if (borrower != null) {
            val loan = LoanEntity(
                borrowerId = borrower.id,
                borrowerName = borrower.name,
                amount = amount,
                interestRate = interestRate,
                loanDate = System.currentTimeMillis(),
                status = "ACTIVE"
            )
            insertLoan(loan)
        }
    }
}
