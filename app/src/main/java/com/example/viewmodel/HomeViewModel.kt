package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HisaabApplication
import com.example.util.DummyData
import com.example.util.LoanMock
import com.example.util.ReportStatsMock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayCollectionItem(
    val borrowerId: Long,
    val borrowerName: String,
    val loanId: Long,
    val recordId: Long,
    val interestAmount: Double,
    val dueDate: Long,
    val status: String
)

data class UpcomingCollectionItem(
    val borrowerId: Long,
    val borrowerName: String,
    val loanId: Long,
    val interestAmount: Double,
    val dueDate: Long,
    val daysUntilDue: Int
)

data class OverdueCollectionItem(
    val borrowerId: Long,
    val borrowerName: String,
    val loanId: Long,
    val daysOverdue: Int,
    val pendingInterest: Double,
    val currentDue: Double
)

enum class AdvancedFilterOption(val label: String) {
    ALL("All"),
    CURRENT("Current"),
    DUE_TODAY("Due Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed"),
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_INTEREST("Highest Interest"),
    LOWEST_INTEREST("Lowest Interest"),
    HIGHEST_PRINCIPAL("Highest Principal"),
    LOWEST_PRINCIPAL("Lowest Principal")
}

data class ParsedLoanInput(
    val borrowerName: String,
    val amount: Double,
    val interestRate: Double
)

fun parseNaturalLanguageInput(input: String): ParsedLoanInput? {
    try {
        val normalized = input.lowercase(java.util.Locale.getDefault()).trim()
        
        // Pattern 1: name ko amount diya interest percent pe/par
        val regex1 = Regex("""([a-zA-Z\s]+)\sko\s+(\d+(?:\.\d+)?)\s+diya\s+(\d+(?:\.\d+)?)\s*(?:percent|%|per)""")
        val match1 = regex1.find(normalized)
        if (match1 != null) {
            val name = match1.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val amount = match1.groupValues[2].toDouble()
            val rate = match1.groupValues[3].toDouble()
            return ParsedLoanInput(name, amount, rate)
        }

        // Pattern 2: name amount interest%
        val regex2 = Regex("""([a-zA-Z\s]+)\s+(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s*(?:percent|%|per)""")
        val match2 = regex2.find(normalized)
        if (match2 != null) {
            val name = match2.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val amount = match2.groupValues[2].toDouble()
            val rate = match2.groupValues[3].toDouble()
            return ParsedLoanInput(name, amount, rate)
        }

        // Pattern 3: name ko amount pe rate percent
        val regex3 = Regex("""([a-zA-Z\s]+)\s+ko\s+(\d+(?:\.\d+)?)\s+(\d+(?:\.\d+)?)\s*(?:%|percent)?""")
        val match3 = regex3.find(normalized)
        if (match3 != null) {
            val name = match3.groupValues[1].trim().replaceFirstChar { it.uppercase() }
            val amount = match3.groupValues[2].toDouble()
            val rate = match3.groupValues[3].toDouble()
            return ParsedLoanInput(name, amount, rate)
        }
        
        // Simple word parser fallback
        val words = normalized.split("\\s+".toRegex())
        if (words.size >= 3) {
            val name = words[0].replaceFirstChar { it.uppercase() }
            val numbers = words.mapNotNull { it.replace("%", "").toDoubleOrNull() }
            if (numbers.size >= 2) {
                return ParsedLoanInput(name, numbers[0], numbers[1])
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as HisaabApplication).repository

    val recentActivities: StateFlow<List<com.example.database.ActivityLogEntity>> = repository.allActivityLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.seedActivityLogsIfEmpty()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val todayCollection: StateFlow<List<TodayCollectionItem>> = combine(
        repository.allLoans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow()
    ) { loanEntities, interestRecords, allPayments ->
        val todayMs = System.currentTimeMillis()
        val activeLoans = loanEntities.filter { it.status == "ACTIVE" }
        
        activeLoans.flatMap { loan ->
            val records = interestRecords.filter { it.loanId == loan.id }
            records.filter { record ->
                val isPaid = record.interestPaid >= record.interestAmount
                val daysDiff = getDaysDiff(record.dueDate, todayMs)
                !isPaid && daysDiff == 0
            }.map { record ->
                TodayCollectionItem(
                    borrowerId = loan.borrowerId,
                    borrowerName = loan.borrowerName,
                    loanId = loan.id,
                    recordId = record.id,
                    interestAmount = record.interestAmount - record.interestPaid,
                    dueDate = record.dueDate,
                    status = "DUE"
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingCollection: StateFlow<List<UpcomingCollectionItem>> = combine(
        repository.allLoans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow()
    ) { loanEntities, interestRecords, allPayments ->
        val todayMs = System.currentTimeMillis()
        val activeLoans = loanEntities.filter { it.status == "ACTIVE" }
        
        activeLoans.flatMap { loan ->
            val records = interestRecords.filter { it.loanId == loan.id }
            records.filter { record ->
                val isPaid = record.interestPaid >= record.interestAmount
                val daysDiff = getDaysDiff(record.dueDate, todayMs)
                !isPaid && daysDiff in 1..7
            }.map { record ->
                val daysUntilDue = getDaysDiff(record.dueDate, todayMs)
                UpcomingCollectionItem(
                    borrowerId = loan.borrowerId,
                    borrowerName = loan.borrowerName,
                    loanId = loan.id,
                    interestAmount = record.interestAmount - record.interestPaid,
                    dueDate = record.dueDate,
                    daysUntilDue = daysUntilDue
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val overdueCollection: StateFlow<List<OverdueCollectionItem>> = combine(
        repository.allLoans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow()
    ) { loanEntities, interestRecords, allPayments ->
        val todayMs = System.currentTimeMillis()
        val activeLoans = loanEntities.filter { it.status == "ACTIVE" }
        
        activeLoans.mapNotNull { loan ->
            val records = interestRecords.filter { it.loanId == loan.id }
            val overdueRecords = records.filter { record ->
                val isPaid = record.interestPaid >= record.interestAmount
                val daysDiff = getDaysDiff(record.dueDate, todayMs)
                !isPaid && daysDiff < 0
            }
            if (overdueRecords.isEmpty()) null
            else {
                val oldestRecord = overdueRecords.minByOrNull { it.dueDate }!!
                val daysOverdue = getDaysDiff(todayMs, oldestRecord.dueDate)
                val totalPendingInterest = overdueRecords.sumOf { it.interestAmount - it.interestPaid }
                
                // Current cycle's due or next due interest
                val currentRecord = records.find { getDaysDiff(it.dueDate, todayMs) == 0 }
                val currentDue = currentRecord?.let { it.interestAmount - it.interestPaid } ?: (loan.amount * loan.interestRate / 100.0)
                
                OverdueCollectionItem(
                    borrowerId = loan.borrowerId,
                    borrowerName = loan.borrowerName,
                    loanId = loan.id,
                    daysOverdue = daysOverdue,
                    pendingInterest = totalPendingInterest,
                    currentDue = currentDue
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getDaysDiff(time1: Long, time2: Long): Int {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val diffMs = cal1.timeInMillis - cal2.timeInMillis
        return (diffMs / (24 * 60 * 60 * 1000)).toInt()
    }

    fun receiveQuickPayment(loanId: Long, amount: Double) {
        viewModelScope.launch {
            val payment = com.example.database.PaymentEntity(
                loanId = loanId,
                paymentType = "Interest",
                interestPaid = amount,
                principalPaid = 0.0,
                totalPaid = amount,
                paymentDate = System.currentTimeMillis(),
                note = "Quick received from Today's Collection dashboard"
            )
            repository.insertPayment(payment)
        }
    }

    // Advanced Search & Filter Options
    private val _activeFilter = MutableStateFlow(AdvancedFilterOption.ALL)
    val activeFilter: StateFlow<AdvancedFilterOption> = _activeFilter.asStateFlow()

    fun onFilterChanged(filter: AdvancedFilterOption) {
        _activeFilter.value = filter
    }

    // Toggle borrower favorite status
    fun toggleFavorite(borrowerId: Long) {
        viewModelScope.launch {
            val b = repository.getBorrowerById(borrowerId)
            if (b != null) {
                repository.insertBorrower(b.copy(isFavorite = !b.isFavorite))
            }
        }
    }

    // Update borrower tag and note
    fun updateBorrowerTagAndNote(borrowerId: Long, tag: String, note: String) {
        viewModelScope.launch {
            val b = repository.getBorrowerById(borrowerId)
            if (b != null) {
                repository.insertBorrower(b.copy(tag = tag, note = note))
            }
        }
    }

    // Delete a borrower and all their active loans
    fun deleteBorrower(borrowerId: Long) {
        viewModelScope.launch {
            val b = repository.getBorrowerById(borrowerId)
            if (b != null) {
                repository.deleteBorrower(b)
                repository.deleteLoansForBorrower(borrowerId)
            }
        }
    }

    // Delete a single loan
    fun deleteLoan(loanId: Long) {
        viewModelScope.launch {
            val loan = repository.getLoanById(loanId)
            if (loan != null) {
                repository.deleteLoan(loan)
            }
        }
    }

    // Receive principal payment
    fun receivePrincipalPayment(loanId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val payment = com.example.database.PaymentEntity(
                loanId = loanId,
                paymentType = "Principal",
                interestPaid = 0.0,
                principalPaid = amount,
                totalPaid = amount,
                paymentDate = System.currentTimeMillis(),
                note = note
            )
            repository.insertPayment(payment)
        }
    }

    // Receive interest payment
    fun receiveInterestPayment(loanId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val payment = com.example.database.PaymentEntity(
                loanId = loanId,
                paymentType = "Interest",
                interestPaid = amount,
                principalPaid = 0.0,
                totalPaid = amount,
                paymentDate = System.currentTimeMillis(),
                note = note
            )
            repository.insertPayment(payment)
        }
    }

    // Create a loan from parsed natural language input
    fun createParsedLoan(name: String, amount: Double, rate: Double) {
        viewModelScope.launch {
            var b = repository.getBorrowerByName(name)
            if (b == null) {
                val bId = repository.insertBorrower(
                    com.example.database.BorrowerEntity(
                        name = name
                    )
                )
                b = repository.getBorrowerById(bId)
            }
            if (b != null) {
                repository.insertLoan(
                    com.example.database.LoanEntity(
                        borrowerId = b.id,
                        borrowerName = b.name,
                        amount = amount,
                        interestRate = rate,
                        loanDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Real-time flow of all loans in database, mapped with simple interest calculation formulas
    val loans: StateFlow<List<LoanMock>> = combine(
        repository.allLoans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow(),
        repository.allBorrowers
    ) { loanEntities, interestRecords, allPayments, borrowers ->
        val todayMs = System.currentTimeMillis()
        val mappedList = loanEntities.map { entity ->
            val borrower = borrowers.find { it.id == entity.borrowerId }
            val isFav = borrower?.isFavorite ?: false
            val tagStr = borrower?.tag ?: ""
            val noteStr = borrower?.note ?: ""

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
                note = entity.note.ifBlank { noteStr },
                status = entity.status,
                totalPaid = totalInterestPaid,
                monthlyInterestAmount = (remainingPrincipal * entity.interestRate / 100.0),
                currentMonth = currentMonthStr,
                currentInterestDue = currentInterestDue,
                pendingInterest = pendingInterest,
                overallAmountDue = overallAmountDue,
                pillStatus = pillStatus,
                isFavorite = isFav,
                tag = tagStr
            )
        }
        
        // Favorite borrowers appear first by default
        mappedList.sortedWith(compareByDescending<LoanMock> { it.isFavorite }.thenBy { it.borrowerName })
    }.combine(combine(_searchQuery, _activeFilter) { q, f -> Pair(q, f) }) { mapped, (query, filter) ->
        var result = mapped

        // Apply Search (Borrower Name, Phone Number, Loan Amount, Interest Rate, Status, Date)
        if (query.isNotBlank()) {
            result = result.filter { item ->
                item.borrowerName.contains(query, ignoreCase = true) ||
                item.note.contains(query, ignoreCase = true) ||
                item.amount.toString().contains(query) ||
                item.interestRate.toString().contains(query) ||
                item.status.contains(query, ignoreCase = true) ||
                item.loanDate.contains(query, ignoreCase = true) ||
                item.tag.contains(query, ignoreCase = true)
            }
        }

        // Apply Advanced Filters & Sorting
        result = when (filter) {
            AdvancedFilterOption.ALL -> result
            AdvancedFilterOption.CURRENT -> result.filter { it.pillStatus == "CURRENT" && it.status == "ACTIVE" }
            AdvancedFilterOption.DUE_TODAY -> result.filter { it.pillStatus == "DUE" && it.status == "ACTIVE" }
            AdvancedFilterOption.UPCOMING -> result.filter { it.pillStatus == "UPCOMING" || (it.pillStatus == "CURRENT" && it.status == "ACTIVE") }
            AdvancedFilterOption.OVERDUE -> result.filter { it.pillStatus == "OVERDUE" && it.status == "ACTIVE" }
            AdvancedFilterOption.COMPLETED -> result.filter { it.status == "PAID" }
            AdvancedFilterOption.NEWEST -> result.sortedByDescending { it.id }
            AdvancedFilterOption.OLDEST -> result.sortedBy { it.id }
            AdvancedFilterOption.HIGHEST_INTEREST -> result.sortedByDescending { it.interestRate }
            AdvancedFilterOption.LOWEST_INTEREST -> result.sortedBy { it.interestRate }
            AdvancedFilterOption.HIGHEST_PRINCIPAL -> result.sortedByDescending { it.amount }
            AdvancedFilterOption.LOWEST_PRINCIPAL -> result.sortedBy { it.amount }
        }

        // Keep favorites on top within filter unless it is an explicit sorting filter
        val isSortingFilter = filter in listOf(
            AdvancedFilterOption.NEWEST, AdvancedFilterOption.OLDEST,
            AdvancedFilterOption.HIGHEST_INTEREST, AdvancedFilterOption.LOWEST_INTEREST,
            AdvancedFilterOption.HIGHEST_PRINCIPAL, AdvancedFilterOption.LOWEST_PRINCIPAL
        )

        if (isSortingFilter) {
            result.sortedWith(compareByDescending { it.isFavorite })
        } else {
            result
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<ReportStatsMock> = combine(
        loans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow()
    ) { mappedLoans, interestRecords, allPayments ->
        val activeLoans = mappedLoans.filter { it.status == "ACTIVE" }
        val totalPrincipal = activeLoans.sumOf { it.amount }
        
        val totalInterestReceived = allPayments.sumOf { it.interestPaid }
        val totalPrincipalReceived = allPayments.sumOf { it.principalPaid }

        val activeLoanIds = activeLoans.map { it.id }.toSet()
        val activeInterestRecords = interestRecords.filter { it.loanId in activeLoanIds }
        val totalPendingInterest = activeInterestRecords
            .filter { it.status != "PAID" }
            .sumOf { it.interestAmount - it.interestPaid }
            
        val totalOverallAmountDue = totalPrincipal + totalPendingInterest

        // Today's Collection
        val todayMs = System.currentTimeMillis()
        val totalTodaysCollection = allPayments
            .filter { isToday(it.paymentDate, todayMs) }
            .sumOf { it.totalPaid }

        val uniqueBorrowers = activeLoans.map { it.borrowerId }.distinct().size

        ReportStatsMock(
            totalOutstandingPrincipal = totalPrincipal,
            totalMonthlyInterestExpected = activeLoans.sumOf { it.monthlyInterestAmount },
            totalCollectedInterest = totalInterestReceived,
            activeBorrowersCount = uniqueBorrowers,
            activeLoansCount = activeLoans.size,
            collectionRate = 100f,
            monthlyGrowthPercent = 0f,
            totalPendingInterest = totalPendingInterest,
            totalOverallAmountDue = totalOverallAmountDue,
            totalPrincipalReceived = totalPrincipalReceived,
            totalTodaysCollection = totalTodaysCollection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportStatsMock(0.0, 0.0, 0.0, 0, 0, 100f, 0f)
    )

    private fun isToday(timestamp: Long, todayMs: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        val cal2 = Calendar.getInstance()
        cal2.timeInMillis = todayMs
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleEmptyState(isEmpty: Boolean) {
        // No-op for DB-driven flow so empty state demo doesn't clear DB
    }

    fun createParsedLoanFromNaturalLanguage(inputString: String) {
        val parsed = parseNaturalLanguageInput(inputString)
        if (parsed != null) {
            createParsedLoan(parsed.borrowerName, parsed.amount, parsed.interestRate)
        }
    }
}
