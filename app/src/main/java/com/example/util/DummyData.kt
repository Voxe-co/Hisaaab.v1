package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple model helpers for UI rendering
data class BorrowerMock(
    val id: Long,
    val name: String,
    val phone: String,
    val note: String,
    val totalBorrowed: Double,
    val activeLoansCount: Int,
    val lastPaymentDate: String,
    val isFavorite: Boolean = false,
    val tag: String = ""
)

data class LoanMock(
    val id: Long,
    val borrowerId: Long,
    val borrowerName: String,
    val amount: Double,
    val interestRate: Double, // monthly %
    val loanDate: String,
    val note: String,
    val status: String, // ACTIVE, PAID
    val totalPaid: Double,
    val monthlyInterestAmount: Double,
    val currentMonth: String = "Month 1",
    val currentInterestDue: Double = 0.0,
    val pendingInterest: Double = 0.0,
    val overallAmountDue: Double = 0.0,
    val pillStatus: String = "CURRENT",
    val isFavorite: Boolean = false,
    val tag: String = ""
)

data class PaymentMock(
    val id: Long,
    val loanId: Long,
    val amount: Double,
    val paymentDate: String,
    val note: String
)

data class ReportStatsMock(
    val totalOutstandingPrincipal: Double,
    val totalMonthlyInterestExpected: Double,
    val totalCollectedInterest: Double,
    val activeBorrowersCount: Int,
    val activeLoansCount: Int,
    val collectionRate: Float, // e.g., 0.92f
    val monthlyGrowthPercent: Float,
    val totalPendingInterest: Double = 0.0,
    val totalOverallAmountDue: Double = 0.0,
    val totalPrincipalReceived: Double = 0.0,
    val totalTodaysCollection: Double = 0.0
)

object DummyData {

    val stats = ReportStatsMock(
        totalOutstandingPrincipal = 1250000.0,
        totalMonthlyInterestExpected = 25000.0,
        totalCollectedInterest = 184500.0,
        activeBorrowersCount = 14,
        activeLoansCount = 19,
        collectionRate = 93.5f,
        monthlyGrowthPercent = 12.4f
    )

    val borrowers = listOf(
        BorrowerMock(1, "Ramesh Kumar", "+91 98765 43210", "Proprietor of Kirana Store", 450000.0, 2, "June 25, 2026"),
        BorrowerMock(2, "Anita Sharma", "+91 91234 56789", "Boutique owner", 300000.0, 1, "July 01, 2026"),
        BorrowerMock(3, "Sukhwinder Singh", "+91 99887 76655", "Dairy farm supply vendor", 250000.0, 1, "June 12, 2026"),
        BorrowerMock(4, "Mohammad Ali", "+91 88776 65544", "Mobile repair shop contract", 150000.0, 1, "June 29, 2026"),
        BorrowerMock(5, "Priya Nair", "+91 77665 54433", "Tuition Center expansion", 100000.0, 0, "May 04, 2026")
    )

    val loans = listOf(
        LoanMock(101, 1, "Ramesh Kumar", 300000.0, 2.0, "Jan 15, 2026", "Shop renovation expansion", "ACTIVE", 36000.0, 6000.0),
        LoanMock(102, 1, "Ramesh Kumar", 150000.0, 2.5, "Apr 10, 2026", "Inventory stocking", "ACTIVE", 11250.0, 3750.0),
        LoanMock(103, 2, "Anita Sharma", 300000.0, 2.0, "Feb 01, 2026", "Boutique sewing machine setup", "ACTIVE", 30000.0, 6000.0),
        LoanMock(104, 3, "Sukhwinder Singh", 250000.0, 1.8, "Mar 20, 2026", "Fodder machine loan", "ACTIVE", 18000.0, 4500.0),
        LoanMock(105, 4, "Mohammad Ali", 150000.0, 3.0, "May 10, 2026", "Store deposit", "ACTIVE", 9000.0, 4500.0),
        LoanMock(106, 5, "Priya Nair", 100000.0, 2.0, "Jan 01, 2026", "Study materials purchase", "PAID", 112000.0, 0.0)
    )

    val payments = listOf(
        PaymentMock(1, 101, 6000.0, "June 15, 2026", "Monthly Interest payment (June)"),
        PaymentMock(2, 102, 3750.0, "June 10, 2026", "Interest payment"),
        PaymentMock(3, 103, 6000.0, "July 01, 2026", "Interest paid via UPI"),
        PaymentMock(4, 101, 6000.0, "May 15, 2026", "Monthly Interest payment (May)"),
        PaymentMock(5, 104, 4500.0, "June 20, 2026", "Cash repayment received"),
        PaymentMock(6, 105, 4500.0, "June 10, 2026", "First month interest payment")
    )

    fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            format.format(amount)
        } catch (e: Exception) {
            "₹%,.2f".format(amount)
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
