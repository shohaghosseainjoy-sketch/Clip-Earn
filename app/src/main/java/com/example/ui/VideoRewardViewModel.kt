package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Vibrator
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ClapEarnRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoRewardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClapEarnRepository(application)

    private val _progressSeconds = MutableStateFlow(0)
    val progressSeconds: StateFlow<Int> = _progressSeconds.asStateFlow()

    private val _chest1Collected = MutableStateFlow(false)
    val chest1Collected: StateFlow<Boolean> = _chest1Collected.asStateFlow()

    private val _chest2Collected = MutableStateFlow(false)
    val chest2Collected: StateFlow<Boolean> = _chest2Collected.asStateFlow()

    private val _chest3Collected = MutableStateFlow(false)
    val chest3Collected: StateFlow<Boolean> = _chest3Collected.asStateFlow()

    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()

    private var timerJob: Job? = null
    
    private var hapticWoodenTriggered = false
    private var hapticGoldenTriggered = false
    private var hapticPremiumTriggered = false

    init {
        // Observe play state to start or stop incrementing progress coroutine
        viewModelScope.launch {
            _isVideoPlaying.collect { playing ->
                if (playing) {
                    startTimer()
                } else {
                    stopTimer()
                }
            }
        }
    }

    fun setVideoPlaying(playing: Boolean) {
        _isVideoPlaying.value = playing
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (_isVideoPlaying.value) {
                delay(1000L)
                val current = _progressSeconds.value
                if (current < 60) {
                    val next = current + 1
                    _progressSeconds.value = next
                    checkAndTriggerHaptic(next)
                } else {
                    // Reset cycle immediately when 60 seconds completes
                    resetCycle()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun checkAndTriggerHaptic(seconds: Int) {
        if (seconds >= 20 && !hapticWoodenTriggered) {
            hapticWoodenTriggered = true
            triggerVibration()
        }
        if (seconds >= 40 && !hapticGoldenTriggered) {
            hapticGoldenTriggered = true
            triggerVibration()
        }
        if (seconds >= 60 && !hapticPremiumTriggered) {
            hapticPremiumTriggered = true
            triggerVibration()
        }
    }

    private fun triggerVibration() {
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(50)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetCycle() {
        _progressSeconds.value = 0
        _chest1Collected.value = false
        _chest2Collected.value = false
        _chest3Collected.value = false
        hapticWoodenTriggered = false
        hapticGoldenTriggered = false
        hapticPremiumTriggered = false
    }

    fun tapChest(chestType: String, currentVideoId: String) {
        viewModelScope.launch {
            val typeStr = when (chestType.lowercase()) {
                "wooden", "wood" -> "wood"
                "golden", "gold" -> "gold"
                "luxury", "premium", "diamond" -> "diamond"
                else -> chestType.lowercase()
            }
            
            // Avoid duplicate collection
            when (chestType.lowercase()) {
                "wooden", "wood" -> {
                    if (_chest1Collected.value) return@launch
                    _chest1Collected.value = true
                }
                "golden", "gold" -> {
                    if (_chest2Collected.value) return@launch
                    _chest2Collected.value = true
                }
                "luxury", "premium", "diamond" -> {
                    if (_chest3Collected.value) return@launch
                    _chest3Collected.value = true
                }
            }

            // Save to database/Firestore via Repository
            repository.claimVideoChest(typeStr, currentVideoId)

            // Dynamic Toast (matches description exactly: "Wooden Chest saved to Rewards! 🎁")
            val toastName = when (chestType.lowercase()) {
                "wooden", "wood" -> "Wooden Chest"
                "golden", "gold" -> "Golden Chest"
                "luxury", "premium", "diamond" -> "Diamond Chest"
                else -> "Chest"
            }
            Toast.makeText(getApplication(), "$toastName saved to Rewards! 🎁", Toast.LENGTH_SHORT).show()
        }
    }
}

class VideoRewardViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoRewardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoRewardViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
