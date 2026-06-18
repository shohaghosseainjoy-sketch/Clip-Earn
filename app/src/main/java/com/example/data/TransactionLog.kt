package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_log")
data class TransactionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Long,
    val isCredit: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
