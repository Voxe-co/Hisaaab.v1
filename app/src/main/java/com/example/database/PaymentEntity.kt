package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val monthlyRecordId: Long? = null,
    val paymentType: String, // Interest, Principal, Both
    val interestPaid: Double,
    val principalPaid: Double,
    val totalPaid: Double,
    val paymentDate: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
