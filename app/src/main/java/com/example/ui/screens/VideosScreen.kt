package com.example.ui.screens

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
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
    val pendingChestType by viewModel.pendingChestType.collectAsState()
    val pendingChestTimeLeft by viewModel.pendingChestTimeLeft.collectAsState()

    var showUploadDialog by remember { mutableStateOf(false) }

    // ClipClaps customized tabs & menu details states
    var selectedCategory by remember { mutableStateOf("For You") }
    var isGlobalMuted by remember { mutableStateOf(true) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var selectedVideoForMenu by remember { mutableStateOf<VideoItem?>(null) }

    val filteredVideos = remember(videos, selectedCategory) {
        when (selectedCategory) {
            "Trending" -> videos.sortedByDescending { it.clapsCount }
            "Following" -> videos.filter { it.creator.lowercase() == "you" || it.creator.hashCode() % 2 == 0 }
            "New" -> videos.sortedByDescending { it.id }
            else -> videos // For You
        }
    }

    // Setup video visible list auto-play integration with VerticalPager
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { filteredVideos.size }
    )

    // Observe pagerState.currentPage and trigger viewModel.selectVideo(currentPage)
    LaunchedEffect(pagerState.currentPage, selectedCategory) {
        val video = filteredVideos.getOrNull(pagerState.currentPage)
        if (video != null) {
            val originalIndex = videos.indexOfFirst { it.id == video.id }
            if (originalIndex != -1) {
                viewModel.selectVideo(originalIndex)
            }
        }
    }

    // Direct scroll handle if sync is requested from outer navigation
    LaunchedEffect(currentPlayingIndex) {
        val video = videos.getOrNull(currentPlayingIndex)
        if (video != null) {
            val filteredIndex = filteredVideos.indexOfFirst { it.id == video.id }
            if (filteredIndex != -1 && pagerState.currentPage != filteredIndex) {
                pagerState.scrollToPage(filteredIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Immersive deep black background
    ) {
        // Vertical Pager holding full size content
        if (filteredVideos.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = filteredVideos.getOrNull(page)
                if (video != null) {
                    val isCurrent = page == pagerState.currentPage
                    VideoShortsPlayerCard(
                        video = video,
                        isPlaying = isCurrent && isPlaying,
                        onPlayClick = {
                            viewModel.toggleVideoPlay()
                        },
                        onClapClick = {
                            val originalIdx = videos.indexOfFirst { it.id == video.id }
                            if (originalIdx != -1) {
                                viewModel.clapVideo(originalIdx)
                            }
                        },
                        isMuted = isGlobalMuted,
                        onMuteToggle = { isGlobalMuted = !isGlobalMuted },
                        onShareClick = {
                            selectedVideoForMenu = video
                            showMenuSheet = true
                        },
                        onMenuClick = {
                            selectedVideoForMenu = video
                            showMenuSheet = true
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

        // --- VIDEO CATEGORIES FLOATING BELOW TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val categories = listOf("For You", "Trending", "Following", "New")
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Column(
                    modifier = Modifier
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color(0xFFFF9500) else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(28.dp)
                            .background(if (isSelected) Color(0xFFFF9500) else Color.Transparent)
                    )
                }
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
                .padding(top = 74.dp, end = 16.dp) // adjusted for overlaying nicely with category row
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
                    val remaining = 60f - wallet.videoProgress
                    remaining.coerceIn(0f, 60f).toInt()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Watch & Earn (1m Chest Cycle)",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (pendingChestType != null) "🚨 TAP THE CHEST TO CLAIM!" else "Next Chest in ${secondsLeft}s",
                        color = if (pendingChestType != null) Color(0xFFE74C3C) else GoldCoins,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Progress Track Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2C2C2C))
                    ) {
                        val progressPct = (wallet.videoProgress / 60f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPct)
                                .background(Color(0xFFF5A623)) // orange (#F5A623) fill
                        )
                    }

                    // Milestone Custom Interactive Nodes for Chests
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        // CHEST 1 — Wooden Chest at 20 second mark (bias = -0.333f)
                        TimelineChestNode(
                            bias = -0.333f,
                            type = "wooden",
                            isUnlocked = wallet.videoProgress >= 20f || wallet.claimedMin1,
                            isPending = pendingChestType?.lowercase() == "wooden",
                            countdownSeconds = pendingChestTimeLeft,
                            onTap = { viewModel.claimPendingChest() }
                        )

                        // CHEST 2 — Golden Chest at 40 second mark (bias = 0.333f)
                        TimelineChestNode(
                            bias = 0.333f,
                            type = "golden",
                            isUnlocked = wallet.videoProgress >= 40f || wallet.claimedMin2,
                            isPending = pendingChestType?.lowercase() == "golden",
                            countdownSeconds = pendingChestTimeLeft,
                            onTap = { viewModel.claimPendingChest() }
                        )

                        // CHEST 3 — Luxury/Diamond Chest at 60 second mark (bias = 1.0f)
                        TimelineChestNode(
                            bias = 1.0f,
                            type = "luxury",
                            isUnlocked = wallet.videoProgress >= 59.5f,
                            isPending = pendingChestType?.lowercase() == "luxury",
                            countdownSeconds = pendingChestTimeLeft,
                            onTap = { viewModel.claimPendingChest() }
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

        // --- CUSTOM CLIPCLAPS MENU BOTTOM SHEET ---
        if (showMenuSheet && selectedVideoForMenu != null) {
            val video = selectedVideoForMenu!!
            val context = LocalContext.current
            
            // Dim overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showMenuSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable(enabled = false) { }
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.DarkGray)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Options for @${video.creator}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    HorizontalDivider(color = Color(0xFF2C2C2C), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Option 1: Not Interested
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.widget.Toast.makeText(context, "We will recommend fewer videos like this!", android.widget.Toast.LENGTH_SHORT).show()
                                showMenuSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚫", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Not interested", color = Color.White, fontSize = 14.sp)
                    }

                    // Option 2: Report Video
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.widget.Toast.makeText(context, "Thank you! We've received your report.", android.widget.Toast.LENGTH_SHORT).show()
                                showMenuSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📢", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Report video", color = Color.White, fontSize = 14.sp)
                    }

                    // Option 3: Copy Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("video_link", video.videoUrl)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "Link copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                showMenuSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔗", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Copy link", color = Color.White, fontSize = 14.sp)
                    }

                    // Option 4: Follow Creator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                android.widget.Toast.makeText(context, "You are now following @${video.creator}!", android.widget.Toast.LENGTH_SHORT).show()
                                showMenuSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("➕", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Follow @${video.creator}", color = Color.White, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun VideoShortsPlayerCard(
    video: VideoItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onClapClick: () -> Unit,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onShareClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val avatarUrl = video.authorAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Upper section: The full width video player container with NO rounded corners
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
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

            // Top-right semi-transparent mute controller
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp) // Clears headers nicely
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onMuteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMuted) "🔇" else "🔊",
                    fontSize = 18.sp
                )
            }
        }

        // Below section: Crispy high-contrast white card containing creator uploader row + caption
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .padding(bottom = 98.dp) // Padded to leave clear space for reward indicator
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left profile row: Avatar circle (40dp) + username (bold, 14sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )

                    Text(
                        text = "@${video.creator}",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Right actions row: 👏 Clap icon + clap count (with animation) + share (↗) + ••• menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Clap Trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClapClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👏", fontSize = 21.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        AnimatedContent(
                            targetState = video.clapsCount,
                            transitionSpec = {
                                slideInVertically { height -> height } + fadeIn() togetherWith
                                        slideOutVertically { height -> -height } + fadeOut()
                            },
                            label = "clapsCounter"
                        ) { claps ->
                            Text(
                                text = claps.toString(),
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Share ↗ action
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onShareClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↗", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    // •• Menu button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•••", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Caption/title text below user row (gray, 13sp)
            Text(
                text = video.title,
                color = TextGrey,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BoxScope.TimelineChestNode(
    bias: Float,
    type: String, // "wooden", "golden", "luxury"
    isUnlocked: Boolean,
    isPending: Boolean,
    countdownSeconds: Int,
    onTap: () -> Unit
) {
    // Collect animation values if pending
    val scale = if (isPending) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        animatedScale
    } else {
        1.0f
    }

    val chestColor = when (type) {
        "wooden" -> Color(0xFF8B4513) // Brown `#8B4513`
        "golden" -> Color(0xFFFFD700) // Gold `#FFD700`
        "luxury" -> Color(0xFF9B59B6) // Purple `#9B59B6`
        else -> Color.Gray
    }

    val chestIcon = when (type) {
        "wooden" -> "🪵"
        "golden" -> "🏆"
        "luxury" -> "👑"
        else -> "📦"
    }

    val labelText = when (type) {
        "wooden" -> "WOODEN"
        "golden" -> "GOLDEN"
        "luxury" -> "PREMIUM"
        else -> ""
    }

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
            // Tap hint bubble
            Box(modifier = Modifier.height(18.dp)) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isPending,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .background(Color(0xFFE74C3C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "TAP! ${countdownSeconds}s",
                            color = Color.White,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(if (isPending) 32.dp else 24.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    )
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked || isPending) {
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(chestColor, Color.Black)
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF222222), Color(0xFF111111))
                            )
                        }
                    )
                    .border(
                        width = if (isPending) 2.dp else 1.dp,
                        color = if (isPending) Color.White else if (isUnlocked) chestColor else Color(0xFF444444),
                        shape = CircleShape
                    )
                    .clickable(enabled = isPending) {
                        onTap()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chestIcon,
                    fontSize = if (isPending) 15.sp else 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = labelText,
                color = if (isUnlocked || isPending) chestColor else Color.Gray,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold
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
