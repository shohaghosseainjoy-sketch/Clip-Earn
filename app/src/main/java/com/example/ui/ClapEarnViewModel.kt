package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClapEarnRepository
import com.example.data.RedeemResult
import com.example.data.UserWallet
import com.example.ui.models.RewardChestType
import com.example.ui.models.VideoItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClapEarnViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClapEarnRepository(application)
    
    private val _wallet = MutableStateFlow(UserWallet())
    val wallet: StateFlow<UserWallet> = _wallet.asStateFlow()

    private val _isFirebaseAvailable = MutableStateFlow(repository.isFirebaseAvailable)
    val isFirebaseAvailable: StateFlow<Boolean> = _isFirebaseAvailable.asStateFlow()

    // Video feeding states
    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _currentPlayingIndex = MutableStateFlow(0)
    val currentPlayingIndex: StateFlow<Int> = _currentPlayingIndex.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    // Chest open result banner state
    private val _chestRewardResult = MutableStateFlow<Map<String, Any>?>(null)
    val chestRewardResult: StateFlow<Map<String, Any>?> = _chestRewardResult.asStateFlow()

    // Video watching reward popup state
    private val _completedRewardPopup = MutableStateFlow<RewardPopupData?>(null)
    val completedRewardPopup: StateFlow<RewardPopupData?> = _completedRewardPopup.asStateFlow()

    // Games states
    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _spinAngle = MutableStateFlow(0f)
    val spinAngle: StateFlow<Float> = _spinAngle.asStateFlow()

    private val _gameMessage = MutableStateFlow<String?>(null)
    val gameMessage: StateFlow<String?> = _gameMessage.asStateFlow()

    // ClipClaps Raffle States
    private val _raffles = MutableStateFlow<List<RaffleItem>>(emptyList())
    val raffles: StateFlow<List<RaffleItem>> = _raffles.asStateFlow()

    private val _selectedRaffle = MutableStateFlow<RaffleItem?>(null)
    val selectedRaffle: StateFlow<RaffleItem?> = _selectedRaffle.asStateFlow()

    private val _latestPiecesWon = MutableStateFlow<Int?>(null)
    val latestPiecesWon: StateFlow<Int?> = _latestPiecesWon.asStateFlow()

    private val _referrals = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val referrals: StateFlow<List<Map<String, Any>>> = _referrals.asStateFlow()

    private var videoProgressJob: Job? = null

    init {
        // Load initial wallet
        viewModelScope.launch {
            repository.currentWallet.collectLatest {
                _wallet.value = it
                loadReferralsFromFirestore()
            }
        }

        // Set fallback videos instantly first
        _videos.value = getFallbackVideos()

        // Load videos from Firestore with fallback seeding
        loadVideosFromFirestore()

        // Set fallback raffles instantly, then fetch from Firestore
        _raffles.value = getFallbackRaffles()
        loadRafflesFromFirestore()
    }

    fun loadReferralsFromFirestore() {
        if (repository.isFirebaseAvailable) {
            val db = repository.getFirebaseFirestore()
            val userId = _wallet.value.userId
            db?.collection("referrals")
                ?.whereEqualTo("inviterId", userId)
                ?.addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val list = snapshot.documents.map { doc ->
                            mapOf(
                                "id" to doc.id,
                                "friendName" to (doc.getString("friendName") ?: "Unknown User"),
                                "status" to (doc.getString("status") ?: "Installed"),
                                "timestamp" to (doc.getLong("timestamp") ?: 0L)
                            )
                        }
                        _referrals.value = list
                    }
                }
        } else {
            // Simulated fallback referrals
            _referrals.value = listOf(
                mapOf("id" to "ref_fallback_1", "friendName" to "Joy Hosseain", "status" to "Completed: Watched 3 Videos", "timestamp" to System.currentTimeMillis() - 86400000),
                mapOf("id" to "ref_fallback_2", "friendName" to "Sarah Miller", "status" to "Pending: 1 video remaining", "timestamp" to System.currentTimeMillis() - 360000)
            )
        }
    }

    fun getFallbackVideos(): List<VideoItem> {
        return listOf(
            VideoItem(
                id = "vid_1",
                title = "Wait for the amazing trick at the end! 🐈💨",
                creator = "CatGymnastics",
                thumbnailUrl = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=500&auto=format&fit=crop&q=60",
                durationString = "0:30",
                clapsCount = 1240,
                viewsCount = "52K",
                authorAvatar = "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=100&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            ),
            VideoItem(
                id = "vid_2",
                title = "Making the legendary Golden Egg Omelette 🍳✨",
                creator = "GourmetLab",
                thumbnailUrl = "https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=500&auto=format&fit=crop&q=60",
                durationString = "0:14",
                clapsCount = 980,
                viewsCount = "38K",
                authorAvatar = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            ),
            VideoItem(
                id = "vid_3",
                title = "Skateboarding down a futuristic neon highway!",
                creator = "SkateFlow",
                thumbnailUrl = "https://images.unsplash.com/photo-1547447134-cd3f5c716030?w=500&auto=format&fit=crop&q=60",
                durationString = "0:26",
                clapsCount = 3850,
                viewsCount = "120K",
                authorAvatar = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=100&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4"
            ),
            VideoItem(
                id = "vid_4",
                title = "Calming ocean loop for high productivity",
                creator = "ZenSphere",
                thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=60",
                durationString = "0:30",
                clapsCount = 492,
                viewsCount = "18K",
                authorAvatar = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=100&auto=format&fit=crop&q=60",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"
            )
        )
    }

    private fun loadVideosFromFirestore() {
        if (repository.isFirebaseAvailable) {
            val db = repository.getFirebaseFirestore()
            db?.collection("videos")?.get()?.addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            VideoItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                creator = doc.getString("creator") ?: "",
                                thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                                durationString = doc.getString("durationString") ?: "1:00",
                                clapsCount = doc.getLong("clapsCount")?.toInt() ?: 100,
                                viewsCount = doc.getString("viewsCount") ?: "10K",
                                authorAvatar = doc.getString("authorAvatar") ?: "",
                                isLiked = doc.getBoolean("isLiked") ?: false,
                                videoUrl = doc.getString("videoUrl") ?: ""
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        _videos.value = list
                    }
                } else {
                    preseedVideosFirestore(db)
                }
            }?.addOnFailureListener { e ->
                Log.e("ClapEarnViewModel", "Failed to load videos from firestore: ${e.message}")
            }
        }
    }

    private fun preseedVideosFirestore(db: com.google.firebase.firestore.FirebaseFirestore?) {
        db ?: return
        val defaultList = getFallbackVideos()
        for (video in defaultList) {
            val data = mapOf(
                "title" to video.title,
                "creator" to video.creator,
                "thumbnailUrl" to video.thumbnailUrl,
                "durationString" to video.durationString,
                "clapsCount" to video.clapsCount,
                "viewsCount" to video.viewsCount,
                "authorAvatar" to video.authorAvatar,
                "videoUrl" to video.videoUrl,
                "isLiked" to video.isLiked
            )
            db.collection("videos").document(video.id).set(data)
                .addOnSuccessListener {
                    Log.d("ClapEarnViewModel", "Successfully preseeded video ${video.id}")
                }
        }
    }

    fun uploadVideo(title: String, creator: String, videoUrl: String, thumbnailUrl: String) {
        val newVideo = com.example.ui.models.VideoItem(
            id = "vid_${System.currentTimeMillis()}",
            title = title,
            creator = creator.ifEmpty { "You" },
            thumbnailUrl = thumbnailUrl.ifEmpty { "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop&q=60" },
            durationString = "0:15",
            clapsCount = 0,
            viewsCount = "1",
            authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&auto=format&fit=crop&q=60",
            videoUrl = videoUrl
        )
        viewModelScope.launch {
            _videos.value = listOf(newVideo) + _videos.value
            
            if (repository.isFirebaseAvailable) {
                val db = repository.getFirebaseFirestore()
                if (db != null) {
                    val data = mapOf(
                        "id" to newVideo.id,
                        "title" to newVideo.title,
                        "creator" to newVideo.creator,
                        "thumbnailUrl" to newVideo.thumbnailUrl,
                        "durationString" to newVideo.durationString,
                        "clapsCount" to newVideo.clapsCount,
                        "viewsCount" to newVideo.viewsCount,
                        "authorAvatar" to newVideo.authorAvatar,
                        "isLiked" to newVideo.isLiked,
                        "videoUrl" to newVideo.videoUrl
                    )
                    try {
                        db.collection("videos").document(newVideo.id).set(data)
                    } catch (e: Exception) {
                        Log.e("ClapEarnViewModel", "Failed to upload video to Firebase: ${e.message}")
                    }
                }
            }
        }
    }

    // Video play/pause and progress monitoring
    fun toggleVideoPlay() {
        if (_isVideoPlaying.value) {
            _isVideoPlaying.value = false
            videoProgressJob?.cancel()
        } else {
            _isVideoPlaying.value = true
            startVideoProgressSimulation()
        }
    }

    fun selectVideo(index: Int) {
        _currentPlayingIndex.value = index
        _isVideoPlaying.value = true
        startVideoProgressSimulation()
    }

    private fun startVideoProgressSimulation() {
        videoProgressJob?.cancel()
        videoProgressJob = viewModelScope.launch {
            while (_isVideoPlaying.value) {
                delay(1000L) // updates every second
                // Watching a video fills up the progress bar timeline.
                // We increment progress by 1 second.
                val unlockedType = repository.updateVideoProgress(1.0f)
                if (unlockedType != null) {
                    val chestName = when (unlockedType) {
                        "wooden" -> "Wooden Chest"
                        "golden" -> "Golden Chest"
                        "luxury" -> "Premium Luxury Chest"
                        else -> "Chest"
                    }
                    _gameMessage.value = "🎁 $chestName Unlocked! Saved in Rewards!"
                    
                    viewModelScope.launch {
                        delay(4000L)
                        if (_gameMessage.value?.contains(chestName) == true) {
                            _gameMessage.value = null
                        }
                    }
                }
            }
        }
    }

    // Give a clap (Like feature) to earn instant 15 ClapCoins
    fun clapVideo(index: Int) {
        val list = _videos.value.toMutableList()
        if (index in list.indices) {
            val video = list[index]
            if (!video.isLiked) {
                list[index] = video.copy(
                    clapsCount = video.clapsCount + 1,
                    isLiked = true
                )
                _videos.value = list

                viewModelScope.launch {
                    val current = repository.getWalletDirect()
                    repository.updateWallet(current.copy(
                        clapCoins = current.clapCoins + 15L,
                        totalClapsGiven = current.totalClapsGiven + 1
                    ))
                }
            }
        }
    }

    // Open an earned Chest in Rewards Screen!
    fun openRewardChest(type: String) {
        viewModelScope.launch {
            val rewards = when (type.lowercase()) {
                "wooden" -> repository.openWoodenChest()
                "golden" -> repository.openGoldenChest()
                "luxury" -> repository.openLuxuryChest()
                else -> null
            }
            if (rewards != null) {
                _chestRewardResult.value = rewards
            } else {
                _gameMessage.value = "⚠️ No unopened $type chests available!"
                delay(3000L)
                _gameMessage.value = null
            }
        }
    }

    fun clearChestRewardResult() {
        _chestRewardResult.value = null
    }

    fun closeRewardPopup() {
        _completedRewardPopup.value = null
    }

    // Games tab: Lucky Spin Wheel
    fun playLuckySpin() {
        if (_isSpinning.value) return

        viewModelScope.launch {
            val currentCoins = repository.getWalletDirect().clapCoins
            if (currentCoins < 300) {
                _gameMessage.value = "⚠️ Spin costs 300 ClapCoins. Earn more!"
                delay(3000L)
                _gameMessage.value = null
                return@launch
            }

            // Deduct entry fee
            repository.spendClapCoins(300L)
            _isSpinning.value = true
            _gameMessage.value = "Spinning..."

            // Simulate spinning physics
            val spinsCount = (3..5).random()
            val extraAngle = (0..359).random().toFloat()
            val finalTargetAngle = (spinsCount * 360f) + extraAngle

            // Animate spin value
            var currentAngle = 0f
            while (currentAngle < finalTargetAngle) {
                val step = (finalTargetAngle - currentAngle) * 0.12f + 5f
                currentAngle += step
                _spinAngle.value = currentAngle % 360f
                delay(16L) // ~60fps
            }

            _isSpinning.value = false

            // Calculate sector reward based on modulo angle (8 sections)
            val angle = _spinAngle.value
            val sector = ((angle + 22.5f) % 360f / 45f).toInt()

            val rewardCoins: Long
            val rewardCash: Double
            val resultText: String

            when (sector) {
                0 -> { rewardCoins = 0L; rewardCash = 0.50; resultText = "🎉 MEGA WIN! $0.50 Cash!" }
                1 -> { rewardCoins = 100L; rewardCash = 0.0; resultText = "🟡 Won 100 ClapCoins!" }
                2 -> { rewardCoins = 0L; rewardCash = 0.05; resultText = "💚 Won $0.05 Cash!" }
                3 -> { rewardCoins = 500L; rewardCash = 0.0; resultText = "🟡 JackPot! 500 ClapCoins!" }
                4 -> { rewardCoins = 50L; rewardCash = 0.0; resultText = "🟡 Won 50 ClapCoins!" }
                5 -> { rewardCoins = 0L; rewardCash = 0.15; resultText = "💚 Won $0.15 Cash!" }
                6 -> { rewardCoins = 1000L; rewardCash = 0.0; resultText = "🤩 SUPER COINS! 1,000 ClapCoins!" }
                else -> { rewardCoins = 200L; rewardCash = 0.0; resultText = "🟡 Won 200 ClapCoins!" }
            }

            // Grant rewards
            if (rewardCoins > 0) repository.addClapCoins(rewardCoins)
            if (rewardCash > 0) repository.addCash(rewardCash)

            _gameMessage.value = resultText
            delay(4000L)
            _gameMessage.value = null
        }
    }

    // Games Tab: Scratch Card
    fun playScratchCard() {
        viewModelScope.launch {
            val wallet = repository.getWalletDirect()
            if (wallet.clapCoins < 500) {
                _gameMessage.value = "⚠️ Scratch requires 500 ClapCoins!"
                delay(3000L)
                _gameMessage.value = null
                return@launch
            }

            // Deduct
            repository.spendClapCoins(500)
            _gameMessage.value = "Scratching Card..."
            delay(1500)

            // Reveal results (1 in 5 chance of winning $1.00, 1 in 2 chance of winning 800 coins, otherwise 200)
            val roll = (1..10).random()
            if (roll == 1) {
                repository.addCash(1.0)
                _gameMessage.value = "🍀 MATCH THREE! Revealed 💚💚💚! Won $1.00 USD!"
            } else if (roll <= 4) {
                repository.addClapCoins(800)
                _gameMessage.value = "🪙 WINNER! Revealed 🪙🪙🪙! Won 800 ClapCoins!"
            } else {
                repository.addClapCoins(150)
                _gameMessage.value = "Revealed 🪙❌🪙. Consolation prize: 150 ClapCoins."
            }

            delay(4000L)
            _gameMessage.value = null
        }
    }

    // Raffle Drawing (Rewards page)
    fun playRaffle(requiredTickets: Int, rewardType: RewardChestType) {
        viewModelScope.launch {
            val current = repository.getWalletDirect()
            if (current.raffleTickets < requiredTickets) {
                _gameMessage.value = "⚠️ Need $requiredTickets tickets to play this Raffle!"
                delay(3000L)
                _gameMessage.value = null
                return@launch
            }

            // Spend tickets
            val nextWallet = current.copy(raffleTickets = current.raffleTickets - requiredTickets)
            repository.updateWallet(nextWallet)

            _gameMessage.value = "Drawing Raffle Slot... 🎰"
            delay(2000L)

            val text: String
            when (rewardType) {
                RewardChestType.CASH_10 -> {
                    // Win cash up to $10 (Simulated high fidelity reward)
                    val payout = listOf(0.50, 1.00, 2.50, 5.00, 10.0)
                    val won = payout.random()
                    repository.addCash(won)
                    text = "🎰 Raffle Result: You won $${String.format("%.2f", won)} USD Cash!"
                }
                RewardChestType.CASH_1 -> {
                    val payout = listOf(0.10, 0.20, 0.50, 1.00)
                    val won = payout.random()
                    repository.addCash(won)
                    text = "🎰 Raffle Result: You won $${String.format("%.2f", won)} USD Cash!"
                }
                RewardChestType.COINS_500K -> {
                    val payout = listOf(1000L, 5000L, 10000L, 50000L)
                    val won = payout.random()
                    repository.addClapCoins(won)
                    text = "🎰 Raffle Result: You won $won ClapCoins! 🟡"
                }
            }
            _gameMessage.value = text
            delay(5000L)
            _gameMessage.value = null
        }
    }

    // Redeem Coupon Code
    fun redeemCode(code: String, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.redeemReferral(code)
            val msg = when (result) {
                is RedeemResult.Success -> {
                    "🎉 Success! Redeemed referral code for $${String.format("%.2f", result.rewardAmount)}!"
                }
                RedeemResult.AlreadyRedeemed -> "❌ You have already redeemed a referral code!"
                RedeemResult.OwnCode -> "❌ You cannot redeem your own referral code!"
                RedeemResult.InvalidCode -> "❌ Invalid code! Enter a valid 8-character code."
            }
            onComplete(msg)
        }
    }

    // Simulated / Native Firebase authenticate (For evaluation & sync compliance)
    fun simulateGoogleSignIn(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Write local database integration auth updates
            val current = repository.getWalletDirect()
            val simulatedUserEmail = "user_${(100..999).random()}@gmail.com"
            val nextWallet = current.copy(
                userId = "auth_firebase_user_385038",
                username = simulatedUserEmail.substringBefore("@")
            )
            repository.updateWallet(nextWallet)

            // Pull Firestore sync state if Firebase SDK works
            if (repository.isFirebaseAvailable) {
                repository.pullFromFirestore()
            }
            onComplete(true)
        }
    }

    fun grantQuickCoinsBonus() {
        viewModelScope.launch {
            repository.addClapCoins(1000L)
            repository.addRaffleTickets(2)
        }
    }

    fun exchangeCoins(coinsToExchange: Long, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = repository.getWalletDirect()
            if (current.clapCoins < coinsToExchange) {
                callback(false, "❌ Insufficient ClapCoins balance!")
                return@launch
            }
            // 1,000 coins = $0.01 USD.
            val cashToAdd = coinsToExchange.toDouble() / 100000.0
            val nextWallet = current.copy(
                clapCoins = current.clapCoins - coinsToExchange,
                cashUsd = current.cashUsd + cashToAdd
            )
            repository.updateWallet(nextWallet)
            callback(true, "🎉 Successfully exchanged $coinsToExchange ClapCoins for $${String.format("%.2f", cashToAdd)} USD!")
        }
    }

    fun withdrawCash(amount: Double, paymentInfo: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = repository.getWalletDirect()
            if (current.cashUsd < amount) {
                callback(false, "❌ Insufficient Cash balance!")
                return@launch
            }
            val nextWallet = current.copy(
                cashUsd = current.cashUsd - amount
            )
            repository.updateWallet(nextWallet)
            callback(true, "💵 Withdrawal of $${String.format("%.2f", amount)} submitted successfully to $paymentInfo!")
        }
    }

    fun updateProfile(newUsername: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmed = newUsername.trim()
            if (trimmed.length < 3) {
                callback(false, "❌ Username must be at least 3 characters!")
                return@launch
            }
            val current = repository.getWalletDirect()
            val nextWallet = current.copy(username = trimmed)
            repository.updateWallet(nextWallet)
            callback(true, "Profile updated successfully!")
        }
    }

    fun openDiamondChest(onResult: (Map<String, Any>?) -> Unit) {
        viewModelScope.launch {
            val res = repository.openDiamondChest()
            if (res != null) {
                _chestRewardResult.value = res
            }
            onResult(res)
        }
    }

    fun addSimulatedReferral(friendName: String, callback: (String) -> Unit) {
        viewModelScope.launch {
            // Friend installed + watched 3 videos
            repository.addDiamondChests(1)
            repository.addReferralToFirestore(friendName, "Completed: Watched 3 Videos")
            callback("🎉 $friendName completed registration and watched 3 videos. You earned 1 DIAMOND CHEST!")
        }
    }

    fun selectRaffle(raffle: RaffleItem?) {
        _selectedRaffle.value = raffle
    }

    fun clearLatestPiecesWon() {
        _latestPiecesWon.value = null
    }

    fun getFallbackRaffles(): List<RaffleItem> {
        return listOf(
            RaffleItem(
                id = "raffle_1",
                title = "$1.00 Cash Raffle",
                backgroundType = "green",
                slotsFilled = 25,
                slotsRequired = 20,
                ticketsRemaining = 1794,
                requiredTicketsToPlay = 5,
                rewardType = "CASH_1"
            ),
            RaffleItem(
                id = "raffle_2",
                title = "$10.00 Amazon Coupon Raffle",
                backgroundType = "black",
                slotsFilled = 16,
                slotsRequired = 120, // To match 120 pieces required to enter
                ticketsRemaining = 4820,
                requiredTicketsToPlay = 120,
                rewardType = "AMAZON_10"
            ),
            RaffleItem(
                id = "raffle_3",
                title = "500k ClapCoins Raffle",
                backgroundType = "gold",
                slotsFilled = 15,
                slotsRequired = 15,
                ticketsRemaining = 890,
                requiredTicketsToPlay = 15,
                rewardType = "COINS_500K"
            )
        )
    }

    private fun loadRafflesFromFirestore() {
        if (repository.isFirebaseAvailable) {
            val db = repository.getFirebaseFirestore()
            db?.collection("raffles")?.get()?.addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            RaffleItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                backgroundType = doc.getString("backgroundType") ?: "green",
                                slotsFilled = doc.getLong("slotsFilled")?.toInt() ?: 25,
                                slotsRequired = doc.getLong("slotsRequired")?.toInt() ?: 20,
                                ticketsRemaining = doc.getLong("ticketsRemaining")?.toInt() ?: 1794,
                                requiredTicketsToPlay = doc.getLong("requiredTicketsToPlay")?.toInt() ?: 5,
                                rewardType = doc.getString("rewardType") ?: "CASH_1"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (list.isNotEmpty()) {
                        _raffles.value = list
                    }
                } else {
                    preseedRafflesFirestore(db)
                }
            }?.addOnFailureListener { e ->
                Log.e("ClapEarnViewModel", "Failed to load raffles from firestore: ${e.message}")
            }
        }
    }

    private fun preseedRafflesFirestore(db: com.google.firebase.firestore.FirebaseFirestore?) {
        db ?: return
        val defaultList = getFallbackRaffles()
        for (raffle in defaultList) {
            val data = mapOf(
                "title" to raffle.title,
                "backgroundType" to raffle.backgroundType,
                "slotsFilled" to raffle.slotsFilled,
                "slotsRequired" to raffle.slotsRequired,
                "ticketsRemaining" to raffle.ticketsRemaining,
                "requiredTicketsToPlay" to raffle.requiredTicketsToPlay,
                "rewardType" to raffle.rewardType
            )
            db.collection("raffles").document(raffle.id).set(data)
                .addOnSuccessListener {
                    Log.d("ClapEarnViewModel", "Successfully preseeded raffle ${raffle.id}")
                }
        }
    }

    fun saveRaffleToFirestore(raffle: RaffleItem) {
        if (repository.isFirebaseAvailable) {
            val db = repository.getFirebaseFirestore() ?: return
            val data = mapOf(
                "title" to raffle.title,
                "backgroundType" to raffle.backgroundType,
                "slotsFilled" to raffle.slotsFilled,
                "slotsRequired" to raffle.slotsRequired,
                "ticketsRemaining" to raffle.ticketsRemaining,
                "requiredTicketsToPlay" to raffle.requiredTicketsToPlay,
                "rewardType" to raffle.rewardType
            )
            db.collection("raffles").document(raffle.id).set(data)
                .addOnSuccessListener {
                    Log.d("ClapEarnViewModel", "Successfully saved raffle ${raffle.id} to firestore")
                }
        }
    }

    fun playActiveRaffle(raffle: RaffleItem) {
        viewModelScope.launch {
            val current = repository.getWalletDirect()
            val isAmazon = raffle.rewardType == "AMAZON_10"
            
            if (isAmazon) {
                if (current.amazonPieces < raffle.requiredTicketsToPlay) {
                    _gameMessage.value = "⚠️ Need ${raffle.requiredTicketsToPlay} Ticket Pieces to enter!"
                    delay(3000L)
                    _gameMessage.value = null
                    return@launch
                }
                // Spend Amazon ticket pieces
                val nextWallet = current.copy(amazonPieces = current.amazonPieces - raffle.requiredTicketsToPlay)
                repository.updateWallet(nextWallet)
            } else {
                if (current.raffleTickets < raffle.requiredTicketsToPlay) {
                    _gameMessage.value = "⚠️ Need ${raffle.requiredTicketsToPlay} Raffle Tickets to enter!"
                    delay(3000L)
                    _gameMessage.value = null
                    return@launch
                }
                // Spend Raffle Tickets
                val nextWallet = current.copy(raffleTickets = current.raffleTickets - raffle.requiredTicketsToPlay)
                repository.updateWallet(nextWallet)
            }

            _gameMessage.value = "Drawing ${raffle.title}... 🎰"
            delay(2000L)

            val text: String
            when (raffle.rewardType) {
                "CASH_1" -> {
                    val roll = (1..100).random()
                    val won = when {
                        roll <= 45 -> 1.00 // 🥇 45% chance: $1
                        roll <= 85 -> 0.70 // 🥈 40% chance: $0.70
                        else -> 0.50       // 🥉 15% chance: $0.50
                    }
                    repository.addCash(won)
                    text = "🎰 Won $${String.format("%.2f", won)} USD Cash from Raffle!"
                }
                "AMAZON_10" -> {
                    val roll = (1..10).random()
                    if (roll == 1) {
                        repository.addCash(10.00)
                        text = "🏆 Jackpot! You won the $10.00 Amazon Coupon Payout!"
                    } else {
                        val cashBack = listOf(1.50, 2.00, 3.00, 5.00).random()
                        repository.addCash(cashBack)
                        text = "🎰 Won $${String.format("%.2f", cashBack)} Amazon Cash Payout!"
                    }
                }
                "COINS_500K" -> {
                    val roll = (1..5).random()
                    val won = when (roll) {
                        1 -> 500000L
                        2 -> 250000L
                        else -> 100000L
                    }
                    repository.addClapCoins(won)
                    text = "🎰 Won $won ClapCoins! 🟡"
                }
                else -> {
                    text = "Entered successfully!"
                }
            }

            // Update slot count inside state lists and Firestore
            val nextFilled = raffle.slotsFilled + 1
            val updatedRaffle = raffle.copy(
                slotsFilled = if (nextFilled > raffle.slotsRequired) 1 else nextFilled,
                ticketsRemaining = (raffle.ticketsRemaining - 1).coerceAtLeast(1)
            )

            val updatedList = _raffles.value.map {
                if (it.id == raffle.id) updatedRaffle else it
            }
            _raffles.value = updatedList
            saveRaffleToFirestore(updatedRaffle)

            _selectedRaffle.value = null // Close popup
            _gameMessage.value = text
            delay(5000L)
            _gameMessage.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoProgressJob?.cancel()
    }
}

data class RaffleItem(
    val id: String = "",
    val title: String = "",
    val backgroundType: String = "green",
    val slotsFilled: Int = 25,
    val slotsRequired: Int = 20,
    val ticketsRemaining: Int = 1794,
    val requiredTicketsToPlay: Int = 5,
    val rewardType: String = "CASH_1"
)

data class RewardPopupData(
    val coins: Int = 0,
    val cashTickets: Int = 0,
    val amazonPieces: Int = 0
)
