package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ClapEarnViewModel
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.MeScreen
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ClapEarnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ClapEarnAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClapEarnAppContent(viewModel: ClapEarnViewModel) {
    val navController = rememberNavController()
    val wallet by viewModel.wallet.collectAsState()
    
    var currentRoute by remember { mutableStateOf("videos") }

    // Track active navigation routing
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentRoute = destination.route ?: "videos"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ClapEarnTopBar(
                coinCount = wallet.clapCoins,
                ticketCount = wallet.raffleTickets,
                cashUsd = wallet.cashUsd,
                unclaimedChests = wallet.unclaimedChests,
                woodenChests = wallet.woodenChests,
                goldenChests = wallet.goldenChests,
                luxuryChests = wallet.luxuryChests,
                onAddCoinsClick = {
                    navController.navigate("games") {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    viewModel.grantQuickCoinsBonus()
                }
            )
        },
        bottomBar = {
            ClapEarnBottomBar(
                currentRoute = currentRoute,
                unclaimedChestsBadgeCount = wallet.unclaimedChests,
                onNavClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "videos",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("videos") {
                VideosScreen(viewModel = viewModel)
            }
            composable("games") {
                GamesScreen(viewModel = viewModel)
            }
            composable("rewards") {
                RewardsScreen(viewModel = viewModel)
            }
            composable("me") {
                MeScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ClapEarnTopBar(
    coinCount: Long,
    ticketCount: Int,
    cashUsd: Double,
    unclaimedChests: Int = 0,
    woodenChests: Int = 0,
    goldenChests: Int = 0,
    luxuryChests: Int = 0,
    onAddCoinsClick: () -> Unit
) {
    // Collect/remember counts to check if a chest/ticket has been earned
    var lastTicketCount by remember { mutableStateOf(ticketCount) }
    var lastChestCount by remember { mutableStateOf(unclaimedChests + woodenChests + goldenChests + luxuryChests) }
    
    val shakeAnimatable = remember { Animatable(0f) }
    
    LaunchedEffect(ticketCount, unclaimedChests, woodenChests, goldenChests, luxuryChests) {
        val currentChestCount = unclaimedChests + woodenChests + goldenChests + luxuryChests
        if (ticketCount > lastTicketCount || currentChestCount > lastChestCount) {
            // Trigger beautiful shake animation
            for (i in 0..3) {
                shakeAnimatable.animateTo(12f, animationSpec = tween(50, easing = LinearEasing))
                shakeAnimatable.animateTo(-12f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeAnimatable.animateTo(0f, animationSpec = tween(50, easing = LinearEasing))
        }
        lastTicketCount = ticketCount
        lastChestCount = currentChestCount
    }

    Surface(
        color = Color(0xFF000000), // Pure Black Background
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Coin count badge, plus button, poker-chip style ticket count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 🟡 Coin icon (orange circle) + coin count number (white text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9500)), // Orange circle background
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🪙", fontSize = 13.sp)
                    }

                    // Count Flip Animation
                    AnimatedContent(
                        targetState = coinCount,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                    slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "coinCountAnimation"
                    ) { count ->
                        Text(
                            text = count.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 2. [+] plus button (tap = go to earn more screen)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9500))
                        .clickable { onAddCoinsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Earn More",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 3. 🎰 Raffle ticket icon (poker chip style) + ticket count number [SHAKING]
                Row(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = shakeAnimatable.value
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Poker chip style ticket icon (Double-ring blue/red style)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE74C3C)), // Beautiful red chip outer circle
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent)
                                .border(1.2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎟️", fontSize = 10.sp)
                        }
                    }

                    // Ticket count number (white text)
                    Text(
                        text = ticketCount.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Right side: 💚 Dollar icon (green) + "0.00 USD" text (white)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2ECC71)), // Green Dollar circle
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = String.format("$%.2f USD", cashUsd),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ClapEarnBottomBar(
    currentRoute: String,
    unclaimedChestsBadgeCount: Int,
    onNavClick: (String) -> Unit
) {
    // 4 tabs only: Videos, Games, Rewards, Me
    // Background: #FFFFFF with black icons
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Black,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Tab 1: Videos
        NavigationBarItem(
            selected = currentRoute == "videos",
            onClick = { onNavClick("videos") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "videos") Icons.Filled.PlayArrow else Icons.Outlined.PlayCircleOutline,
                    contentDescription = "Videos",
                    tint = Color.Black
                )
            },
            label = { Text("Videos", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GoldCoins.copy(alpha = 0.2f)
            )
        )

        // Tab 2: Games
        NavigationBarItem(
            selected = currentRoute == "games",
            onClick = { onNavClick("games") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "games") Icons.Filled.Casino else Icons.Outlined.Casino,
                    contentDescription = "Games",
                    tint = Color.Black
                )
            },
            label = { Text("Games", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GoldCoins.copy(alpha = 0.2f)
            )
        )

        // Tab 3: Rewards (with red badge showing count)
        NavigationBarItem(
            selected = currentRoute == "rewards",
            onClick = { onNavClick("rewards") },
            icon = {
                BadgedBox(
                    badge = {
                        if (unclaimedChestsBadgeCount > 0) {
                            Badge(containerColor = Color.Red) {
                                Text(
                                    text = unclaimedChestsBadgeCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentRoute == "rewards") Icons.Filled.CardGiftcard else Icons.Outlined.CardGiftcard,
                        contentDescription = "Rewards",
                        tint = Color.Black
                    )
                }
            },
            label = { Text("Rewards", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GoldCoins.copy(alpha = 0.2f)
            )
        )

        // Tab 4: Me
        NavigationBarItem(
            selected = currentRoute == "me",
            onClick = { onNavClick("me") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "me") Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Me",
                    tint = Color.Black
                )
            },
            label = { Text("Me", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = GoldCoins.copy(alpha = 0.2f)
            )
        )
    }
}
