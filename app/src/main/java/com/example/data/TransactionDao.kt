package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transaction_log ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionLog>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionLog)
}
