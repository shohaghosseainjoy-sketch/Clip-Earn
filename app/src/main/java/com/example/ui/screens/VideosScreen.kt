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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

    var showUploadDialog by remember { mutableStateOf(false) }

    // Setup video visible list auto-play integration with VerticalPager
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { videos.size }
    )

    // Observe pagerState.currentPage and trigger viewModel.selectVideo(currentPage)
    LaunchedEffect(pagerState.currentPage) {
        if (videos.isNotEmpty() && pagerState.currentPage in videos.indices) {
            viewModel.selectVideo(pagerState.currentPage)
        }
    }

    // Direct scroll handle if sync is requested from outer navigation
    LaunchedEffect(currentPlayingIndex) {
        if (videos.isNotEmpty() && currentPlayingIndex in videos.indices && pagerState.currentPage != currentPlayingIndex) {
            pagerState.scrollToPage(currentPlayingIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Immersive deep black background
    ) {
        // Vertical Pager holding full size content
        if (videos.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = videos.getOrNull(page)
                if (video != null) {
                    val isCurrent = page == currentPlayingIndex
                    VideoShortsPlayerCard(
                        video = video,
                        isPlaying = isCurrent && isPlaying,
                        onPlayClick = {
                            viewModel.toggleVideoPlay()
                        },
                        onClapClick = {
                            viewModel.clapVideo(page)
                        }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "কোনো ভিডিও পাওয়া যায়নি",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }
        }

        // FLOATING (+) UPLOAD OPTION
        FloatingActionButton(
            onClick = { showUploadDialog = true },
            containerColor = GoldCoins,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Upload video",
                modifier = Modifier.size(28.dp)
            )
        }

        // persistent REWARD PROGRESS BAR at the bottom of the screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xCC121212)) // Translucent cinematic dark background
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        color = Color.LightGray,
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Progress Track Line
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

                    // Milestone mini nodes for visual feedback
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        MilestoneNodeSmall(
                            bias = -0.333f,
                            isUnlocked = wallet.videoProgress >= 60f || wallet.claimedMin1,
                            icon = "🪵"
                        )
                        MilestoneNodeSmall(
                            bias = 0.333f,
                            isUnlocked = wallet.videoProgress >= 120f || wallet.claimedMin2,
                            icon = "🏆"
                        )
                        MilestoneNodeSmall(
                            bias = 1.0f,
                            isUnlocked = wallet.videoProgress >= 170f,
                            icon = "👑"
                        )
                    }
                }
            }
        }

        // Add Upload Modal Sheet Dialog logic
        if (showUploadDialog) {
            UploadVideoDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { title, creator, url ->
                    viewModel.uploadVideo(title, creator, url, "")
                    showUploadDialog = false
                }
            )
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
fun VideoShortsPlayerCard(
    video: VideoItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onClapClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    val avatarUrl = video.authorAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video playing container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onPlayClick() },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying && video.videoUrl.isNotEmpty()) {
                VideoPlayer(
                    videoUrl = video.videoUrl,
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(video.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Big play badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0x55000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 28.sp
                    )
                }
            }
        }

        // Bottom dark shadow overlay for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Bottom Left uploader meta & title details
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 90.dp, end = 90.dp) // padded beautifully
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .border(1.5.dp, GoldCoins, CircleShape)
                )

                Column {
                    Text(
                        text = "@${video.creator}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "সরাসরি • Shorts Feed",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                text = video.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Far-Right Action Icons Overlay column (TikTok layout)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp), // safe space above bottom navigation bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Clap/Like Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable { onClapClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("👏", fontSize = 21.sp)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = video.clapsCount.toString(),
                    color = if (video.isLiked) GoldCoins else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            // 2. Sound Control Widget (🔊 / 🔇)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMuted) "🔇" else "🔊",
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isMuted) "Unmute" else "Mute",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3. Share icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable { /* simulated share */ },
                    contentAlignment = Alignment.Center
                ) {
                    Text("↗", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "শেয়ার",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun androidx.compose.foundation.layout.BoxScope.MilestoneNodeSmall(
    bias: Float,
    isUnlocked: Boolean,
    icon: String
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .align(androidx.compose.ui.BiasAlignment(bias, 0f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isUnlocked) 22.dp else 18.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) {
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFFFF9800), Color(0xFFFFD700))
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E))
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUnlocked) Color(0xFFFFD700) else Color(0xFF4C4C4C),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = if (isUnlocked) 11.sp else 9.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadVideoDialog(
    onDismiss: () -> Unit,
    onUpload: (title: String, creator: String, url: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    val presets = listOf(
        Triple("কিউট বেড়াল 🐈 (Cat Gymnastics)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200"),
        Triple("মজার ওমলেট তৈরি 🍳 (Chef Lab)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "https://images.unsplash.com/photo-1498654896293-37aacf113fd9?w=200"),
        Triple("হাইওয়ে স্কেটিং 🛹 (Skater)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4", "https://images.unsplash.com/photo-1547447134-cd3f5c716030?w=200"),
        Triple("প্রশান্ত ড্রোন শট 🌊 (Ocean Stream)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=200")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন শর্টস ভিডিও আপলোড করুন 🎬",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        containerColor = Color(0xFF1A1A1A),
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "নিচে বিবরণ দিন অথবা একটি প্রিসেট লিঙ্ক সিলেক্ট করুন:",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ভিডিওর শিরোনাম (Title)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = GoldCoins,
                        focusedBorderColor = GoldCoins,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creator,
                    onValueChange = { creator = it },
                    label = { Text("আপলোডারের নাম (Creator)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = GoldCoins,
                        focusedBorderColor = GoldCoins,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("ভিডিও লিঙ্ক (MP4 URL)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = GoldCoins,
                        focusedBorderColor = GoldCoins,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "প্রিসেট ভিডিও শর্টস:",
                    color = GoldCoins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (url == preset.second) GoldCoins else Color(0xFF2C2C2C))
                                .clickable {
                                    url = preset.second
                                    if (title.isEmpty()) title = preset.first.substringBefore(" (")
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset.first.substringBefore(" "),
                                color = if (url == preset.second) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty() && url.isNotEmpty()) {
                        onUpload(title, creator, url)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldCoins, contentColor = Color.Black)
            ) {
                Text("আপলোড করুন 🚀", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = Color.Gray)
            }
        }
    )
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
