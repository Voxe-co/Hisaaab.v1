package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_interest_records")
data class MonthlyInterestRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val monthNumber: Int,
    val dueDate: Long,
    val interestAmount: Double,
    val interestPaid: Double = 0.0,
    val status: String, // PENDING, OVERDUE, PAID, UPCOMING, PARTIAL
    val paidDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
