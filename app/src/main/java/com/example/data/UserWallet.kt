package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val userId: String = "guest_user",
    val username: String = "Clapper_8473",
    val clipId: String = "12984732",
    val clapCoins: Long = 6250L,
    val cashUsd: Double = 1.35,
    val raffleTickets: Int = 18,
    val unclaimedChests: Int = 3,
    val videoProgress: Float = 0.0f, // progress from 0 to 180 seconds
    val referralCode: String = "FREE7788",
    val hasRedeemedCode: Boolean = false,
    val streakDays: Int = 5,
    val totalClapsGiven: Int = 142,
    val amazonPieces: Int = 16,
    val diamondChests: Int = 2,
    val woodenChests: Int = 3,
    val goldenChests: Int = 2,
    val luxuryChests: Int = 1,
    val claimedMin1: Boolean = false,
    val claimedMin2: Boolean = false
)
