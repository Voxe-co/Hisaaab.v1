package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HisaabApplication
import com.example.util.ReportStatsMock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class ChartPoint(val label: String, val value: Float)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as HisaabApplication).repository

    // Combined Flow to calculate real stats dynamically
    val stats: StateFlow<ReportStatsMock> = combine(
        repository.allLoans,
        repository.allInterestRecords,
        repository.getAllPaymentsFlow()
    ) { loanEntities, interestRecords, allPayments ->
        val activeLoans = loanEntities.filter { it.status == "ACTIVE" }
        val activeLoanIds = activeLoans.map { it.id }.toSet()
        
        // Sum of payments for active loans
        val paymentsForActiveLoans = allPayments.filter { it.loanId in activeLoanIds }
        val principalPaidForActiveLoans = paymentsForActiveLoans.sumOf { it.principalPaid }
        
        // Total Outstanding Principal = total amount of active loans - principal paid on those active loans
        val totalOutstandingPrincipal = (activeLoans.sumOf { it.amount } - principalPaidForActiveLoans).coerceAtLeast(0.0)
        
        // Total Expected Monthly Interest = monthly interest rate applied to outstanding principal
        val totalMonthlyInterestExpected = activeLoans.sumOf { loan ->
            val bPayments = allPayments.filter { it.loanId == loan.id }
            val remainingP = (loan.amount - bPayments.sumOf { it.principalPaid }).coerceAtLeast(0.0)
            remainingP * (loan.interestRate / 100.0)
        }

        val totalInterestReceived = allPayments.sumOf { it.interestPaid }
        val totalPrincipalReceived = allPayments.sumOf { it.principalPaid }

        // Pending Interest from active interest records
        val totalPendingInterest = interestRecords
            .filter { it.loanId in activeLoanIds && it.interestPaid < it.interestAmount }
            .sumOf { it.interestAmount - it.interestPaid }

        val totalOverallAmountDue = totalOutstandingPrincipal + totalPendingInterest

        // Today's Collection
        val todayMs = System.currentTimeMillis()
        val totalTodaysCollection = allPayments
            .filter { isToday(it.paymentDate, todayMs) }
            .sumOf { it.totalPaid }

        val uniqueBorrowers = activeLoans.map { it.borrowerId }.distinct().size

        // Calculate dynamic collection rate (collected vs total expected interest from interest records)
        val totalExpectedInterestFromRecords = interestRecords.sumOf { it.interestAmount }
        val totalCollectedInterestFromRecords = interestRecords.sumOf { it.interestPaid }
        val rawCollectionRate = if (totalExpectedInterestFromRecords > 0) {
            (totalCollectedInterestFromRecords / totalExpectedInterestFromRecords * 100.0).toFloat()
        } else {
            100f
        }
        val roundedCollectionRate = Math.round(rawCollectionRate * 10) / 10f

        // Dynamic growth MoM of interest collections
        val now = Calendar.getInstance()
        val curMonth = now.get(Calendar.MONTH)
        val curYear = now.get(Calendar.YEAR)

        val prevMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonth = prevMonthCal.get(Calendar.MONTH)
        val prevYear = prevMonthCal.get(Calendar.YEAR)

        val curMonthPayments = allPayments.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.paymentDate }
            cal.get(Calendar.MONTH) == curMonth && cal.get(Calendar.YEAR) == curYear
        }.sumOf { it.interestPaid }

        val prevMonthPayments = allPayments.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.paymentDate }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }.sumOf { it.interestPaid }

        val rawGrowth = when {
            prevMonthPayments == 0.0 && curMonthPayments > 0.0 -> 100f
            prevMonthPayments == 0.0 && curMonthPayments == 0.0 -> 0f
            else -> (((curMonthPayments - prevMonthPayments) / prevMonthPayments) * 100.0).toFloat()
        }
        val roundedGrowth = Math.round(rawGrowth * 10) / 10f

        ReportStatsMock(
            totalOutstandingPrincipal = totalOutstandingPrincipal,
            totalMonthlyInterestExpected = totalMonthlyInterestExpected,
            totalCollectedInterest = totalInterestReceived,
            activeBorrowersCount = uniqueBorrowers,
            activeLoansCount = activeLoans.size,
            collectionRate = roundedCollectionRate,
            monthlyGrowthPercent = roundedGrowth,
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

    // Monthly receipts trend (interest paid over the last 6 months)
    val monthlyTrend: StateFlow<List<ChartPoint>> = repository.getAllPaymentsFlow()
        .combine(repository.allLoans) { allPayments, _ ->
            (5 downTo 0).map { monthsAgo ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -monthsAgo)
                val monthStr = getMonthAbbreviation(cal.get(Calendar.MONTH))
                val m = cal.get(Calendar.MONTH)
                val y = cal.get(Calendar.YEAR)
                
                val totalForMonth = allPayments.filter {
                    val pCal = Calendar.getInstance().apply { timeInMillis = it.paymentDate }
                    pCal.get(Calendar.MONTH) == m && pCal.get(Calendar.YEAR) == y
                }.sumOf { it.interestPaid }
                
                ChartPoint(monthStr, totalForMonth.toFloat())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf(
                ChartPoint("Jan", 0f),
                ChartPoint("Feb", 0f),
                ChartPoint("Mar", 0f),
                ChartPoint("Apr", 0f),
                ChartPoint("May", 0f),
                ChartPoint("Jun", 0f)
            )
        )

    // Outstanding principal distribution by borrower
    val principalDistribution: StateFlow<List<ChartPoint>> = combine(
        repository.allLoans,
        repository.getAllPaymentsFlow()
    ) { loanEntities, allPayments ->
        val activeLoans = loanEntities.filter { it.status == "ACTIVE" }
        
        val borrowerSums = activeLoans.groupBy { it.borrowerName }.mapValues { entry ->
            val bLoans = entry.value
            val bLoanIds = bLoans.map { it.id }.toSet()
            val bPayments = allPayments.filter { it.loanId in bLoanIds }
            val totalAmt = bLoans.sumOf { it.amount }
            val principalPaid = bPayments.sumOf { it.principalPaid }
            (totalAmt - principalPaid).coerceAtLeast(0.0)
        }.filter { it.value > 0.0 }

        val totalOutstanding = borrowerSums.values.sum()

        if (totalOutstanding > 0.0) {
            val sortedBorrowers = borrowerSums.toList().sortedByDescending { it.second }
            if (sortedBorrowers.size <= 5) {
                sortedBorrowers.map { (name, amt) ->
                    val percentage = ((amt / totalOutstanding) * 100.0).toFloat()
                    ChartPoint(name, Math.round(percentage * 10) / 10f)
                }
            } else {
                val top4 = sortedBorrowers.take(4)
                val othersAmt = sortedBorrowers.drop(4).sumOf { it.second }
                val topPoints = top4.map { (name, amt) ->
                    val percentage = ((amt / totalOutstanding) * 100.0).toFloat()
                    ChartPoint(name, Math.round(percentage * 10) / 10f)
                }
                val othersPercentage = ((othersAmt / totalOutstanding) * 100.0).toFloat()
                topPoints + ChartPoint("Others", Math.round(othersPercentage * 10) / 10f)
            }
        } else {
            listOf(ChartPoint("No Active Loans", 100f))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(ChartPoint("Loading", 100f))
    )

    private fun isToday(timestamp: Long, todayMs: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        val cal2 = Calendar.getInstance()
        cal2.timeInMillis = todayMs
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getMonthAbbreviation(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            else -> ""
        }
    }
}
