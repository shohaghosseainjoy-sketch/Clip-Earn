package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                onSpinClick = { viewModel.playLuckySpin() }
            )
        } else {
            // SCRATCH CARD COMPONENT
            ScratchCardLayout(
                clapCoins = wallet.clapCoins,
                onScratchClick = { viewModel.playScratchCard() }
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
    onSpinClick: () -> Unit
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
    onScratchClick: () -> Unit
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
    }
}
