package com.example.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.ClapEarnViewModel
import com.example.ui.RewardPopupData
import com.example.ui.models.VideoItem
import com.example.ui.theme.GoldCoins
import com.example.ui.theme.TextGrey
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    viewModel: ClapEarnViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val currentPlayingIndex by viewModel.currentPlayingIndex.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()
    val completedPopup by viewModel.completedRewardPopup.collectAsState()

    // Setup video visible list auto-play integration
    val listState = rememberLazyListState()
    // Observe visible items log and first visible settling
    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(firstVisibleIndex) {
        if (videos.isNotEmpty() && firstVisibleIndex in videos.indices) {
            viewModel.selectVideo(firstVisibleIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Pure cinematic dark background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 90.dp) // space for persistent progress bar
            ) {
                itemsIndexed(videos) { index, video ->
                    val isCurrent = index == currentPlayingIndex
                    VideoFeedCard(
                        video = video,
                        isPlaying = isCurrent && isPlaying,
                        onPlayClick = {
                            viewModel.selectVideo(index)
                        },
                        onClapClick = {
                            viewModel.clapVideo(index)
                        }
                    )
                }
            }
        }

        // persistent REWARD PROGRESS BAR at the very bottom of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xF2161616))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Countdown timer showing seconds left for 180s cycle
                val secondsLeft = remember(wallet.videoProgress) {
                    val remaining = 180f - wallet.videoProgress
                    remaining.coerceIn(0f, 180f).toInt()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Watch & Earn (3m Cycle)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Next Chest in ${secondsLeft}s",
                        color = GoldCoins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom premium timeline progress bar with milestone nodes
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 1. Progress track inside the middle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2C2C2C))
                    ) {
                        val progressPct = (wallet.videoProgress / 180f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPct)
                                .background(
                                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF9800), Color(0xFFFFD700))
                                    )
                                )
                        )
                    }

                    // 2. Interactive Premium Milestone nodes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        // Minute 1 Chest node (Wooden Style) at 1/3 (60s) -> bias = -0.333f
                        MilestoneNode(
                            bias = -0.333f,
                            isUnlocked = wallet.videoProgress >= 60f || wallet.claimedMin1,
                            title = "1 Min",
                            themeColor = Color(0xFF8B4513),
                            borderColor = Color(0xFFD2B48C),
                            icon = "🪵📦"
                        )

                        // Minute 2 Chest node (Golden Style) at 2/3 (120s) -> bias = 0.333f
                        MilestoneNode(
                            bias = 0.333f,
                            isUnlocked = wallet.videoProgress >= 120f || wallet.claimedMin2,
                            title = "2 Min",
                            themeColor = Color(0xFFB58900),
                            borderColor = Color(0xFFFFD700),
                            icon = "🏆🎁"
                        )

                        // Minute 3 Chest node (Premium Luxury style) at 3/3 (180s) -> bias = 1.0f
                        MilestoneNode(
                            bias = 1.0f,
                            isUnlocked = wallet.videoProgress >= 170f, // active when completing
                            title = "3 Min",
                            themeColor = Color(0xFF0F4C81),
                            borderColor = Color(0xFF85C1E9),
                            icon = "👑💎"
                        )
                    }
                }
            }
        }

        // Animated Rewards Opening Claim Overlay
        completedPopup?.let { reward ->
            RewardPopup(
                reward = reward,
                onClose = { viewModel.closeRewardPopup() }
            )
        }
    }
}

@Composable
fun VideoFeedCard(
    video: VideoItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onClapClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .background(Color.Black)
    ) {
        // Video player container at top (16:9 ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (isPlaying && video.videoUrl.isNotEmpty()) {
                VideoPlayer(
                    videoUrl = video.videoUrl,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Static high-fidelity thumbnail with Play Button
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Large play trigger overlay button
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x99000000), CircleShape)
                        .align(Alignment.Center)
                ) {
                    Text(
                        text = "▶",
                        color = GoldCoins,
                        fontSize = 24.sp
                    )
                }
            }

            // Mute/unmute button at TOP RIGHT corner of video player (🔇/🔊 icon in dark rounded box)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x99000000))
                    .clickable { isMuted = !isMuted }
                    .padding(8.dp)
            ) {
                Text(
                    text = if (isMuted) "🔇" else "🔊",
                    fontSize = 14.sp
                )
            }

            // Duration badge at Bottom-Right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = video.durationString,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }

        // Row of Uploader metadata & interactions below player
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Uploader avatar (small circle, left side)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.authorAvatar)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Username text
            Text(
                text = video.creator,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Claps count (👏 icon + number) on RIGHT side with custom highlight when liked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF222222))
                    .clickable { onClapClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("👏", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = video.clapsCount.toString(),
                    color = if (video.isLiked) GoldCoins else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Share button (↗ arrow icon) next to claps count
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF222222))
                    .clickable { /* Simulate share action */ }
                    .padding(8.dp)
            ) {
                Text(
                    text = "↗",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Three dots menu (...) far right
            Text(
                text = "•••",
                color = Color.Gray,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { /* menu options simulation */ }
                    .padding(4.dp)
            )
        }

        // Video title/caption text below the uploader row
        Text(
            text = video.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(context, videoUrl) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            playWhenReady = isPlaying
            repeatMode = Player.REPEAT_MODE_ALL
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            volume = if (isMuted) 0f else 1f
        }
        player = exoPlayer

        onDispose {
            exoPlayer.release()
            player = null
        }
    }

    LaunchedEffect(isPlaying) {
        player?.let { p ->
            p.playWhenReady = isPlaying
            if (isPlaying) {
                p.play()
            } else {
                p.pause()
            }
        }
    }

    LaunchedEffect(isMuted) {
        player?.volume = if (isMuted) 0f else 1f
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                player = player
            }
        },
        update = { playerView ->
            playerView.player = player
        },
        modifier = modifier
    )
}

@Composable
fun RewardPopup(
    reward: RewardPopupData,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            // Treasure chest animation opening state simulation at top
            var isChestOpen by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(400)
                isChestOpen = true
            }

            Text(
                text = if (isChestOpen) "🔓🎁✨" else "🔒🎁",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "You have obtained" text
            Text(
                text = "You have obtained:",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Cards design representation
            // Yellow card for coins
            if (reward.coins > 0) {
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1C40F))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🟡 COINS",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "🪙",
                            fontSize = 90.sp
                        )
                        Text(
                            text = "+${reward.coins} Coins",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Green card for cash tickets
            if (reward.cashTickets > 0) {
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2ECC71))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💚 CASH TOKENS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "💵",
                            fontSize = 90.sp
                        )
                        Text(
                            text = "+${reward.cashTickets} Tokens",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Black card for Amazon
            if (reward.amazonPieces > 0) {
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .height(280.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🛍️ AMAZON PIECES",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "💎",
                            fontSize = 90.sp
                        )
                        Text(
                            text = "+${reward.amazonPieces} Pieces",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // TIP text at bottom: "Green tokens can be exchanged for cash raffle tickets"
            Text(
                text = "Green tokens can be exchanged for cash raffle tickets",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tap anywhere to close",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.MilestoneNode(
    bias: Float,
    isUnlocked: Boolean,
    title: String,
    themeColor: Color,
    borderColor: Color,
    icon: String
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .align(androidx.compose.ui.BiasAlignment(bias, 0f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (isUnlocked) 40.dp else 32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) {
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(themeColor, themeColor.copy(alpha = 0.5f))
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E))
                            )
                        }
                    )
                    .then(
                        Modifier.border(
                            width = 1.5.dp,
                            color = if (isUnlocked) borderColor else Color(0xFF4C4C4C),
                            shape = CircleShape
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = if (isUnlocked) 20.sp else 14.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = if (isUnlocked) Color.White else Color.Gray,
                fontSize = 8.sp,
                fontWeight = if (isUnlocked) FontWeight.Black else FontWeight.Normal
            )
        }
    }
}
