package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HisaabApplication
import com.example.database.BorrowerEntity
import com.example.database.LoanEntity
import com.example.database.MonthlyInterestRecord
import com.example.util.BorrowerMock
import com.example.util.DummyData
import com.example.util.LoanMock
import com.example.util.PaymentMock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class TimelineItem {
    data class LoanCreated(val loan: LoanMock) : TimelineItem()
    data class MonthlyInterest(
        val id: Long,
        val loanId: Long,
        val monthNumber: Int,
        val interestAmount: Double,
        val interestPaid: Double,
        val dueDate: Long,
        val status: String, // PAID, PENDING, OVERDUE, UPCOMING, PARTIAL
        val paidDate: Long?
    ) : TimelineItem()
}

class BorrowerDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as HisaabApplication).repository

    private val _borrower = MutableStateFlow<BorrowerMock?>(null)
    val borrower: StateFlow<BorrowerMock?> = _borrower.asStateFlow()

    private val _loans = MutableStateFlow<List<LoanMock>>(emptyList())
    val loans: StateFlow<List<LoanMock>> = _loans.asStateFlow()

    private val _rawLoans = MutableStateFlow<List<LoanEntity>>(emptyList())
    val rawLoans: StateFlow<List<LoanEntity>> = _rawLoans.asStateFlow()

    private val _timeline = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timeline: StateFlow<List<TimelineItem>> = _timeline.asStateFlow()

    private val _payments = MutableStateFlow<List<com.example.database.PaymentEntity>>(emptyList())
    val payments: StateFlow<List<com.example.database.PaymentEntity>> = _payments.asStateFlow()

    private var selectJob: kotlinx.coroutines.Job? = null

    fun selectBorrower(borrowerId: Long) {
        selectJob?.cancel()
        selectJob = viewModelScope.launch {
            val borrowerEntity = repository.getBorrowerById(borrowerId) ?: return@launch

            combine(
                repository.getLoansForBorrower(borrowerId),
                repository.allInterestRecords,
                repository.getAllPaymentsFlow()
            ) { loanEntities, interestRecords, allPayments ->
                Triple(loanEntities, interestRecords, allPayments)
            }.collect { (loanEntities, interestRecords, allPayments) ->
                _rawLoans.value = loanEntities
                
                val todayMs = System.currentTimeMillis()
                val loanIds = loanEntities.map { it.id }.toSet()
                val paymentsForBorrower = allPayments.filter { it.loanId in loanIds }
                _payments.value = paymentsForBorrower
                
                val mappedLoans = loanEntities.map { entity ->
                    val recordsForLoan = interestRecords.filter { it.loanId == entity.id }
                    val paymentsForLoan = allPayments.filter { it.loanId == entity.id }

                    val totalPrincipalPaid = paymentsForLoan.sumOf { it.principalPaid }
                    val totalInterestPaid = paymentsForLoan.sumOf { it.interestPaid }
                    val remainingPrincipal = (entity.amount - totalPrincipalPaid).coerceAtLeast(0.0)
                    
                    val recordStatuses = recordsForLoan.map { record ->
                        val status = if (record.status == "PAID" || record.interestPaid >= record.interestAmount) {
                            "PAID"
                        } else if (record.interestPaid > 0.0) {
                            "PARTIAL"
                        } else {
                            val nextCal = Calendar.getInstance()
                            nextCal.timeInMillis = entity.loanDate
                            nextCal.add(Calendar.MONTH, record.monthNumber + 1)
                            val nextDueDate = nextCal.timeInMillis
                            
                            when {
                                todayMs < record.dueDate -> "UPCOMING"
                                todayMs >= record.dueDate && todayMs < nextDueDate -> "PENDING"
                                else -> "OVERDUE"
                            }
                        }
                        record.copy(status = status)
                    }

                    val unpaidRecords = recordStatuses.filter { it.status != "PAID" }
                    val currentRecord = unpaidRecords.minByOrNull { it.monthNumber } ?: recordsForLoan.maxByOrNull { it.monthNumber }
                    val currentMonthStr = if (currentRecord != null) "Month ${currentRecord.monthNumber}" else "Month 1"

                    val currentInterestDue = recordStatuses.filter { it.status == "PENDING" }.sumOf { it.interestAmount - it.interestPaid }
                    val pendingInterest = recordStatuses.filter { it.status == "OVERDUE" || it.status == "PARTIAL" }.sumOf { it.interestAmount - it.interestPaid }
                    val overallAmountDue = remainingPrincipal + currentInterestDue + pendingInterest

                    val hasOverdue = recordStatuses.any { it.status == "OVERDUE" }
                    val hasPending = recordStatuses.any { it.status == "PENDING" }
                    val pillStatus = when {
                        hasOverdue -> "OVERDUE"
                        hasPending -> "DUE"
                        else -> "CURRENT"
                    }

                    LoanMock(
                        id = entity.id,
                        borrowerId = entity.borrowerId,
                        borrowerName = entity.borrowerName,
                        amount = remainingPrincipal,
                        interestRate = entity.interestRate,
                        loanDate = DummyData.formatDate(entity.loanDate),
                        note = entity.note,
                        status = entity.status,
                        totalPaid = totalInterestPaid,
                        monthlyInterestAmount = (remainingPrincipal * entity.interestRate / 100.0),
                        currentMonth = currentMonthStr,
                        currentInterestDue = currentInterestDue,
                        pendingInterest = pendingInterest,
                        overallAmountDue = overallAmountDue,
                        pillStatus = pillStatus
                    )
                }

                _loans.value = mappedLoans

                _borrower.value = BorrowerMock(
                    id = borrowerEntity.id,
                    name = borrowerEntity.name,
                    phone = borrowerEntity.phone,
                    note = borrowerEntity.note,
                    totalBorrowed = mappedLoans.filter { it.status == "ACTIVE" }.sumOf { it.amount },
                    activeLoansCount = mappedLoans.count { it.status == "ACTIVE" },
                    lastPaymentDate = if (paymentsForBorrower.isNotEmpty()) DummyData.formatDate(paymentsForBorrower.first().paymentDate) else "N/A"
                )

                val timelineList = mutableListOf<TimelineItem>()
                
                // Add Loan Disbursed events first (oldest by nature)
                mappedLoans.forEach { loan ->
                    timelineList.add(TimelineItem.LoanCreated(loan))
                }

                // Gather and sort Monthly Interest Records
                val recordsForBorrower = interestRecords.filter { it.loanId in loanIds }
                
                val finalInterestTimeline = recordsForBorrower.map { record ->
                    val loan = loanEntities.find { it.id == record.loanId }
                    val status = if (record.status == "PAID" || record.interestPaid >= record.interestAmount) {
                        "PAID"
                    } else if (record.interestPaid > 0.0) {
                        "PARTIAL"
                    } else if (loan != null) {
                        val nextCal = Calendar.getInstance()
                        nextCal.timeInMillis = loan.loanDate
                        nextCal.add(Calendar.MONTH, record.monthNumber + 1)
                        val nextDueDate = nextCal.timeInMillis
                        
                        when {
                            todayMs < record.dueDate -> "UPCOMING"
                            todayMs >= record.dueDate && todayMs < nextDueDate -> "PENDING"
                            else -> "OVERDUE"
                        }
                    } else {
                        "UPCOMING"
                    }
                    TimelineItem.MonthlyInterest(
                        id = record.id,
                        loanId = record.loanId,
                        monthNumber = record.monthNumber,
                        interestAmount = record.interestAmount,
                        interestPaid = record.interestPaid,
                        dueDate = record.dueDate,
                        status = status,
                        paidDate = record.paidDate
                    )
                }.sortedBy { it.dueDate }

                timelineList.addAll(finalInterestTimeline)
                _timeline.value = timelineList
            }
        }
    }

    fun receivePayment(
        loanId: Long,
        paymentType: String, // Interest, Principal, Both
        amount: Double,
        interestPortion: Double,
        principalPortion: Double,
        paymentDate: Long,
        note: String
    ) {
        viewModelScope.launch {
            val payment = com.example.database.PaymentEntity(
                loanId = loanId,
                paymentType = paymentType,
                interestPaid = interestPortion,
                principalPaid = principalPortion,
                totalPaid = amount,
                paymentDate = paymentDate,
                note = note
            )
            repository.insertPayment(payment)
        }
    }

    fun undoLastPayment(loanId: Long) {
        viewModelScope.launch {
            val payments = repository.getPaymentsForLoanSync(loanId)
            val latest = payments.maxByOrNull { it.paymentDate }
            if (latest != null) {
                repository.deletePayment(latest)
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePaymentById(paymentId)
        }
    }

    fun deleteBorrower(onSuccess: () -> Unit) {
        val currentBorrower = _borrower.value ?: return
        viewModelScope.launch {
            try {
                val bEntity = repository.getBorrowerById(currentBorrower.id)
                if (bEntity != null) {
                    repository.deleteBorrower(bEntity)
                }
                repository.deleteLoansForBorrower(currentBorrower.id)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateLoan(
        loanId: Long,
        name: String,
        amount: Double,
        interestRate: Double,
        date: Long,
        note: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existingLoan = repository.getLoanById(loanId) ?: return@launch
                
                val bEntity = repository.getBorrowerById(existingLoan.borrowerId)
                if (bEntity != null && bEntity.name != name) {
                    repository.insertBorrower(bEntity.copy(name = name))
                }

                val updatedLoan = existingLoan.copy(
                    borrowerName = name,
                    amount = amount,
                    interestRate = interestRate,
                    loanDate = date,
                    note = note
                )
                repository.updateLoan(updatedLoan)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
