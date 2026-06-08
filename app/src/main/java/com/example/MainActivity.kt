package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
                onAddCoinsClick = {
                    // Click [+] gives a quick demo bonus of 1000 ClapCoins & 2 tickets!
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
    onAddCoinsClick: () -> Unit
) {
    // Exact specification layout:
    // Left side: 🟡 [coin count] [+] 🎰 [raffle ticket count]
    // Right side: 💚 [0.00 USD]
    // Black background, white/colored text
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(ClipClapBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Coins and Tickets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Chip: #F5A623 background
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldCoins)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "🟡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = coinCount.toString(),
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )

                    // Plus [+] button on Chip
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onAddCoinsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Quick Coins",
                            tint = GoldCoins,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Raffle slot count chip: Black/Dark with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF222222))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "🎰", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$ticketCount Tickets",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Right side: Cash Chip Green (#2ECC71)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CashGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "💚", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$${String.format("%.2f", cashUsd)} USD",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
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
