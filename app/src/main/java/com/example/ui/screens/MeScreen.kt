package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClapEarnViewModel
import com.example.ui.theme.*

// Local sub-route enum for Me tab flow states
enum class MeSubRoute {
    PROFILE_HOME,
    EDIT_PROFILE,
    CLAPCOINS_EXCHANGE,
    CASH_OUT,
    NOTIFICATIONS,
    SUBSCRIPTIONS,
    MY_CLAPS,
    INVITE_FRIENDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: ClapEarnViewModel,
    modifier: Modifier = Modifier
) {
    // Current local sub-route inside ME screen flow
    var currentSubRoute by remember { mutableStateOf(MeSubRoute.PROFILE_HOME) }

    // Persistent avatar selection stored locally in SharedPreferences
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("clapearn_prefs", Context.MODE_PRIVATE) }
    var avatarIndex by remember { mutableStateOf(sharedPrefs.getInt("avatar_index", 0)) }

    // Avatar resources mappings
    val avatarEmojis = listOf("🦊", "🦁", "🐼", "🐨", "🦄", "🐸")
    val avatarNames = listOf("Lucky Fox", "Brave Lion", "Happy Panda", "Cute Koala", "Mystic Unicorn", "Clever Frog")
    val avatarColors = listOf(
        Color(0xFFFFE0B2), // Fox Orange bg
        Color(0xFFFFF9C4), // Lion Yellow bg
        Color(0xFFE0F7FA), // Panda Cyan bg
        Color(0xFFD1C4E9), // Koala Purple bg
        Color(0xFFF8BBD0), // Unicorn Pink bg
        Color(0xFFC8E6C9)  // Frog Green bg
    )

    // Collect global wallet values
    val wallet by viewModel.wallet.collectAsState()
    val isFirebaseAvailable by viewModel.isFirebaseAvailable.collectAsState()

    // Screen navigation crossfades
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (currentSubRoute) {
            MeSubRoute.PROFILE_HOME -> {
                ProfileHomeScreen(
                    walletCoins = wallet.clapCoins,
                    walletCash = wallet.cashUsd,
                    username = wallet.username,
                    clipId = wallet.clipId,
                    referralCode = wallet.referralCode,
                    hasRedeemedCode = wallet.hasRedeemedCode,
                    isFirebaseEnabled = isFirebaseAvailable,
                    avatarEmoji = avatarEmojis[avatarIndex],
                    avatarBgColor = avatarColors[avatarIndex],
                    onNavigateTo = { route -> currentSubRoute = route },
                    onRedeemCodeSubmit = { code, callback ->
                        viewModel.redeemCode(code, callback)
                    },
                    onTriggerSignIn = {
                        viewModel.simulateGoogleSignIn { }
                    }
                )
            }
            MeSubRoute.EDIT_PROFILE -> {
                EditProfileScreen(
                    currentUsername = wallet.username,
                    avatarEmojis = avatarEmojis,
                    avatarNames = avatarNames,
                    avatarColors = avatarColors,
                    selectedIndex = avatarIndex,
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME },
                    onSave = { newName, newAvatarIndex ->
                        sharedPrefs.edit().putInt("avatar_index", newAvatarIndex).apply()
                        avatarIndex = newAvatarIndex
                        viewModel.updateProfile(newName) { success, _ ->
                            if (success) {
                                currentSubRoute = MeSubRoute.PROFILE_HOME
                            }
                        }
                    }
                )
            }
            MeSubRoute.CLAPCOINS_EXCHANGE -> {
                ClapCoinsExchangeScreen(
                    currentCoins = wallet.clapCoins,
                    currentCash = wallet.cashUsd,
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME },
                    onExchangeSubmit = { coinsToExchange, callback ->
                        viewModel.exchangeCoins(coinsToExchange, callback)
                    }
                )
            }
            MeSubRoute.CASH_OUT -> {
                CashOutScreen(
                    currentCash = wallet.cashUsd,
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME },
                    onWithdrawSubmit = { amount, email, callback ->
                        viewModel.withdrawCash(amount, email, callback)
                    }
                )
            }
            MeSubRoute.NOTIFICATIONS -> {
                NotificationsHistoryScreen(
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME }
                )
            }
            MeSubRoute.SUBSCRIPTIONS -> {
                SubscriptionsScreen(
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME }
                )
            }
            MeSubRoute.MY_CLAPS -> {
                ClapsHistoryScreen(
                    walletClaps = wallet.totalClapsGiven,
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME }
                )
            }
            MeSubRoute.INVITE_FRIENDS -> {
                InviteFriendsScreen(
                    viewModel = viewModel,
                    referralCode = wallet.referralCode,
                    clipId = wallet.clipId,
                    diamondChests = wallet.diamondChests,
                    onBack = { currentSubRoute = MeSubRoute.PROFILE_HOME }
                )
            }
        }
    }
}

// ==========================================
// 1. PROFILE HOME SCREEN (EXACT SPEC CONTENT)
// ==========================================
@Composable
fun ProfileHomeScreen(
    walletCoins: Long,
    walletCash: Double,
    username: String,
    clipId: String,
    referralCode: String,
    hasRedeemedCode: Boolean,
    isFirebaseEnabled: Boolean,
    avatarEmoji: String,
    avatarBgColor: Color,
    onNavigateTo: (MeSubRoute) -> Unit,
    onRedeemCodeSubmit: (String, (String) -> Unit) -> Unit,
    onTriggerSignIn: () -> Unit
) {
    var redeemInput by remember { mutableStateOf(TextFieldValue("")) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP SECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(MeSubRoute.EDIT_PROFILE) }
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .testTag("edit_profile_header"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar (large circle, left side)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor)
                    .border(3.dp, GoldCoins, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarEmoji,
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Username & Clapcode
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = username,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Orange clapcode label
                Text(
                    text = "Clapcode: $clipId",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldCoins
                )
            }

            // Arrow > to edit profile
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Edit Profile Arrow",
                tint = TextGrey,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // BALANCE CARDS ROW (2 cards side by side)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left card (Orange gradient)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFF39C12), Color(0xFFE67E22))
                        )
                    )
                    .clickable { onNavigateTo(MeSubRoute.CLAPCOINS_EXCHANGE) }
                    .padding(16.dp)
                    .testTag("exchange_card_button")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CLAPCOINS BALANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = walletCoins.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "EXCHANGE ▶",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Right card (Green/teal gradient)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2ECC71), Color(0xFF1ABC9C))
                        )
                    )
                    .clickable { onNavigateTo(MeSubRoute.CASH_OUT) }
                    .padding(16.dp)
                    .testTag("cash_out_card_button")
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CASH BALANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$ ${String.format("%.2f", walletCash)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CASH OUT ▶",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // FEEDBACK BANNER (If present)
        feedbackMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldCoins.copy(alpha = 0.12f))
                    .border(1.dp, GoldCoins, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = msg,
                    color = DarkGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // MENU LIST (white background, gray dividers)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            // Invite Friends Item
            MenuListItem(
                emoji = "👥",
                label = "Invite Friends & Get Diamond Chests ✨",
                testTag = "menu_item_invite_friends",
                onClick = { onNavigateTo(MeSubRoute.INVITE_FRIENDS) }
            )
            Divider(color = Color(0xFFF1F1F1), thickness = 1.dp)

            // Notifications Item
            MenuListItem(
                emoji = "🔔",
                label = "Notifications",
                testTag = "menu_item_notifications",
                onClick = { onNavigateTo(MeSubRoute.NOTIFICATIONS) }
            )
            Divider(color = Color(0xFFF1F1F1), thickness = 1.dp)

            // Subscriptions Item
            MenuListItem(
                emoji = "🔖",
                label = "My Subscriptions",
                testTag = "menu_item_subscriptions",
                onClick = { onNavigateTo(MeSubRoute.SUBSCRIPTIONS) }
            )
            Divider(color = Color(0xFFF1F1F1), thickness = 1.dp)

            // My Claps Item
            MenuListItem(
                emoji = "👏",
                label = "My Claps",
                testTag = "menu_item_my_claps",
                onClick = { onNavigateTo(MeSubRoute.MY_CLAPS) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // REDEEM REFERRAL CODE (Preserved from old design but integrated as a neat visual section)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "🎟️ REDEEM REFERRAL CODE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Enter a friend's code to claim an INSTANT $1.00 USD reward!",
                    fontSize = 11.sp,
                    color = TextGrey,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = redeemInput,
                        onValueChange = { redeemInput = it },
                        placeholder = { Text("E.g. FREE7788", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldCoins,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val code = redeemInput.text.trim()
                            if (code.isNotEmpty()) {
                                onRedeemCodeSubmit(code) { response ->
                                    feedbackMessage = response
                                    redeemInput = TextFieldValue("")
                                }
                            } else {
                                feedbackMessage = "Please type a referral code first!"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Claim", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // FIREBASE STATUS & ACTIVATE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191919))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sync",
                        tint = if (isFirebaseEnabled) CashGreen else GoldCoins,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFirebaseEnabled) "CLOUD DATA SYNCHRONIZED" else "LOCAL DEVICE SAFE-CORE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFirebaseEnabled) {
                        "Your ClapCoins balances and referral details are fully synced in Google Firestore."
                    } else {
                        "Data is secured locally using Room persistence. Authenticate with Google to back up your records."
                    },
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onTriggerSignIn() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFirebaseEnabled) CashGreen else GoldCoins),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Sign in icon",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sign-In & Backup with Google",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "My custom Referral Code: $referralCode",
                    color = GoldCoins,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Menu Row helper
@Composable
fun MenuListItem(
    emoji: String,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Menu Chevron",
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ==========================================
// 2. EDIT PROFILE SUB-VIEW
// ==========================================
@Composable
fun EditProfileScreen(
    currentUsername: String,
    avatarEmojis: List<String>,
    avatarNames: List<String>,
    avatarColors: List<Color>,
    selectedIndex: Int,
    onBack: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var usernameVal by remember { mutableStateOf(currentUsername) }
    var chosenAvatarIndex by remember { mutableStateOf(selectedIndex) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Safe navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected representation
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(avatarColors[chosenAvatarIndex])
                    .border(3.dp, GoldCoins, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarEmojis[chosenAvatarIndex],
                    fontSize = 54.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = avatarNames[chosenAvatarIndex],
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrey
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selection grid
            Text(
                text = "CHOOSE NEW AVATAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in 0..5) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(avatarColors[i])
                            .border(
                                width = if (chosenAvatarIndex == i) 3.dp else 0.dp,
                                color = if (chosenAvatarIndex == i) GoldCoins else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { chosenAvatarIndex = i }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(avatarEmojis[i], fontSize = 22.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Username editor input
            Text(
                text = "EDIT DISPLAY NAME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = usernameVal,
                onValueChange = { usernameVal = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCoins,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { onSave(usernameVal, chosenAvatarIndex) },
                colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_save_button"),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Save Profile Changes",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==========================================
// 3. CLAPCOINS EXCHANGE SCREEN SUB-VIEW
// ==========================================
@Composable
fun ClapCoinsExchangeScreen(
    currentCoins: Long,
    currentCash: Double,
    onBack: () -> Unit,
    onExchangeSubmit: (Long, (Boolean, String) -> Unit) -> Unit
) {
    var exchangeValue by remember { mutableStateOf("1000") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var loadingState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "ClapCoins Exchange",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rate Information box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "STANDARD EXCHANGE RATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100,000 Coins 🟡 = $1.00 USD 💚",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Text(
                        text = "1,000 Coins = $0.01 USD",
                        fontSize = 12.sp,
                        color = TextGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Balances Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("My Coins", fontSize = 11.sp, color = TextGrey)
                        Text("$currentCoins", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("My Cash Balance", fontSize = 11.sp, color = TextGrey)
                        Text("$${String.format("%.2f", currentCash)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CashGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SELECT AMOUNT TO EXCHANGE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick select options
            val options = listOf(1000L, 5000L, 10000L, 50000L, currentCoins)
            val labels = listOf("1K Coins", "5K Coins", "10K Coins", "50K Coins", "Max All")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.zip(labels).take(3).forEach { (valAmt, labelTxt) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (exchangeValue == valAmt.toString()) GoldCoins else Color.LightGray,
                                RoundedCornerShape(8.dp)
                            )
                            .background(if (exchangeValue == valAmt.toString()) GoldCoins.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { exchangeValue = valAmt.toString() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(labelTxt, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.zip(labels).drop(3).forEach { (valAmt, labelTxt) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (exchangeValue == valAmt.toString()) GoldCoins else Color.LightGray,
                                RoundedCornerShape(8.dp)
                            )
                            .background(if (exchangeValue == valAmt.toString()) GoldCoins.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { exchangeValue = valAmt.toString() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(labelTxt, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Input
            OutlinedTextField(
                value = exchangeValue,
                onValueChange = { exchangeValue = it.filter { char -> char.isDigit() } },
                label = { Text("Or Type Custom Coins Count") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldCoins,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Receive preview
            val typedCoins = exchangeValue.toLongOrNull() ?: 0L
            val previewCash = typedCoins.toDouble() / 100000.0

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF9EE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Spent: $typedCoins Coins", fontSize = 13.sp, color = Color.Black)
                    Text("Receive: $${String.format("%.4f", previewCash)} USD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (loadingState) {
                CircularProgressIndicator(color = GoldCoins)
            } else {
                Button(
                    onClick = {
                        if (typedCoins <= 0L) {
                            feedbackMessage = "❌ Please enter a positive coins amount"
                            return@Button
                        }
                        if (currentCoins < typedCoins) {
                            feedbackMessage = "❌ You only have $currentCoins Coins"
                            return@Button
                        }
                        loadingState = true
                        onExchangeSubmit(typedCoins) { success, msg ->
                            loadingState = false
                            feedbackMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exchange_submit_button"),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("CONFIRM EXCHANGE NOW", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            feedbackMessage?.let { msg ->
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// 4. CASH OUT (WITHDRAWAL) VIEW
// ==========================================
@Composable
fun CashOutScreen(
    currentCash: Double,
    onBack: () -> Unit,
    onWithdrawSubmit: (Double, String, (Boolean, String) -> Unit) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("PayPal") }
    var selectedThresholdIndex by remember { mutableStateOf(0) }
    var emailAddress by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val thresholds = listOf(0.10, 10.00, 15.00, 20.00, 30.00, 50.00)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "Cash Out (Withdraw)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large visual balance info
            Text("CURRENT CASH BALANCE", fontSize = 11.sp, color = TextGrey, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${String.format("%.2f", currentCash)} USD",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = CashGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment method selector
            Text(
                text = "SELECT PAYMENT METHOD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val methods = listOf("PayPal", "Amazon Code", "Mobile Topup")
                methods.forEach { method ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.5.dp,
                                color = if (selectedMethod == method) CashGreen else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(if (selectedMethod == method) CashGreen.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { selectedMethod = method }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Threshold grids
            Text(
                text = "SELECT CASH OUT AMOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid layout manually crafted
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val amount = thresholds[index]
                            val isSelectable = currentCash >= amount

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selectedThresholdIndex == index) 2.dp else 1.dp,
                                        color = when {
                                            selectedThresholdIndex == index -> CashGreen
                                            isSelectable -> Color.LightGray
                                            else -> Color.LightGray.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        when {
                                            selectedThresholdIndex == index -> CashGreen.copy(alpha = 0.1f)
                                            isSelectable -> Color.Transparent
                                            else -> Color(0xFFF8F9FA)
                                        }
                                    )
                                    .clickable { selectedThresholdIndex = index }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$${String.format("%.2f", amount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isSelectable) Color.Black else Color.LightGray
                                    )
                                    if (index == 0) {
                                        Text(
                                            "Newbie",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CashGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Account Detail
            Text(
                text = "RECEIVING ACCOUNT DETAIL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = emailAddress,
                onValueChange = { emailAddress = it },
                placeholder = {
                    Text(
                        when (selectedMethod) {
                            "PayPal" -> "PayPal Account Email"
                            "Amazon Code" -> "Delivery Email Address"
                            else -> "Mobile Country Code + Number"
                        },
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CashGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            if (isLoading) {
                CircularProgressIndicator(color = CashGreen)
            } else {
                Button(
                    onClick = {
                        val limit = thresholds[selectedThresholdIndex]
                        if (currentCash < limit) {
                            feedbackMsg = "❌ Insufficient balance for this cash out threshold!"
                            return@Button
                        }
                        if (emailAddress.trim().isEmpty()) {
                            feedbackMsg = "❌ Please enter your withdrawal account details!"
                            return@Button
                        }
                        isLoading = true
                        onWithdrawSubmit(limit, emailAddress) { success, text ->
                            isLoading = false
                            feedbackMsg = text
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_out_submit_button"),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("CONFIRM WITHDRAWAL NOW", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            feedbackMsg?.let { msg ->
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// 5. NOTIFICATIONS SUB-SCREEN
// ==========================================
@Composable
fun NotificationsHistoryScreen(onBack: () -> Unit) {
    val alerts = listOf(
        "🎁 Welcome Reward! Claimed 5,000 intro ClapCoins successfully!" to "3 hours ago",
        "👏 Awesome! Received 150 ClapCoins for your first clapped video!" to "1 day ago",
        "🔥 Streak multiplier active! 5-Day daily streak milestone recognized!" to "2 days ago",
        "💵 Referral bonus payout: Received $1.00 USD cash code successfully!" to "3 days ago"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "Notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(alerts) { (msg, time) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(msg, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(time, fontSize = 11.sp, color = TextGrey)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SUBSCRIPTIONS SUB-SCREEN
// ==========================================
@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    var subsList by remember {
        mutableStateOf(
            listOf(
                Triple("CatGymnastics", "524K subscribers", true),
                Triple("GourmetLab", "230K subscribers", true),
                Triple("SkateFlow", "981K subscribers", false),
                Triple("ZenSphere", "45K subscribers", false)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "My Subscriptions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subsList) { sItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoldCoins),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sItem.first.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(sItem.first, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Text(sItem.second, fontSize = 11.sp, color = TextGrey)
                        }
                    }

                    Button(
                        onClick = {
                            subsList = subsList.map {
                                if (it.first == sItem.first) it.copy(third = !it.third) else it
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sItem.third) Color.LightGray else GoldCoins
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (sItem.third) "Subscribed" else "Subscribe",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. CLAPS HISTORY SUB-SCREEN
// ==========================================
@Composable
fun ClapsHistoryScreen(walletClaps: Int, onBack: () -> Unit) {
    val claps = listOf(
        "Clapped on 'Wait for the amazing trick at the end! 🐈💨'" to "+1 Clap given successfully",
        "Clapped on 'Making the legendary Golden Egg Omelette 🍳✨'" to "+1 Clap given successfully",
        "Clapped on 'Skateboarding down a futuristic neon highway!'" to "+1 Clap given successfully",
        "Clapped on 'Calming ocean loop for high productivity'" to "+1 Clap given successfully"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "My Claps History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Divider(color = Color(0xFFF1F1F1))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("👏 Total Claps Given: ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("$walletClaps", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DarkGold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(claps) { (topic, details) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(topic, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(details, fontSize = 11.sp, color = TextGrey)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. INVITE FRIENDS SCREEN & QR CODE STATE
// ==========================================
@Composable
fun InviteFriendsScreen(
    viewModel: ClapEarnViewModel,
    referralCode: String,
    clipId: String,
    diamondChests: Int,
    onBack: () -> Unit
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val referrals by viewModel.referrals.collectAsState()

    var showQrCode by remember { mutableStateOf(false) }
    var activePrizeDialog by remember { mutableStateOf<Map<String, Any>?>(null) }
    var feedbackToast by remember { mutableStateOf<String?>(null) }
    var simFriendName by remember { mutableStateOf("") }

    val referralLink = "https://clapearn.page.link/invite?code=$referralCode&id=$clipId"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("invite_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Invite Friends",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                TextButton(
                    onClick = { showQrCode = true },
                    modifier = Modifier.testTag("qr_code_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code Icon",
                        tint = GoldCoins,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "QR Code",
                        color = GoldCoins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Divider(color = Color(0xFFF1F1F1))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // MAIN NAVY GRADIENT CONTENT CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF415A77))
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sparkles decorating top
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("✨", fontSize = 24.sp)
                                Text("✨", fontSize = 18.sp)
                                Text("✨", fontSize = 24.sp)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "INVITE FRIENDS TO GET",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAEC6CF),
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "✨ DIAMOND CHESTS ✨",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF5A623),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Large Treasure Chest Illustration
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(2.dp, Color(0xFFF5A623).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Burst sparkles
                                    Text("🟡 💥 🟡", fontSize = 22.sp)
                                    Text("💎📦💎", fontSize = 72.sp, modifier = Modifier.padding(vertical = 4.dp))
                                    Text("💰✨🪙", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Every friend who installs and watches 3 videos\nguarantees you 1 Diamond Chest!",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // PRIZE CARDS ROW (3 cards below chest)
                Text(
                    text = "DIAMOND CHEST REWARDS POOL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Card (Purple): iPad Pro 11
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD8B4FE))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📱", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "IPAD PRO 11",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF7C3AED),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Center Card (Green): Cash
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF86EFAC))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Red Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Red, RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "Guaranteed",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("💵", fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "CASH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF15803D),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Right Card (Gold): Air Jordan sneaker
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF9C3)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE047))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👟", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AIR JORDAN 1",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // INTERACTIVE GAMEPLAY CHEST OPENER
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎁 MY DIAMOND CHESTS: $diamondChests",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (diamondChests > 0) {
                                    viewModel.openDiamondChest { reward ->
                                        activePrizeDialog = reward
                                    }
                                } else {
                                    feedbackToast = "❌ You don't have any Diamond Chests left! Simulate referrals below to earn more!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "OPEN 1 DIAMOND CHEST 💎",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BOTTOM SHARE BUTTONS
                Text(
                    text = "SHARE INVITE LINK VIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Facebook (blue circle)
                    ShareCircleButton(
                        color = Color(0xFF1877F2),
                        iconLabel = "f",
                        labelText = "Facebook",
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralLink))
                            feedbackToast = "Copied link! Opening Facebook simulation..."
                        }
                    )

                    // Message (green circle)
                    ShareCircleButton(
                        color = Color(0xFF25D366),
                        iconLabel = "💬",
                        labelText = "Message",
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralLink))
                            feedbackToast = "Copied link! Opening SMS messenger simulation..."
                        }
                    )

                    // More (gray circle)
                    ShareCircleButton(
                        color = Color(0xFF7F8C8D),
                        iconLabel = "•••",
                        labelText = "More",
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralLink))
                            feedbackToast = "Copied link to clipboard!"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // DEMO MODE / SANDBOX SIMULATION
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "🛠️ DEMO MODE: SIMULATE A REFERRAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Generate a fake installation event with 3 watched videos to instantly earn a Diamond Chest. This records to Firestore and updates your logs.",
                            fontSize = 10.sp,
                            color = TextGrey,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = simFriendName,
                            onValueChange = { simFriendName = it },
                            placeholder = { Text("Friend's name (e.g. John)", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldCoins,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val name = simFriendName.trim()
                                if (name.isNotEmpty()) {
                                    viewModel.addSimulatedReferral(name) { msg ->
                                        feedbackToast = msg
                                        simFriendName = ""
                                    }
                                } else {
                                    feedbackToast = "Please type a friend's name to simulate registration!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CashGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Registration & 3 Video Views", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // MY REFERRALS LOGS TRACKER
                Text(
                    text = "MY REFERRALS & TRACKING LOGS (FIRESTORE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (referrals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No referrals tracked yet.\nSimulate registration above to see logs!",
                            fontSize = 11.sp,
                            color = TextGrey,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        referrals.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item["friendName"]?.toString() ?: "Friend",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Status: ${item["status"]}",
                                        fontSize = 10.sp,
                                        color = if (item["status"].toString().contains("Completed")) CashGreen else TextGrey
                                    )
                                }
                                Text(
                                    text = "💎 +1 Chest Awarded",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldCoins
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // FEEDBACK TOAST / ALERT BANNER AT THE BOTTOM
        feedbackToast?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .padding(horizontal = 24.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = { feedbackToast = null },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        // QR CODE DIALOG
        if (showQrCode) {
            AlertDialog(
                onDismissRequest = { showQrCode = false },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showQrCode = false }) {
                        Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text(
                        "MY QR CODE & INVITE LINK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Graphic
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .background(Color.White)
                                .border(2.dp, GoldCoins, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⬛  ⬛  ⬛  ⬛  ⬛", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("⬛  ⬜  ⬜  ⬜  ⬛", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("⬛  ⬜  ⬛  ⬜  ⬛", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("⬛  ⬜  ⬜  ⬜  ⬛", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("⬛  ⬛  ⬛  ⬛  ⬛", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "My Referral Code: $referralCode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkGold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = referralLink,
                            fontSize = 11.sp,
                            color = TextGrey,
                            textAlign = TextAlign.Center,
                            textDecoration = TextDecoration.Underline,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(referralLink))
                                feedbackToast = "🎉 Copied referral link to clipboard!"
                                showQrCode = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldCoins),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                containerColor = Color.White
            )
        }

        // DIAMOND CHEST PRIZE Dialog
        activePrizeDialog?.let { reward ->
            AlertDialog(
                onDismissRequest = { activePrizeDialog = null },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { activePrizeDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCoins)
                    ) {
                        Text("AWESOME", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                },
                title = {
                    Text(
                        "🎉 DIAMOND CHEST OPENED! 🎉",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldCoins,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "You have successfully revealed a majestic item from the pool:",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .border(2.dp, GoldCoins, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (reward["prizeType"].toString()) {
                                        "iPad Pro 11" -> "📱"
                                        "Air Jordan 1" -> "👟"
                                        "Cash Reward" -> "💵"
                                        else -> "🟡"
                                    },
                                    fontSize = 44.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reward["prizeType"].toString().uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = DarkGold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reward["prizeValue"].toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = "The rewards have been credited to your active wallet balance! Premium item winners will receive instructions at their linked profile email address.",
                            fontSize = 10.sp,
                            color = TextGrey,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun ShareCircleButton(
    color: Color,
    iconLabel: String,
    labelText: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconLabel,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = labelText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}
