package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ClapEarnRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val walletDao = database.userWalletDao()

    // Video completion & fraud prevention helpers
    fun isVideoCompleted(videoId: String): Boolean {
        val prefs = appContext.getSharedPreferences("clapearn_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("video_completed_$videoId", false)
    }

    fun markVideoCompleted(videoId: String) {
        val prefs = appContext.getSharedPreferences("clapearn_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("video_completed_$videoId", true).apply()
    }

    // Save uncollected chest of type "wood", "gold", or "diamond" to Firestore "user_chests"
    suspend fun saveUncollectedChestToFirestore(chestType: String, videoId: String) {
        if (!isFirebaseAvailable) return
        val auth = firebaseAuth ?: return
        val db = firestore ?: return
        val currentUser = auth.currentUser ?: return
        
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "userId" to currentUser.uid,
                    "chestType" to chestType, // "wood", "gold", "diamond"
                    "earnedAt" to System.currentTimeMillis(),
                    "videoId" to videoId,
                    "isOpened" to false
                )
                db.collection("user_chests").add(data)
                Log.d("ClapEarnRepository", "Uncollected chest of type $chestType stored in Firestore.")
                
                // Cleanup job: max 50 and older than 24h
                cleanupOldChestsFirestore(currentUser.uid)
            } catch (e: Exception) {
                Log.e("ClapEarnRepository", "Failed to save chest to Firestore: ${e.message}")
            }
        }
    }

    private fun cleanupOldChestsFirestore(uid: String) {
        val db = firestore ?: return
        val cutoff = System.currentTimeMillis() - 86400000 // 24 hours ago
        
        db.collection("user_chests")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null) {
                    val documents = snapshot.documents
                    // Delete expired or opened chests
                    for (doc in documents) {
                        val earnedAt = doc.getLong("earnedAt") ?: 0L
                        val isOpened = doc.getBoolean("isOpened") ?: false
                        if (earnedAt < cutoff || isOpened) {
                            db.collection("user_chests").document(doc.id).delete()
                        }
                    }
                    
                    // Enforce max 50 count limit
                    val remainingUnopened = documents.filter {
                        val earnedAt = it.getLong("earnedAt") ?: 0L
                        val isOpened = it.getBoolean("isOpened") ?: false
                        earnedAt >= cutoff && !isOpened
                    }.sortedBy { it.getLong("earnedAt") ?: 0L }
                    
                    if (remainingUnopened.size > 50) {
                        val toDeleteCount = remainingUnopened.size - 50
                        for (i in 0 until toDeleteCount) {
                            db.collection("user_chests").document(remainingUnopened[i].id).delete()
                        }
                    }
                }
            }
    }

    // Mark matching chest as opened in Firestore
    suspend fun markChestAsOpenedInFirestore(chestType: String) {
        if (!isFirebaseAvailable) return
        val auth = firebaseAuth ?: return
        val db = firestore ?: return
        val currentUser = auth.currentUser ?: return
        
        withContext(Dispatchers.IO) {
            try {
                db.collection("user_chests")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("chestType", chestType)
                    .whereEqualTo("isOpened", false)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            val docId = snapshot.documents[0].id
                            db.collection("user_chests").document(docId).update("isOpened", true)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ClapEarnRepository", "Failed to mark chest as opened in Firestore: ${e.message}")
            }
        }
    }

    // Firebase references with safe initialization checks
    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    var isFirebaseAvailable: Boolean = false
        private set

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDT1Wua3wSSafas7E6BKtsap5Srerw3-eE")
                        .setApplicationId("1:42422643851:android:777741ed77d8432dfcb88e")
                        .setProjectId("gen-lang-client-0386476779")
                        .setDatabaseUrl("https://gen-lang-client-0386476779-default-rtdb.firebaseio.com")
                        .setStorageBucket("gen-lang-client-0386476779.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.d("ClapEarnRepository", "Successfully initialized default FirebaseApp with programmatic options.")
                } catch (e: Exception) {
                    Log.e("ClapEarnRepository", "Failed to initialize programmatic app, trying context default: ${e.message}")
                    FirebaseApp.initializeApp(context)
                }
            }

            firebaseAuth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseAvailable = true
            Log.d("ClapEarnRepository", "Firebase system successfully initialized with local/remote bindings.")
        } catch (e: Exception) {
            isFirebaseAvailable = false
            Log.w("ClapEarnRepository", "Firebase initialization failed: ${e.message}")
        }
    }

    // Get current local wallet state reactively
    val currentWallet: Flow<UserWallet> = walletDao.getWalletFlow()
        .map { it ?: UserWallet() }
        .flowOn(Dispatchers.IO)

    suspend fun getWalletDirect(): UserWallet {
        return withContext(Dispatchers.IO) {
            walletDao.getWallet() ?: UserWallet()
        }
    }

    // Updates wallet locally and syncs to firestore if online
    suspend fun updateWallet(wallet: UserWallet) {
        withContext(Dispatchers.IO) {
            walletDao.updateWallet(wallet)
            syncToFirestore(wallet)
        }
    }

    // Attempt real Firestore syncing if initialized
    private suspend fun syncToFirestore(wallet: UserWallet) {
        if (!isFirebaseAvailable) return
        val auth = firebaseAuth ?: return
        val db = firestore ?: return
        val currentUser = auth.currentUser ?: return

        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "username" to wallet.username,
                    "clipId" to wallet.clipId,
                    "clapCoins" to wallet.clapCoins,
                    "cashUsd" to wallet.cashUsd,
                    "raffleTickets" to wallet.raffleTickets,
                    "unclaimedChests" to wallet.unclaimedChests,
                    "videoProgress" to wallet.videoProgress,
                    "referralCode" to wallet.referralCode,
                    "hasRedeemedCode" to wallet.hasRedeemedCode,
                    "streakDays" to wallet.streakDays,
                    "totalClapsGiven" to wallet.totalClapsGiven,
                    "amazonPieces" to wallet.amazonPieces,
                    "diamondChests" to wallet.diamondChests,
                    "woodenChests" to wallet.woodenChests,
                    "goldenChests" to wallet.goldenChests,
                    "luxuryChests" to wallet.luxuryChests,
                    "claimedMin1" to wallet.claimedMin1,
                    "claimedMin2" to wallet.claimedMin2
                )
                db.collection("users").document(currentUser.uid).set(data)
                Log.d("ClapEarnRepository", "Wallet synced with Firestore for user ${currentUser.uid}")
            } catch (e: Exception) {
                Log.e("ClapEarnRepository", "Failed to sync with Firestore: ${e.message}")
            }
        }
    }

    // Sync from Firestore to Local Room DB when authenticated
    suspend fun pullFromFirestore() {
        if (!isFirebaseAvailable) return
        val auth = firebaseAuth ?: return
        val db = firestore ?: return
        val currentUser = auth.currentUser ?: return

        withContext(Dispatchers.IO) {
            try {
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val wallet = UserWallet(
                                userId = currentUser.uid,
                                username = document.getString("username") ?: "Clapper_${currentUser.uid.take(4)}",
                                clipId = document.getString("clipId") ?: (10000000 + (0..9999999).random()).toString(),
                                clapCoins = document.getLong("clapCoins") ?: 6250L,
                                cashUsd = document.getDouble("cashUsd") ?: 1.35,
                                raffleTickets = document.getLong("raffleTickets")?.toInt() ?: 18,
                                unclaimedChests = document.getLong("unclaimedChests")?.toInt() ?: 3,
                                videoProgress = document.getDouble("videoProgress")?.toFloat() ?: 0.0f,
                                referralCode = document.getString("referralCode") ?: "FREE7788",
                                hasRedeemedCode = document.getBoolean("hasRedeemedCode") ?: false,
                                streakDays = document.getLong("streakDays")?.toInt() ?: 5,
                                totalClapsGiven = document.getLong("totalClapsGiven")?.toInt() ?: 142,
                                amazonPieces = document.getLong("amazonPieces")?.toInt() ?: 16,
                                diamondChests = document.getLong("diamondChests")?.toInt() ?: 2,
                                woodenChests = document.getLong("woodenChests")?.toInt() ?: 3,
                                goldenChests = document.getLong("goldenChests")?.toInt() ?: 2,
                                luxuryChests = document.getLong("luxuryChests")?.toInt() ?: 1,
                                claimedMin1 = document.getBoolean("claimedMin1") ?: false,
                                claimedMin2 = document.getBoolean("claimedMin2") ?: false
                            )
                            // Save locally
                            database.userWalletDao().getWalletFlow().let {
                                Log.d("ClapEarnRepository", "Local Room updated with remote Firestore values.")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e("ClapEarnRepository", "Failed to pull from Firestore: ${e.message}")
            }
        }
    }

    // Real Firebase Auth Accessors
    fun getFirebaseAuth(): FirebaseAuth? = firebaseAuth
    fun getFirebaseFirestore(): FirebaseFirestore? = firestore

    // Convenience functions for transactions
    suspend fun addClapCoins(count: Long) {
        val current = getWalletDirect()
        updateWallet(current.copy(clapCoins = current.clapCoins + count))
    }

    suspend fun spendClapCoins(count: Long): Boolean {
        val current = getWalletDirect()
        if (current.clapCoins >= count) {
            updateWallet(current.copy(clapCoins = current.clapCoins - count))
            return true
        }
        return false
    }

    suspend fun addCash(amount: Double) {
        val current = getWalletDirect()
        updateWallet(current.copy(cashUsd = current.cashUsd + amount))
    }

    suspend fun addRaffleTickets(count: Int) {
        val current = getWalletDirect()
        updateWallet(current.copy(raffleTickets = current.raffleTickets + count))
    }

    suspend fun addAmazonPieces(count: Int) {
        val current = getWalletDirect()
        updateWallet(current.copy(amazonPieces = current.amazonPieces + count))
    }

    suspend fun addUnclaimedChests(count: Int) {
        val current = getWalletDirect()
        updateWallet(current.copy(unclaimedChests = current.unclaimedChests + count))
    }

    suspend fun openChest(): Map<String, Any>? {
        val current = getWalletDirect()
        if (current.unclaimedChests <= 0) return null

        // Generate rewards (ClipClaps Chest logic)
        val coinsWon = (200..1200).random().toLong()
        val ticketsWon = if ((1..3).random() == 1) (1..3).random() else 0
        val cashWon = if ((1..10).random() == 1) 0.15 else 0.0
        val piecesWon = (8..20).random() // Always award pieces to make the ticket pieces popup instantly viewable!

        val nextWallet = current.copy(
            unclaimedChests = current.unclaimedChests - 1,
            clapCoins = current.clapCoins + coinsWon,
            raffleTickets = current.raffleTickets + ticketsWon,
            cashUsd = current.cashUsd + cashWon,
            amazonPieces = current.amazonPieces + piecesWon
        )
        updateWallet(nextWallet)

        return mapOf(
            "coins" to coinsWon,
            "tickets" to ticketsWon,
            "cash" to cashWon,
            "amazonPieces" to piecesWon
        )
    }

    suspend fun updateProgressDirectly(progress: Float) {
        val current = getWalletDirect()
        updateWallet(current.copy(videoProgress = progress))
    }

    suspend fun resetVideoProgressForNewVideo() {
        val current = getWalletDirect()
        updateWallet(current.copy(
            videoProgress = 0.0f,
            claimedMin1 = false,
            claimedMin2 = false
        ))
    }

    suspend fun setVideoProgressCompletedForCompletedVideo() {
        val current = getWalletDirect()
        updateWallet(current.copy(
            videoProgress = 60.0f,
            claimedMin1 = true,
            claimedMin2 = true
        ))
    }

    suspend fun claimVideoChest(type: String, videoId: String) {
        val current = getWalletDirect()
        var nextClaimedMin1 = current.claimedMin1
        var nextClaimedMin2 = current.claimedMin2
        var nextWooden = current.woodenChests
        var nextGolden = current.goldenChests
        var nextLuxury = current.luxuryChests
        
        val fType = when (type.lowercase()) {
            "wooden", "wood" -> "wood"
            "golden", "gold" -> "gold"
            "luxury", "diamond" -> "diamond"
            else -> type.lowercase()
        }
        
        saveUncollectedChestToFirestore(fType, videoId)
        
        when (type.lowercase()) {
            "wooden", "wood" -> {
                nextClaimedMin1 = true
                nextWooden += 1
            }
            "golden", "gold" -> {
                nextClaimedMin2 = true
                nextGolden += 1
            }
            "luxury", "diamond" -> {
                nextLuxury += 1
                markVideoCompleted(videoId)
                updateWallet(current.copy(
                    videoProgress = 60.0f, // cap at completion
                    claimedMin1 = true,
                    claimedMin2 = true,
                    luxuryChests = nextLuxury
                ))
                return
            }
        }
        
        updateWallet(current.copy(
            claimedMin1 = nextClaimedMin1,
            claimedMin2 = nextClaimedMin2,
            woodenChests = nextWooden,
            goldenChests = nextGolden,
            luxuryChests = nextLuxury
        ))
    }

    suspend fun updateVideoProgress(increment: Float): String? {
        val current = getWalletDirect()
        var newProgress = current.videoProgress + increment
        var newlyUnlocked: String? = null
        
        var nextClaimedMin1 = current.claimedMin1
        var nextClaimedMin2 = current.claimedMin2
        var nextWooden = current.woodenChests
        var nextGolden = current.goldenChests
        var nextLuxury = current.luxuryChests
        
        if (newProgress >= 20.0f && !current.claimedMin1) {
            nextClaimedMin1 = true
            nextWooden += 1
            newlyUnlocked = "wooden"
        }
        if (newProgress >= 40.0f && !current.claimedMin2) {
            nextClaimedMin2 = true
            nextGolden += 1
            newlyUnlocked = "golden"
        }
        if (newProgress >= 60.0f) {
            nextLuxury += 1
            newlyUnlocked = "luxury"
            newProgress = 0.0f
            nextClaimedMin1 = false
            nextClaimedMin2 = false
        }
        
        updateWallet(current.copy(
            videoProgress = newProgress,
            claimedMin1 = nextClaimedMin1,
            claimedMin2 = nextClaimedMin2,
            woodenChests = nextWooden,
            goldenChests = nextGolden,
            luxuryChests = nextLuxury
        ))
        
        return newlyUnlocked
    }

    suspend fun openWoodenChest(): Map<String, Any>? {
        val current = getWalletDirect()
        if (current.woodenChests <= 0) return null
        
        markChestAsOpenedInFirestore("wood")
        
        val coinsWon = (3..7).random().toLong()
        var cashWon = 0.0
        var ticketsWon = 0
        
        val bonusRoll = (1..100).random()
        if (bonusRoll <= 15) {
            cashWon = 0.05
        } else if (bonusRoll in 16..30) {
            ticketsWon = 1
        }
        
        val nextWallet = current.copy(
            woodenChests = current.woodenChests - 1,
            clapCoins = current.clapCoins + coinsWon,
            cashUsd = current.cashUsd + cashWon,
            raffleTickets = current.raffleTickets + ticketsWon
        )
        updateWallet(nextWallet)
        
        return mapOf(
            "chestType" to "Wooden",
            "coins" to coinsWon,
            "cash" to cashWon,
            "tickets" to ticketsWon
        )
    }

    suspend fun openGoldenChest(): Map<String, Any>? {
        val current = getWalletDirect()
        if (current.goldenChests <= 0) return null
        
        markChestAsOpenedInFirestore("gold")
        
        val coinsWon = (8..15).random().toLong()
        var cashWon = 0.0
        var ticketsWon = 0
        
        val bonusRoll = (1..100).random()
        if (bonusRoll <= 20) {
            cashWon = 0.10
        } else if (bonusRoll in 21..40) {
            ticketsWon = 2
        }
        
        val nextWallet = current.copy(
            goldenChests = current.goldenChests - 1,
            clapCoins = current.clapCoins + coinsWon,
            cashUsd = current.cashUsd + cashWon,
            raffleTickets = current.raffleTickets + ticketsWon
        )
        updateWallet(nextWallet)
        
        return mapOf(
            "chestType" to "Golden",
            "coins" to coinsWon,
            "cash" to cashWon,
            "tickets" to ticketsWon
        )
    }

    suspend fun openLuxuryChest(): Map<String, Any>? {
        val current = getWalletDirect()
        if (current.luxuryChests <= 0) return null
        
        markChestAsOpenedInFirestore("diamond")
        
        val coinsWon = (15..30).random().toLong()
        var cashWon = 0.0
        var ticketsWon = 0
        
        val bonusRoll = (1..100).random()
        if (bonusRoll <= 25) {
            cashWon = 0.20
        } else if (bonusRoll in 26..50) {
            ticketsWon = 3
        }
        
        val nextWallet = current.copy(
            luxuryChests = current.luxuryChests - 1,
            clapCoins = current.clapCoins + coinsWon,
            cashUsd = current.cashUsd + cashWon,
            raffleTickets = current.raffleTickets + ticketsWon
        )
        updateWallet(nextWallet)
        
        return mapOf(
            "chestType" to "Luxury",
            "coins" to coinsWon,
            "cash" to cashWon,
            "tickets" to ticketsWon
        )
    }

    suspend fun redeemReferral(code: String): RedeemResult {
        val current = getWalletDirect()
        if (current.hasRedeemedCode) {
            return RedeemResult.AlreadyRedeemed
        }
        if (code.uppercase() == current.referralCode.uppercase()) {
            return RedeemResult.OwnCode
        }
        if (code.length < 4) {
            return RedeemResult.InvalidCode
        }

        // ClipClaps Referral Reward: $1.00 USD!
        updateWallet(current.copy(
            hasRedeemedCode = true,
            cashUsd = current.cashUsd + 1.00
        ))
        return RedeemResult.Success(1.00)
    }

    suspend fun addDiamondChests(count: Int) {
        val current = getWalletDirect()
        updateWallet(current.copy(diamondChests = current.diamondChests + count))
    }

    suspend fun openDiamondChest(): Map<String, Any>? {
        val current = getWalletDirect()
        if (current.diamondChests <= 0) return null

        // Diamond Chest contents: random prize from iPad/Cash/AirJordan/coins pool
        // iPad Pro 11, Cash ($1, $5, $10, etc.), Air Jordan 1 sneaker, or 5000 - 25000 ClapCoins!
        val rolledValue = (1..100).random()
        val prizeType: String
        val prizeValue: String
        var coinsWon = 0L
        var cashWon = 0.0

        when {
            rolledValue <= 2 -> {
                prizeType = "iPad Pro 11"
                prizeValue = "1x Premium iPad Pro 11 (Claim via Email)"
            }
            rolledValue <= 6 -> {
                prizeType = "Air Jordan 1"
                prizeValue = "1x Air Jordan 1 Retro Sneaker (Claim via Email)"
            }
            rolledValue <= 25 -> {
                prizeType = "Cash Reward"
                val cashAmount = listOf(1.00, 2.00, 5.00, 10.00).random()
                prizeValue = "$${String.format("%.2f", cashAmount)} USD"
                cashWon = cashAmount
            }
            else -> {
                prizeType = "ClapCoins Pack"
                val coinsAmount = (2000..8000).random().toLong()
                prizeValue = "🟡 $coinsAmount ClapCoins"
                coinsWon = coinsAmount
            }
        }

        val nextWallet = current.copy(
            diamondChests = current.diamondChests - 1,
            clapCoins = current.clapCoins + coinsWon,
            cashUsd = current.cashUsd + cashWon
        )
        updateWallet(nextWallet)

        return mapOf(
            "prizeType" to prizeType,
            "prizeValue" to prizeValue,
            "coins" to coinsWon,
            "cash" to cashWon
        )
    }

    // Records a referral install event in Firestore 'referrals' collection
    suspend fun addReferralToFirestore(friendName: String, status: String) {
        if (!isFirebaseAvailable) return
        val db = firestore ?: return
        val current = getWalletDirect()
        val inviterId = current.userId

        withContext(Dispatchers.IO) {
            try {
                val refData = mapOf(
                    "inviterId" to inviterId,
                    "inviterCode" to current.referralCode,
                    "friendName" to friendName,
                    "status" to status,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("referrals").add(refData)
                Log.d("ClapEarnRepository", "Referral recorded successfully in Firestore 'referrals' collection.")
            } catch (e: Exception) {
                Log.e("ClapEarnRepository", "Failed to write referral: ${e.message}")
            }
        }
    }
}

sealed class RedeemResult {
    data class Success(val rewardAmount: Double) : RedeemResult()
    object AlreadyRedeemed : RedeemResult()
    object OwnCode : RedeemResult()
    object InvalidCode : RedeemResult()
}
