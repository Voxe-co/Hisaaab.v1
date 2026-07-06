package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrowers")
data class BorrowerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = "",
    val isFavorite: Boolean = false,
    val tag: String = "", // Family, Friend, Business, High Priority
    val createdAt: Long = System.currentTimeMillis()
)
