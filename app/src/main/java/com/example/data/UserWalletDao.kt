package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWalletDao {
    @Query("SELECT * FROM user_wallet WHERE userId = :userId LIMIT 1")
    fun getWalletFlow(userId: String = "guest_user"): Flow<UserWallet?>

    @Query("SELECT * FROM user_wallet WHERE userId = :userId LIMIT 1")
    suspend fun getWallet(userId: String = "guest_user"): UserWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWallet(wallet: UserWallet)
}
