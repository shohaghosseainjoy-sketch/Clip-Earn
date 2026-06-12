package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClapEarnViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GamesScreen(
    viewModel: ClapEarnViewModel,
    modifier: Modifier = Modifier
) {
    val wallet by viewModel.wallet.collectAsState()
    val isSpinning by viewModel.isSpinning.collectAsState()
    val spinAngle by viewModel.spinAngle.collectAsState()
    val gameMessage by viewModel.gameMessage.collectAsState()

    var activeGameTab by remember { mutableStateOf(0) } // 0: Spin Wheel, 1: Scratch Card

    // Collect our BDT monetization states
    val bdtBalance by viewModel.bdtBalance.collectAsState()
    val withdrawalHistory by viewModel.withdrawalHistory.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val dakPurple = Color(0xFF8A2BE2)
    val dakSecondary = Color(0xFF9370DB)
    val dakGradient = Brush.linearGradient(listOf(dakPurple, dakSecondary))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- DAK ADS MONETIZATION CARD ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dakGradient)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAK AD-EARNING WALLET",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.3f BDT", bdtBalance),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💼", fontSize = 22.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Watch Ads & Earn Button
                        Button(
                            onClick = {
                                viewModel.earnBdt(0.01)
                                try {
                                    uriHandler.openUri("https://www.revenuecpmgate.com/ivhqy3ex?key=c0943ab31e4637775ea0a7449e014bb4")
                                } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = dakPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch Ads (+0.01)", color = dakPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        // Withdraw BDT Button
                        OutlinedButton(
                            onClick = { showWithdrawDialog = true },
                            border = BorderStroke(1.5.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("💸 Withdraw BDT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Withdrawal task history
                    if (withdrawalHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Payout Tasks History",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        withdrawalHistory.take(3).forEach { item ->
                            val dateStr = try {
                                val mill = item["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                                val formatter = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                                formatter.format(java.util.Date(mill))
                            } catch (e: Exception) { "" }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${item["amount"]} BDT via ${item["method"]?.uppercase()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A/C: ${item["phone"]} • $dateStr",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (item["status"] == "pending") Color(0xFFFF9800) else Color(0xFF4CAF50))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item["status"]?.uppercase() ?: "PENDING",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- WITHDRAW BDT POPUP DIALOG ---
        if (showWithdrawDialog) {
            var inputAmount by remember { mutableStateOf("") }
            var inputAccount by remember { mutableStateOf("") }
            var selectedMethod by remember { mutableStateOf("bKash") }
            var statusMessage by remember { mutableStateOf<String?>(null) }
            var isError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = {
                    Text(
                        text = "💸 Cashout BDT Wallet",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Fill recipient credentials below to cash out your balance.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        // Selector Rows
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("bKash", "Nagad", "Flexiload").forEach { method ->
                                val isSelected = selectedMethod.lowercase() == method.lowercase()
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) dakPurple else Color(0xFFF1F1F1))
                                        .clickable { selectedMethod = method }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = method,
                                        color = if (isSelected) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Minimum Guidelines representation
                        val guidelinesInfo = if (selectedMethod == "Flexiload") {
                            "Minimum Cashout: 20 BDT"
                        } else {
                            "Minimum Cashout: 100 BDT"
                        }
                        Text(
                            text = "💡 Guideline: $guidelinesInfo",
                            color = dakPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )

                        // Mobile number textfield
                        OutlinedTextField(
                            value = inputAccount,
                            onValueChange = { inputAccount = it },
                            label = { Text("Mobile Number (BKash/Nagad/Phone)") },
                            placeholder = { Text("01xxxxxxxxx") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = dakPurple,
                                focusedLabelColor = dakPurple
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Numeric Amount field
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Amount (BDT)") },
                            placeholder = { Text("Enter BDT Amount") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = dakPurple,
                                focusedLabelColor = dakPurple
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage!!,
                                color = if (isError) Color.Red else Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = inputAmount.toDoubleOrNull()
                            if (amt == null) {
                                statusMessage = "Please enter a valid numeric amount!"
                                isError = true
                                return@Button
                            }
                            if (inputAccount.trim().isEmpty()) {
                                statusMessage = "Please input valid mobile destination number!"
                                isError = true
                                return@Button
                            }

                            viewModel.withdrawBdt(amt, inputAccount.trim(), selectedMethod) { success, msg ->
                                statusMessage = msg
                                isError = !success
                                if (success) {
                                    coroutineScope.launch {
                                        delay(1500)
                                        showWithdrawDialog = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = dakPurple)
                    ) {
                        Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // Game Category Selector Tag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(Color(0xFFF5F5F5))
                .padding(4.dp)
        ) {
            Button(
                onClick = { activeGameTab = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeGameTab == 0) GoldCoins else Color.Transparent,
                    contentColor = if (activeGameTab == 0) Color.Black else TextGrey
                ),
                elevation = null,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lucky Wheel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { activeGameTab = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeGameTab == 1) GoldCoins else Color.Transparent,
                    contentColor = if (activeGameTab == 1) Color.Black else TextGrey
                ),
                elevation = null,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scratch & Win", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Live notification alerts
        AnimatedVisibility(
            visible = gameMessage != null,
            enter = slideInVertically(animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            gameMessage?.let { msg ->
                val containerColor = if (msg.contains("Won") || msg.contains("🎰") || msg.contains("WIN_") || msg.contains("🎉")) {
                    CashGreen.copy(alpha = 0.15f)
                } else {
                    GoldCoins.copy(alpha = 0.15f)
                }
                val textColor = if (msg.contains("Won") || msg.contains("🎰") || msg.contains("WIN_") || msg.contains("🎉")) {
                    DarkGreen
                } else {
                    DarkGold
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .padding(12.dp)
                        .border(1.dp, textColor, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = msg,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeGameTab == 0) {
            // LUCKY WHEEL SCREEN COMPONENT
            LuckyWheelLayout(
                isSpinning = isSpinning,
                spinAngle = spinAngle,
                clapCoins = wallet.clapCoins,
                onSpinClick = { viewModel.playLuckySpin() },
                onEarnSponsorAd = {
                    viewModel.earnBdt(0.01)
                    try {
                        uriHandler.openUri("https://www.revenuecpmgate.com/ivhqy3ex?key=c0943ab31e4637775ea0a7449e014bb4")
                    } catch (e: Exception) {}
                }
            )
        } else {
            // SCRATCH CARD COMPONENT
            ScratchCardLayout(
                clapCoins = wallet.clapCoins,
                onScratchClick = { viewModel.playScratchCard() },
                onEarnSponsorAd = {
                    viewModel.earnBdt(0.01)
                    try {
                        uriHandler.openUri("https://www.revenuecpmgate.com/ivhqy3ex?key=c0943ab31e4637775ea0a7449e014bb4")
                    } catch (e: Exception) {}
                }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun LuckyWheelLayout(
    isSpinning: Boolean,
    spinAngle: Float,
    clapCoins: Long,
    onSpinClick: () -> Unit,
    onEarnSponsorAd: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "MEGA LUCKY WHEEL",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )
        Text(
            text = "Spin with 300 🟡 for huge CASH & Coin rewards!",
            fontSize = 12.sp,
            color = TextGrey,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // The Wheel Container with indicator
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer decorated rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(6.dp, Color.Black, CircleShape)
                    .padding(4.dp)
                    .border(8.dp, GoldCoins, CircleShape)
            )

            // Drawing slices on Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .rotate(spinAngle)
            ) {
                val wheelColors = listOf(
                    GoldCoins, Color(0xFF34495E),
                    CashGreen, Color(0xFF2C3E50),
                    GoldCoins, Color(0xFF34495E),
                    CashGreen, Color(0xFF2C3E50)
                )
                val totalSlices = 8
                val sliceAngle = 360f / totalSlices

                for (i in 0 until totalSlices) {
                    drawArc(
                        color = wheelColors[i],
                        startAngle = i * sliceAngle - 112.5f, // offset by 90 + half slice for top alignment
                        sweepAngle = sliceAngle,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                }

                // draw tiny inner pin
                drawCircle(color = Color.Black, radius = 16f)
            }

            // Wheel Pins overlay (megawins, 50c, etc. as labels)
            // Draw static text indicators around the wheel coordinates
            WheelLabelsOverlay()

            // Needle on top pointing down
            Box(
                modifier = Modifier
                    .size(24.dp, 36.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width / 2, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path = path, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spin Button
        Button(
            onClick = { onSpinClick() },
            enabled = !isSpinning,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldCoins,
                disabledContainerColor = Color.LightGray
            ),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = if (isSpinning) "SPINNING..." else "SPIN • 300 🟡",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your Balance: $clapCoins 🟡",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        // Sponsored Offer link below Lucky Wheel
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onEarnSponsorAd() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎁", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sponsor Flash Bonus!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Complete sponsor tasks & claim instant 0.01 BDT reward!",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD54F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("GO ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun WheelLabelsOverlay() {
    Box(modifier = Modifier.size(260.dp)) {
        val labels = listOf(
            "$0.50" to Alignment.TopCenter,
            "100 🟡" to Alignment.TopEnd,
            "$0.05" to Alignment.CenterEnd,
            "500 🟡" to Alignment.BottomEnd,
            "50 🟡" to Alignment.BottomCenter,
            "$0.15" to Alignment.BottomStart,
            "1,000" to Alignment.CenterStart,
            "200 🟡" to Alignment.TopStart
        )

        labels.forEach { (text, alignment) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = alignment
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xB3000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ScratchCardLayout(
    clapCoins: Long,
    onScratchClick: () -> Unit,
    onEarnSponsorAd: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "SCRATCH & WIN CASH",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )
        Text(
            text = "Match emojis to unlock instant rewards up to $1.00 USD!",
            fontSize = 12.sp,
            color = TextGrey,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Simulated Scratch Card Surface
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(2.dp, GoldCoins, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Background behind silver coating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 48.sp)
                    Text("🍀", fontSize = 48.sp)
                    Text("💚", fontSize = 48.sp)
                }

                // Silver Overlay coating representing a fresh card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFBDC3C7), Color(0xFF95A5A6), Color(0xFF7F8C8D))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LUCKY SCRATCH SURFACE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP SCRATCH TO REVEAL SYMBOLS",
                            color = Color(0xFFDFE6E9),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scratch trigger button
        Button(
            onClick = { onScratchClick() },
            colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .width(200.dp)
                .height(50.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                "SCRATCH • 500 🟡",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your Balance: $clapCoins 🟡",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        // Sponsored Offer link below Scratch Card
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEarnSponsorAd() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
            border = BorderStroke(1.5.dp, Color(0xFFFFD54F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎁", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sponsor Scratch Match!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Explore sponsor link & get instant 0.01 BDT gift!",
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFD54F))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("GO ➔", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
        }
    }
}
