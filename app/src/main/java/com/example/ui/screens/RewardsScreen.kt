package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClapEarnViewModel
import com.example.ui.RaffleItem
import kotlinx.coroutines.delay

// Colors
private val OrangeClaps = Color(0xFFFF9800)
private val LightGold = Color(0xFFF1C40F)
private val CashGreen = Color(0xFF2ECC71)
private val DarkSlate = Color(0xFF1E1E1E)
private val BgGrey = Color(0xFFF7F8FA)
private val BorderGrey = Color(0xFFE5E5E5)
private val TextDark = Color(0xFF2C3E50)

@Composable
fun RewardsScreen(
    viewModel: ClapEarnViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val raffles by viewModel.raffles.collectAsState()
    val selectedRaffle by viewModel.selectedRaffle.collectAsState()
    val latestPiecesWon by viewModel.latestPiecesWon.collectAsState()
    val chestRewardResult by viewModel.chestRewardResult.collectAsState()
    val gameMessage by viewModel.gameMessage.collectAsState()

    var showRedeemDialog by remember { mutableStateOf(false) }
    var redeemCodeText by remember { mutableStateOf("") }
    var redeemStatusMessage by remember { mutableStateOf<String?>(null) }
    var isRedeeming by remember { mutableStateOf(false) }

    // Pieces popup trigger
    var showPiecesDetailPopup by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgGrey)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- TOP BAR ----
            TopBarAndWallet(
                coinBalance = wallet.clapCoins,
                cashBalance = wallet.cashUsd,
                onRedeemClick = {
                    redeemCodeText = ""
                    redeemStatusMessage = null
                    showRedeemDialog = true
                }
            )

            // ---- WINNER ANNOUNCEMENT MARQUEE BANNER ----
            WinnerMarqueeBanner()

            // Main Content Scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- "MY CHESTS" Section ----
                val totalChestsAvailable = wallet.woodenChests + wallet.goldenChests + wallet.luxuryChests
                if (totalChestsAvailable > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💼 MY CHESTS",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            if (wallet.woodenChests >= 3) {
                                Button(
                                    onClick = { viewModel.openAllWooden() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5A623)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Open All Wooden", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        }
                        
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (wallet.woodenChests > 0) {
                                item {
                                    MyChestCard(
                                        type = "wooden",
                                        count = wallet.woodenChests,
                                        onOpen = { viewModel.openRewardChest("wooden") }
                                    )
                                }
                            }
                            if (wallet.goldenChests > 0) {
                                item {
                                    MyChestCard(
                                        type = "golden",
                                        count = wallet.goldenChests,
                                        onOpen = { viewModel.openRewardChest("golden") }
                                    )
                                }
                            }
                            if (wallet.luxuryChests > 0) {
                                item {
                                    MyChestCard(
                                        type = "luxury",
                                        count = wallet.luxuryChests,
                                        onOpen = { viewModel.openRewardChest("luxury") }
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- REWARD CHESTS PANEL ----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎁 CHOOSE CHEST TO OPEN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val totalUnopened = wallet.woodenChests + wallet.goldenChests + wallet.luxuryChests + wallet.diamondChests

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Wooden Chest
                            ChestItemView(
                                icon = "🪵📦",
                                name = "Wooden",
                                description = "5-10 🪙",
                                availableCount = wallet.woodenChests,
                                isActive = wallet.woodenChests > 0,
                                onOpen = { viewModel.openRewardChest("wooden") },
                                modifier = Modifier.weight(1f)
                            )

                            // Golden Chest
                            ChestItemView(
                                icon = "🏆🎁",
                                name = "Golden",
                                description = "10-20 🪙",
                                availableCount = wallet.goldenChests,
                                isActive = wallet.goldenChests > 0,
                                onOpen = { viewModel.openRewardChest("golden") },
                                modifier = Modifier.weight(1f)
                            )

                            // Luxury Chest
                            ChestItemView(
                                icon = "👑💎",
                                name = "Luxury",
                                description = "20-50 🪙",
                                availableCount = wallet.luxuryChests,
                                isActive = wallet.luxuryChests > 0,
                                onOpen = { viewModel.openRewardChest("luxury") },
                                modifier = Modifier.weight(1f)
                            )

                            // Diamond Chest
                            ChestItemView(
                                icon = "✨💎",
                                name = "Diamond",
                                description = "Special",
                                availableCount = wallet.diamondChests,
                                isActive = wallet.diamondChests > 0,
                                onOpen = { viewModel.openDiamondChest { } },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (totalUnopened > 0) "🎉 TAP ANY UNLOCKED CHEST TO OPEN!" else "Watch video feeds to earn more treasure chests!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalUnopened > 0) OrangeClaps else Color.DarkGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "In stock: $totalUnopened Unopened Chests",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // ---- AMAZON PIECES ENTRY BUTTON/BANNER ----
                AmazonPiecesBanner(
                    currentPieces = wallet.amazonPieces,
                    onViewClick = { showPiecesDetailPopup = true }
                )

                // ---- RAFFLE DRAWINGS ROW HEADERS ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎰 LIVE REWARD RAFFLES",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, BorderGrey, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🎟️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${wallet.raffleTickets} Tickets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                // ---- RAFFLE CARDS LIST ----
                if (raffles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OrangeClaps)
                    }
                } else {
                    raffles.forEach { raffle ->
                        RaffleCard(
                            raffle = raffle,
                            userTickets = wallet.raffleTickets,
                            userPieces = wallet.amazonPieces,
                            onCardTap = { viewModel.selectRaffle(raffle) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // ---- LIVE GLOBAL SUCCESS/ERROR NOTIFICATIONS ----
        AnimatedVisibility(
            visible = gameMessage != null,
            enter = slideInVertically(animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            gameMessage?.let { msg ->
                val cardColor = if (msg.contains("Won") || msg.contains("win") || msg.contains("Result")) {
                    Color(0xFFE8F8F5)
                } else {
                    Color(0xFFFEF9E7)
                }
                val borderColor = if (msg.contains("Won") || msg.contains("win") || msg.contains("Result")) {
                    CashGreen
                } else {
                    OrangeClaps
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎁",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = msg,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---- RAFFLE DETAIL POPUP DETAIL VIEW ----
        selectedRaffle?.let { raffle ->
            RaffleDetailPopup(
                raffle = raffle,
                userTickets = wallet.raffleTickets,
                userPieces = wallet.amazonPieces,
                onClose = { viewModel.selectRaffle(null) },
                onPlay = { viewModel.playActiveRaffle(raffle) }
            )
        }

        // ---- RAFFLE TICKET PIECES POPUP VIEW ----
        if (showPiecesDetailPopup || latestPiecesWon != null) {
            val piecesWon = latestPiecesWon ?: 0
            RaffleTicketPiecesPopup(
                currentPieces = wallet.amazonPieces,
                piecesWon = piecesWon,
                unclaimedChests = wallet.unclaimedChests,
                onClose = {
                    showPiecesDetailPopup = false
                    viewModel.clearLatestPiecesWon()
                }
            )
        }

        // ---- REWARD CHEST OPENING Claim Overlay ----
        chestRewardResult?.let { result ->
            val coins = result["coins"] as? Long ?: 0L
            val tickets = result["tickets"] as? Int ?: 0
            val cash = result["cash"] as? Double ?: 0.0
            val pieces = result["amazonPieces"] as? Int ?: 0
            val prizeType = result["prizeType"] as? String
            val prizeValue = result["prizeValue"] as? String
            val chestType = result["chestType"] as? String

            ChestResultClaimOverlay(
                chestType = chestType,
                coins = coins,
                tickets = tickets,
                cash = cash,
                pieces = pieces,
                prizeType = prizeType,
                prizeValue = prizeValue,
                onClaim = {
                    viewModel.clearChestRewardResult()
                }
            )
        }

        // ---- REDEEM REFERRAL CODE DIALOG ----
        if (showRedeemDialog) {
            AlertDialog(
                onDismissRequest = { showRedeemDialog = false },
                title = {
                    Text(
                        text = "Redeem Referral Code",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Claim instant $1.00 USD cash reward by entering an 8-character referral coupon code! (Try FREE7788 script / simulated custom uploader codes)",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = redeemCodeText,
                            onValueChange = { redeemCodeText = it.take(12) },
                            placeholder = { Text("Enter Referral Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                        )

                        redeemStatusMessage?.let { status ->
                            Text(
                                text = status,
                                color = if (status.contains("Success")) CashGreen else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (redeemCodeText.isBlank()) return@Button
                            isRedeeming = true
                            viewModel.redeemCode(redeemCodeText) { result ->
                                isRedeeming = false
                                redeemStatusMessage = result
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeClaps),
                        enabled = !isRedeeming
                    ) {
                        Text("Redeem")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRedeemDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

// ----------------------------------------------------
// SUBCOMPONENTS
// ----------------------------------------------------

@Composable
fun TopBarAndWallet(
    coinBalance: Long,
    cashBalance: Double,
    onRedeemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance row with visual spacers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Coins
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("🟡", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%,d", coinBalance),
                    color = LightGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Cash
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("💚", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("$%.2f USD", cashBalance),
                    color = CashGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        // Redeem button
        Button(
            onClick = onRedeemClick,
            colors = ButtonDefaults.buttonColors(containerColor = OrangeClaps),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = "Redeem",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun WinnerMarqueeBanner() {
    val announcements = remember {
        listOf(
            "Michael_88 won $1.00 USD from $1.00 Cash raffle! 🎉",
            "Clapper_3429 won $10.00 cash coupon from $10 Amazon Raffle! 🛍️",
            "SuperDina won 500,000 Coins from 500k ClapCoins raffle! 🪙",
            "Sonia_T won $1.00 USD from $1.00 Cash raffle! ✨",
            "Keanu_R won $10.00 cash coupon from $10 Amazon Raffle! 💚"
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    var translationX by remember { mutableStateOf(350f) }

    LaunchedEffect(currentIndex) {
        translationX = 350f
        while (translationX > -350f) {
            delay(16L) // ~60fps smooth loop ticker update
            translationX -= 2.0f
        }
        currentIndex = (currentIndex + 1) % announcements.size
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(Color(0xFFFFB300)) // amber gold strip
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Notification alert",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = announcements[currentIndex],
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.offset(x = translationX.dp)
            )
        }
    }
}

@Composable
fun ChestItemView(
    icon: String,
    name: String,
    description: String,
    availableCount: Int,
    isActive: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.82f)
            .clickable(enabled = isActive, onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isActive) OrangeClaps.copy(alpha = 0.3f) else BorderGrey),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Large Emoticon Icon
                Text(
                    text = icon,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = if (isActive) Color.Unspecified else Color.Gray
                )

                // OPEN status label
                Text(
                    text = if (isActive) "OPEN" else "LOCKED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) OrangeClaps else Color.LightGray
                )

                // Sub title name
                Text(
                    text = name,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Circular Red Badge count
            if (isActive && availableCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = availableCount.toString(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun AmazonPiecesBanner(
    currentPieces: Int,
    onViewClick: () -> Unit
) {
    val progress = (currentPieces.toFloat() / 120f).coerceIn(0.0f, 1.0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onViewClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side representation
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("🛍️", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Amazon Ticket Pieces",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(
                    text = "Progress to enter $10 Coupon: $currentPieces/120 Pieces",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF333333))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF9800), LightGold)))
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onViewClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "VIEW",
                    color = DarkSlate,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun RaffleCard(
    raffle: RaffleItem,
    userTickets: Int,
    userPieces: Int,
    onCardTap: () -> Unit
) {
    val isAmazon = raffle.rewardType == "AMAZON_10"
    val userBalance = if (isAmazon) userPieces else userTickets
    val progress = (userBalance.toFloat() / raffle.requiredTicketsToPlay.toFloat()).coerceIn(0.0f, 1.0f)
    val isReady = userBalance >= raffle.requiredTicketsToPlay

    val bgBrush = when (raffle.backgroundType) {
        "black" -> Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF000000)))
        "gold" -> Brush.linearGradient(listOf(Color(0xFFF39C12), Color(0xFFF1C40F)))
        else -> Brush.linearGradient(listOf(Color(0xFF2ECC71), Color(0xFF27AE60)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardTap() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = raffle.title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isAmazon) "Cost: ${raffle.requiredTicketsToPlay} Ticket Pieces" else "Cost: ${raffle.requiredTicketsToPlay} Tickets",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAmazon) "🛍️" else if (raffle.backgroundType == "gold") "🟡" else "💵",
                            fontSize = 18.sp
                        )
                    }
                }

                // Cards progress bar representations
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${raffle.ticketsRemaining} raffle tickets left",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Text(
                            text = "Filled: ${raffle.slotsFilled}/${raffle.slotsRequired}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Progress bar slots
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Progress fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color.White)
                                .align(Alignment.CenterStart)
                        )

                        Text(
                            text = if (isAmazon) "$userBalance / ${raffle.requiredTicketsToPlay} Pieces" else "$userBalance / ${raffle.requiredTicketsToPlay} Tickets",
                            color = if (progress > 0.5f) Color.Black else Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = onCardTap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReady) Color.White else Color.White.copy(alpha = 0.3f),
                        contentColor = if (raffle.backgroundType == "black") Color.Black else Color(0xFF27AE60)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (isReady) "PLAY NOW" else "COLLECT TICKETS",
                        color = if (isReady) Color.Black else Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RaffleDetailPopup(
    raffle: RaffleItem,
    userTickets: Int,
    userPieces: Int,
    onClose: () -> Unit,
    onPlay: () -> Unit
) {
    val isAmazon = raffle.rewardType == "AMAZON_10"
    val balance = if (isAmazon) userPieces else userTickets
    val hasEnough = balance >= raffle.requiredTicketsToPlay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false) {}, // prevent click-through onClose trigger
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // X Close row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRIZE POOL",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Detail Modal",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LEFT and RIGHT row layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT side: Large thematic card representation (Green / Gold / Black)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when (raffle.backgroundType) {
                                    "black" -> Brush.linearGradient(listOf(Color.DarkGray, Color.Black))
                                    "gold" -> Brush.linearGradient(listOf(Color(0xFFF39C12), LightGold))
                                    else -> Brush.linearGradient(listOf(Color(0xFF2ECC71), Color(0xFF27AE60)))
                                }
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🏆 STAGE ACTIVE",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Column {
                                Text(
                                    text = if (isAmazon) "$10.00 Reward" else if (raffle.backgroundType == "gold") "500K Coins" else "$1.00 Reward",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "RAFFLE",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Slots: ${raffle.slotsFilled}/${raffle.slotsRequired}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAmazon) "🛍️" else "🪙",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    // RIGHT side: Prizes tier list
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Drawing Odds:",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        if (isAmazon) {
                            PrizeTierRow(crown = "🥇", amount = "$10.00", count = "1 left", color = LightGold)
                            PrizeTierRow(crown = "🥈", amount = "$2.50", count = "10 left", color = Color.Gray)
                            PrizeTierRow(crown = "🥉", amount = "$1.00", count = "50 left", color = Color(0xFFCD7F32))
                        } else if (raffle.backgroundType == "gold") {
                            PrizeTierRow(crown = "🥇", amount = "500K Coins", count = "1 left", color = LightGold)
                            PrizeTierRow(crown = "🥈", amount = "100K Coins", count = "10 left", color = Color.Gray)
                            PrizeTierRow(crown = "🥉", amount = "20K Coins", count = "100 left", color = Color(0xFFCD7F32))
                        } else {
                            PrizeTierRow(crown = "🥇", amount = "$1.00", count = "45 left", color = LightGold)
                            PrizeTierRow(crown = "🥈", amount = "$0.70", count = "50 left", color = Color.Gray)
                            PrizeTierRow(crown = "🥉", amount = "$0.50", count = "50 left", color = Color(0xFFCD7F32))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PLAY action button
                Button(
                    onClick = onPlay,
                    enabled = hasEnough,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeClaps,
                        disabledContainerColor = Color.LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp)
                ) {
                    Text(
                        text = if (hasEnough) "PLAY ▶" else "INSUFFICIENT BALANCE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ticket count remaining text below button
                Text(
                    text = "${raffle.ticketsRemaining} raffle tickets left in pool",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PrizeTierRow(
    crown: String,
    amount: String,
    count: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgGrey)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = crown, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
            Text(
                text = amount,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        }
        Text(
            text = count,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun RaffleTicketPiecesPopup(
    currentPieces: Int,
    piecesWon: Int,
    unclaimedChests: Int,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .clickable(enabled = false) {}, // prevent closing on panel click
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSlate)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header label
                Text(
                    text = "Raffle Ticket Pieces",
                    color = LightGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Large Black Amazon icon xCount
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(2.dp, OrangeClaps)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛍️", fontSize = 42.sp)
                            Text(
                                text = "x$currentPieces",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Descriptions
                Text(
                    text = "Collect 120 Ticket Pieces to enter raffle, win $10 Amazon Coupon!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pieces progress bar with gold piece indicators
                val progress = (currentPieces.toFloat() / 120f).coerceIn(0.0f, 1.0f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💎", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))

                    // Progress
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFF333333))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFF9800), LightGold)))
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$currentPieces/120",
                        color = LightGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                // Banner for chest badges
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF262626))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎁", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Unopened Chests Stock:",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$unclaimedChests Available",
                        color = OrangeClaps,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                if (piecesWon > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "+$piecesWon pieces gained from chest! 🎉",
                        color = CashGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeClaps),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingCoinAnimation() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "coins")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        val count = 25
        for (i in 0 until count) {
            val randomXOffset = remember { (10..90).random() / 100f }
            val randomSpeed = remember { (60..140).random() / 100f }
            val randomYStart = remember { (60..90).random() / 100f }
            val rotationAngle = remember { (0..360).random() }
            
            val yPosPct = (randomYStart - (animProgress * randomSpeed * 0.8f)).coerceIn(0f, 1f)
            val scale = (0.5f + (1f - animProgress) * randomSpeed).coerceIn(0.2f, 2.0f)
            val alpha = (1f - animProgress).coerceIn(0f, 1f)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        translationX = (randomXOffset * 600f) - 300f,
                        translationY = (yPosPct * 1000f) - 500f,
                        rotationZ = (rotationAngle + animProgress * 360f * randomSpeed),
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🪙", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun ChestResultClaimOverlay(
    chestType: String? = "Wooden",
    coins: Long,
    tickets: Int,
    cash: Double,
    pieces: Int,
    prizeType: String? = null,
    prizeValue: String? = null,
    onClaim: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(150)
                }
            }
        } catch (e: Exception) {}

        try {
            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneG.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 200)
        } catch (e: Exception) {}
    }

    var animationPhase by remember { mutableStateOf(0) } // 0: Closed bouncing, 1: Chest burst & light rays, 2: Cards flying, 3: Settled show all

    LaunchedEffect(Unit) {
        delay(1000)
        animationPhase = 1
        delay(600)
        animationPhase = 2
        delay(800)
        animationPhase = 3
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rays")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Bounce infinite animation for phase 0
    val bounceY by animateFloatAsState(
        targetValue = if (animationPhase == 0) -25f else 0f,
        animationSpec = if (animationPhase == 0) {
            infiniteRepeatable(
                animation = tween(250, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(150)
        },
        label = "bounceY"
    )

    // Scale animation of chest as it pops open
    val chestScale by animateFloatAsState(
        targetValue = when (animationPhase) {
            0 -> 1.0f
            1 -> 1.4f
            else -> 0.8f
        },
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "chestScale"
    )

    val chestEmoji = when (chestType?.lowercase()) {
        "wooden", "wood" -> "🪵📦"
        "golden", "gold" -> "🏆🎁"
        "luxury", "diamond" -> "👑💎"
        else -> "📦"
    }

    val chestLabel = when (chestType?.lowercase()) {
        "wooden", "wood" -> "Wooden Chest"
        "golden", "gold" -> "Golden Chest"
        "luxury", "diamond" -> "Diamond Chest"
        else -> chestType ?: "Treasure Chest"
    }

    val cardColor = when (chestType?.lowercase()) {
        "wooden", "wood" -> Color(0xFF8B4513)
        "golden", "gold" -> Color(0xFFD4AF37)
        "luxury", "diamond" -> Color(0xFF9B59B6)
        else -> OrangeClaps
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onClaim() },
        contentAlignment = Alignment.Center
    ) {
        // Rotating sunburst background
        Canvas(
            modifier = Modifier
                .size(360.dp)
                .graphicsLayer { rotationZ = rotationAngle }
        ) {
            val rayCount = 12
            val angleStep = 360f / rayCount
            for (i in 0 until rayCount) {
                drawArc(
                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                    startAngle = i * angleStep,
                    sweepAngle = angleStep / 2,
                    useCenter = true
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Chest representation
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = bounceY
                        scaleX = chestScale
                        scaleY = chestScale
                    }
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (animationPhase == 0) chestEmoji else "🔓✨",
                    fontSize = 72.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text indicators
            androidx.compose.animation.AnimatedVisibility(
                visible = animationPhase >= 1,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn()
            ) {
                Text(
                    text = "$chestLabel Opened!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flyout reward cards (Phase 2 & 3)
            androidx.compose.animation.AnimatedVisibility(
                visible = animationPhase >= 2,
                enter = androidx.compose.animation.scaleIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + androidx.compose.animation.fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "You Obtained:",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Yellow Coin card flyer
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .padding(8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // Yellow card
                        border = BorderStroke(2.dp, Color(0xFFFFD54F))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🟡", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "+$coins Coins",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }

                    // Bonus indicators
                    if (cash > 0.0 || tickets > 0) {
                        Text(
                            text = "✨ PLUS BONUSES! ✨",
                            color = Color(0xFF81C784),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (cash > 0.0) {
                                Card(
                                    modifier = Modifier.padding(4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Green cash card
                                    border = BorderStroke(1.5.dp, Color(0xFF81C784))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("💵", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = String.format("+$%.2f Cash", cash),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            if (tickets > 0) {
                                Card(
                                    modifier = Modifier.padding(4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE)), // Blue ticket card
                                    border = BorderStroke(1.5.dp, Color(0xFF4FC3F7))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎟️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+$tickets Tickets",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0277BD)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = onClaim,
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "CLAIM ALL",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (chestEmoji == "🏆🎁") Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun ChestResultRow(
    emoji: String,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgGrey)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.Black
            )
        }
        Text(
            text = value,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = CashGreen
        )
    }
}

@Composable
fun MyChestCard(
    type: String, // "wooden", "golden", "luxury"
    count: Int,
    onOpen: () -> Unit
) {
    val cardColor = when (type) {
        "wooden" -> Color(0xFF8B5E3C) // Brown #8B5E3C
        "golden" -> Color(0xFFFFD700) // Gold #FFD700
        "luxury" -> Color(0xFF7B2FBE) // Purple #7B2FBE
        else -> Color.Gray
    }
    
    val displayName = when (type) {
        "wooden" -> "Wooden Chest"
        "golden" -> "Golden Chest"
        "luxury" -> "Diamond Chest"
        else -> "Chest"
    }

    val chestIllustration = when (type) {
        "wooden" -> "🪵📦"
        "golden" -> "🏆🎁"
        "luxury" -> "👑💎"
        else -> "📦"
    }

    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 12.dp)
            .size(width = 120.dp, height = 140.dp)
    ) {
        // Render layers of card stack if count > 1
        if (count > 1) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 6.dp, y = (-6).dp)
                    .graphicsLayer(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, cardColor.copy(alpha = 0.2f))
            ) { Box(modifier = Modifier.fillMaxSize()) }
        }
        if (count > 2) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = 12.dp, y = (-12).dp)
                    .graphicsLayer(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.03f)),
                border = BorderStroke(1.dp, cardColor.copy(alpha = 0.1f))
            ) { Box(modifier = Modifier.fillMaxSize()) }
        }

        // Main upper Card
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, cardColor.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: chest illustration (60dp circle)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(cardColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chestIllustration, fontSize = 28.sp)
                }

                // Middle: chest name
                Text(
                    text = displayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Bottom: "TAP TO OPEN" orange button
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5A623)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Text(
                        text = "TAP TO OPEN",
                        fontWeight = FontWeight.Black,
                        fontSize = 8.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Top-right corner badge "x3" if count > 1
        if (count > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(Color.Red, RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "x$count",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
