package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val borrowerId: Long,
    val borrowerName: String,
    val amount: Double,
    val interestRate: Double,
    val loanDate: Long,
    val note: String = "",
    val status: String = "ACTIVE", // ACTIVE, PAID
    val createdAt: Long = System.currentTimeMillis()
)
