package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val borrowerName: String,
    val type: String, // "NEW_LOAN", "PAYMENT_RECEIVED", "PRINCIPAL_RECEIVED", "INTEREST_RECEIVED", "PAYMENT_REVERSED"
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
