package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Contribution
import com.example.data.Loan
import com.example.data.TransactionRecord
import com.example.ui.theme.Translations
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.BackupLog
import com.example.ui.viewmodel.Message
import com.example.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

// --- Customized Elegant Dark Palette Theme Tokens ---
val EarthDarkBg = Color(0xFF1C1B1F)     // Main body dark background [Tailwind bg-[#1C1B1F]]
val ForestCard = Color(0xFF2B2930)      // Sleek card / component background [#2B2930]
val SageNeutral = Color(0xFF49454F)     // Borders and dividers [#49454F]
val MintPrimary = Color(0xFFD0BCFF)     // Glowing Purple Accent [#D0BCFF]
val GoldAccent = Color(0xFFEADDFF)      // Light high-contrast lavender background [#EADDFF]
val LightSageText = Color(0xFFE6E1E5)   // Polished white-gray text [#E6E1E5]
val TerracottaWarn = Color(0xFFF2B8B5)  // Warning/repayment red [#F2B8B5]
val SolidBlack = Color(0xFF1D192B)      // Deep dark violet brand color [#1D192B]
val VioletDeep = Color(0xFF21005D)      // Extremely high-contrast dark text inside light lavender card [#21005D]
val PureGold = Color(0xFFE5C07B)        // Traditional gold highlights
val LightLavenderBg = Color(0xFFEADDFF) // Beautiful light lavender accent

@Composable
fun VillageSavingsApp(viewModel: SavingsViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLang by viewModel.currentLanguage.collectAsStateWithLifecycle()

    // Determine scheme colors
    val themeBg = if (isDarkMode) EarthDarkBg else Color(0xFFF4F8F5)
    val themeSurface = if (isDarkMode) ForestCard else Color(0xFFFFFFFF)
    val themeText = if (isDarkMode) LightSageText else Color(0xFF1A1F1C)
    val dividerColor = if (isDarkMode) SageNeutral else Color(0xFFE0E6E2)

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = themeBg,
            surface = themeSurface,
            primary = MintPrimary,
            secondary = GoldAccent,
            onBackground = themeText,
            onSurface = themeText
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = themeBg
        ) {
            when (authState) {
                is AuthState.Locked, is AuthState.Error, is AuthState.Authenticating -> {
                    SecurityLockScreen(viewModel, authState, currentLang)
                }
                is AuthState.Authenticated -> {
                    MainAppLayout(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        themeBg = themeBg,
                        themeSurface = themeSurface,
                        themeText = themeText,
                        isDarkMode = isDarkMode,
                        dividerColor = dividerColor
                    )
                }
            }
        }
    }
}

// --- SECURE BIOMETRIC & PIN LOCK SCREEN ---
@Composable
fun SecurityLockScreen(
    viewModel: SavingsViewModel,
    authState: AuthState,
    lang: String
) {
    var pinValue by remember { mutableStateOf("") }
    val displayError = authState is AuthState.Error

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SolidBlack, EarthDarkBg)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
        ) {
            // Secure Vault Header Illustration
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SageNeutral)
                    .border(2.dp, GoldAccent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Lock Icon",
                    tint = GoldAccent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = Translations.get("secure_locked", lang),
                fontSize = 22.sp,
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = Translations.get("enter_pin", lang),
                fontSize = 14.sp,
                color = LightSageText,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots Indicator UI
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..4) {
                    val active = pinValue.length >= i
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (active) MintPrimary else SageNeutral)
                            .border(1.dp, if (active) MintPrimary else LightSageText, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Traditional Lockpad Grid (Custom built for 48dp+ accessibility size)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Clear", "0", "Unlock")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (key in row) {
                            val isAction = key == "Clear" || key == "Unlock"
                            val buttonBg = if (isAction) SageNeutral else ForestCard
                            val buttonTextCol = if (key == "Unlock") MintPrimary else if (key == "Clear") TerracottaWarn else LightSageText

                            Button(
                                onClick = {
                                    if (key == "Clear") {
                                        pinValue = ""
                                    } else if (key == "Unlock") {
                                        viewModel.authenticateWithPin(pinValue)
                                    } else {
                                        if (pinValue.length < 4) {
                                            pinValue += key
                                        }
                                        if (pinValue.length == 4) {
                                            viewModel.authenticateWithPin(pinValue)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .testTag("keypad_$key"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonBg,
                                    contentColor = buttonTextCol
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated Biometric Scan Passkey Entry (Requested requirement)
            Button(
                onClick = { viewModel.authenticateWithBiometric() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("biometric_entry_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageNeutral,
                    contentColor = GoldAccent
                ),
                border = BorderStroke(1.dp, GoldAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Key Icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get("finger_unlock", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (displayError) {
                val errMsg = (authState as? AuthState.Error)?.message ?: "Invalid Code"
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errMsg,
                    color = TerracottaWarn,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Translations.get("pin_hint", lang),
                color = LightSageText.copy(alpha = 0.5f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


// --- MAIN APP LAYOUT (TABBED DRAWER CONTAINER) ---
@Composable
fun MainAppLayout(
    viewModel: SavingsViewModel,
    currentLang: String,
    themeBg: Color,
    themeSurface: Color,
    themeText: Color,
    isDarkMode: Boolean,
    dividerColor: Color
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val totalSavings by viewModel.totalSavingsPool.collectAsStateWithLifecycle()
    val activeLoansSum by viewModel.activeLoansOut.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(themeSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = Translations.get("app_title", currentLang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                        Text(
                            text = Translations.get("offline_notice", currentLang),
                            fontSize = 11.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.W500
                        )
                    }

                    // Interactive Settings Tool: Language Switcher and Theme Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // English Button
                        Button(
                            onClick = { viewModel.setLanguage("en") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == "en") MintPrimary else SageNeutral,
                                contentColor = if (currentLang == "en") SolidBlack else LightSageText
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("lang_en_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("EN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Local Language (Chichewa) Button
                        Button(
                            onClick = { viewModel.setLanguage("local") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == "local") MintPrimary else SageNeutral,
                                contentColor = if (currentLang == "local") SolidBlack else LightSageText
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("lang_local_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("CH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Theme switch
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.Settings else Icons.Default.Warning, 
                                contentDescription = "Toggle Contrast Brightness Theme Mode",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Logout Block Vault
                        IconButton(
                            onClick = { viewModel.lockApp() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("lock_app_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Application Secured Vault Mode",
                                tint = TerracottaWarn,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Divider(color = dividerColor)
            }
        },
        bottomBar = {
            Column {
                Divider(color = dividerColor)
                NavigationBar(
                    containerColor = themeSurface,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    val tabsList = listOf(
                        Triple(0, Translations.get("dashboard", currentLang), Icons.Default.Home),
                        Triple(1, Translations.get("contributions", currentLang), Icons.Default.Add),
                        Triple(2, Translations.get("loans", currentLang), Icons.Default.Warning),
                        Triple(3, Translations.get("sync_backup", currentLang), Icons.Default.Info),
                        Triple(4, Translations.get("chat_support", currentLang), Icons.Default.Send)
                    )

                    for (tab in tabsList) {
                        val isSelected = currentTab == tab.first
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab.first) },
                            icon = {
                                Icon(
                                    imageVector = tab.third,
                                    contentDescription = tab.second,
                                    tint = if (isSelected) SolidBlack else if (isDarkMode) LightSageText else Color.DarkGray
                                )
                            },
                            label = {
                                Text(
                                    text = tab.second,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MintPrimary,
                                selectedTextColor = MintPrimary,
                                unselectedTextColor = if (isDarkMode) LightSageText.copy(alpha = 0.6f) else Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(themeBg)
        ) {
            when (currentTab) {
                0 -> DashboardTabScreen(viewModel, currentLang, themeSurface, themeText)
                1 -> ContributionsTabScreen(viewModel, currentLang, themeSurface, themeText)
                2 -> LoansTabScreen(viewModel, currentLang, themeSurface, themeText)
                3 -> BackupTabScreen(viewModel, currentLang, themeSurface, themeText)
                4 -> ChatSupportTabScreen(viewModel, currentLang, themeSurface, themeText)
            }
        }
    }
}


// --- 1. DASHBOARD TAB SCREEN ---
@Composable
fun DashboardTabScreen(
    viewModel: SavingsViewModel,
    lang: String,
    themeSurface: Color,
    themeText: Color
) {
    val totalSavings by viewModel.totalSavingsPool.collectAsStateWithLifecycle()
    val activeLoansSum by viewModel.activeLoansOut.collectAsStateWithLifecycle()
    val records by viewModel.transactionRecords.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "${Translations.get("hello_member", lang)} 👋",
                fontSize = 20.sp,
                color = if (viewModel.isDarkMode.value) Color.White else SolidBlack,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Translations.get("group_analytics", lang),
                fontSize = 13.sp,
                color = GoldAccent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Analytics Cards Panel: HTML-mock 1-to-1 "Elegant Dark" light purple hero card with deep violet text
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(28.dp))
                    .background(LightLavenderBg, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = Translations.get("total_savings", lang).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletDeep.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MK ${String.format(Locale.US, "%,.0f", totalSavings * 1700)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VioletDeep,
                            letterSpacing = (-1).sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(VioletDeep)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item 1: Your Balance / Outstanding Loans
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = Translations.get("active_loans", lang).uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletDeep.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "MK ${String.format(Locale.US, "%,.0f", activeLoansSum * 1700)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletDeep
                        )
                    }

                    // Item 2: Next Meeting
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "NEXT MEETING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletDeep.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Oct 12",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = VioletDeep
                        )
                    }
                }
            }
        }

        // Custom drawn visual circular charts representation (No external drawing deps)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface, RoundedCornerShape(16.dp))
                    .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = Translations.get("savings_pool", lang),
                    fontSize = 14.sp,
                    color = themeText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val fraction = if (totalSavings > 0) {
                    (activeLoansSum / totalSavings).coerceIn(0.0, 1.0).toFloat()
                } else 0.0f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        // Background circle (Sage neutral)
                        drawCircle(
                            color = SageNeutral.copy(alpha = 0.3f),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Active sweep representing active loan capital percentage out
                        drawArc(
                            color = MintPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = GoldAccent,
                            startAngle = -90f,
                            sweepAngle = fraction * 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale.US, "%.0f%%", fraction * 100f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAccent
                        )
                        Text(
                            text = "Loans Out",
                            fontSize = 10.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IndicatorLegendItem("Total Pool", MintPrimary)
                    IndicatorLegendItem("Disbursed", GoldAccent)
                }
            }
        }

        // Recent compliant activity ledger log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Translations.get("recent_activity", lang),
                    fontSize = 15.sp,
                    color = themeText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See Ledger Tab ➜",
                    fontSize = 11.sp,
                    color = MintPrimary,
                    modifier = Modifier.clickable { viewModel.selectTab(3) }
                )
            }
        }

        val subset = records.take(3)
        if (subset.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No local history saved. Reseed from database to start.", color = themeText.copy(alpha = 0.6f))
                }
            }
        } else {
            items(subset) { audit ->
                CompliantRecordRow(audit, themeSurface, themeText)
            }
        }

        // Quick reseed data button (To help visual previewing and debugging)
        item {
            Button(
                onClick = { viewModel.restoreSampleDatabase() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageNeutral,
                    contentColor = GoldAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reseed_data_button"),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text("Reseed Local Database Demo Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = Translations.get("gdpr_compliance", lang),
                color = themeText.copy(alpha = 0.4f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


// --- 2. CONTRIBUTIONS TAB SCREEN ---
@Composable
fun ContributionsTabScreen(
    viewModel: SavingsViewModel,
    lang: String,
    themeSurface: Color,
    themeText: Color
) {
    val contributionsList by viewModel.contributions.collectAsStateWithLifecycle()
    var isFormOpen by remember { mutableStateOf(false) }

    // Form inputs state
    var inputName by remember { mutableStateOf("") }
    var inputAmt by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Translations.get("contributions", lang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                        Text(
                            text = "${contributionsList.size} members recorded offline",
                            fontSize = 12.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )
                    }

                    // Large 48dp+ interactive Log Button
                    Button(
                        onClick = { isFormOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintPrimary,
                            contentColor = SolidBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_contribution_form_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Plus Icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("submit_contribution", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (contributionsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Empty state icon",
                                modifier = Modifier.size(60.dp),
                                tint = SageNeutral
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No contributions synced locally.", color = themeText.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                items(contributionsList) { cont ->
                    ContributionCard(cont, themeSurface, themeText)
                }
            }
        }

        // Custom contribution form floating dialog (GDPR and validation covered)
        if (isFormOpen) {
            Dialog(onDismissRequest = { isFormOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = Translations.get("submit_contribution", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )

                        Divider(color = SageNeutral)

                        // Input fields
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text(Translations.get("member_name", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contrib_form_name"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        OutlinedTextField(
                            value = inputAmt,
                            onValueChange = { inputAmt = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(Translations.get("amount", lang)) },
                            supportingText = {
                                val parsed = inputAmt.toDoubleOrNull() ?: 0.0
                                Text("Equivalent: MK " + String.format(Locale.US, "%,.0f", parsed * 1700), fontSize = 10.sp, color = MintPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contrib_form_amt"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        OutlinedTextField(
                            value = inputNotes,
                            onValueChange = { inputNotes = it },
                            label = { Text(Translations.get("notes", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contrib_form_notes"),
                            singleLine = false,
                            maxLines = 2,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        if (formError.isNotEmpty()) {
                            Text(formError, color = TerracottaWarn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isFormOpen = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("contrib_form_cancel"),
                                colors = ButtonDefaults.buttonColors(containerColor = SageNeutral)
                            ) {
                                Text("Cancel", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val amtValue = inputAmt.toDoubleOrNull()
                                    if (inputName.isBlank()) {
                                        formError = "Please enter member name"
                                    } else if (amtValue == null || amtValue <= 0) {
                                        formError = "Please enter valid contribution amount"
                                    } else {
                                        viewModel.recordContribution(inputName, amtValue, inputNotes)
                                        // Clear states
                                        inputName = ""
                                        inputAmt = ""
                                        inputNotes = ""
                                        formError = ""
                                        isFormOpen = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("contrib_form_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack)
                            ) {
                                Text("Record", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- 3. LOANS TAB SCREEN ---
@Composable
fun LoansTabScreen(
    viewModel: SavingsViewModel,
    lang: String,
    themeSurface: Color,
    themeText: Color
) {
    val loansList by viewModel.loans.collectAsStateWithLifecycle()
    var isRequestOpen by remember { mutableStateOf(false) }

    // Req Form states
    var reqName by remember { mutableStateOf("") }
    var reqAmt by remember { mutableStateOf("") }
    var reqInterest by remember { mutableStateOf("10") }
    var reqDuration by remember { mutableStateOf("3") }
    var reqNotes by remember { mutableStateOf("") }
    var reqError by remember { mutableStateOf("") }

    // Quick repayment state
    var selectedRepayLoanId by remember { mutableStateOf<Int?>(null) }
    var repayAmountInput by remember { mutableStateOf("") }
    var repayError by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Translations.get("loans", lang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "Standard 10% Interest Rate",
                            fontSize = 11.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )
                    }

                    Button(
                        onClick = { isRequestOpen = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = SolidBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("open_loan_req_form_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Translations.get("add_loan", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (loansList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active group loan records.", color = themeText.copy(alpha = 0.6f))
                    }
                }
            } else {
                items(loansList) { loan ->
                    LoanRecordRow(
                        loan = loan,
                        lang = lang,
                        themeSurface = themeSurface,
                        themeText = themeText,
                        onRepay = { selectedRepayLoanId = loan.id },
                        onToggleNotify = { viewModel.toggleLoanNotification(loan.id) },
                        onApprove = { viewModel.approveRequest(loan.id) },
                        onReject = { viewModel.rejectRequest(loan.id) }
                    )
                }
            }
        }

        // 3.1 REQUEST LOAN FORM
        if (isRequestOpen) {
            Dialog(onDismissRequest = { isRequestOpen = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = Translations.get("add_loan", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )

                        Divider(color = SageNeutral)

                        OutlinedTextField(
                            value = reqName,
                            onValueChange = { reqName = it },
                            label = { Text(Translations.get("member_name", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_name"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = reqAmt,
                            onValueChange = { reqAmt = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text(Translations.get("amount", lang)) },
                            supportingText = {
                                val parsed = reqAmt.toDoubleOrNull() ?: 0.0
                                Text("Equivalent: MK " + String.format(Locale.US, "%,.0f", parsed * 1700), fontSize = 10.sp, color = MintPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_amt"),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Predefined interest selector items
                            OutlinedTextField(
                                value = reqInterest,
                                onValueChange = { reqInterest = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text(Translations.get("apr_rate", lang) + " %") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_form_interest"),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = reqDuration,
                                onValueChange = { reqDuration = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Months") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_form_months"),
                                singleLine = true
                            )
                        }

                        // Calculate live payment report previews
                        val calculatedTotal = run {
                            val princVal = reqAmt.toDoubleOrNull() ?: 0.0
                            val intPercent = reqInterest.toDoubleOrNull() ?: 10.0
                            princVal * (1.0 + (intPercent / 100.0))
                        }

                        if (calculatedTotal > 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SageNeutral.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Estimated Repayment Total: MK ${String.format(Locale.US, "%,.0f", calculatedTotal * 1700)}",
                                    fontSize = 12.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reqNotes,
                            onValueChange = { reqNotes = it },
                            label = { Text(Translations.get("notes", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_notes"),
                            singleLine = false,
                            maxLines = 2
                        )

                        if (reqError.isNotEmpty()) {
                            Text(reqError, color = TerracottaWarn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isRequestOpen = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("loan_form_cancel"),
                                colors = ButtonDefaults.buttonColors(containerColor = SageNeutral)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val amtVal = reqAmt.toDoubleOrNull()
                                    val intRate = reqInterest.toDoubleOrNull() ?: 10.0
                                    val durMths = reqDuration.toIntOrNull() ?: 3
                                    if (reqName.isBlank()) {
                                        reqError = "Please specify member name"
                                    } else if (amtVal == null || amtVal <= 0.0) {
                                        reqError = "Please enter valid principal sum"
                                    } else {
                                        viewModel.submitLoanRequest(reqName, amtVal, intRate, durMths, reqNotes)
                                        // Reset states
                                        reqName = ""
                                        reqAmt = ""
                                        reqNotes = ""
                                        reqError = ""
                                        isRequestOpen = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("loan_form_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SolidBlack)
                            ) {
                                Text("Request", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3.2 QUICK HAND REPAYMENT FLOW ALERT DIALOG
        if (selectedRepayLoanId != null) {
            Dialog(onDismissRequest = { selectedRepayLoanId = null }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = themeSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Record Loan Repayment",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )

                        Text(
                            text = "Log instant offline cash received back into the group treasury.",
                            fontSize = 11.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )

                        OutlinedTextField(
                            value = repayAmountInput,
                            onValueChange = { repayAmountInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Payment Amount") },
                            supportingText = {
                                val parsed = repayAmountInput.toDoubleOrNull() ?: 0.0
                                Text("Equivalent: MK " + String.format(Locale.US, "%,.0f", parsed * 1700), fontSize = 10.sp, color = MintPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("repay_amount_input"),
                            singleLine = true
                        )

                        if (repayError.isNotEmpty()) {
                            Text(repayError, color = TerracottaWarn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    selectedRepayLoanId = null
                                    repayAmountInput = ""
                                    repayError = ""
                                },
                                modifier = Modifier.testTag("repay_cancel_btn")
                            ) {
                                Text("Cancel", color = LightSageText)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    val valAmt = repayAmountInput.toDoubleOrNull()
                                    if (valAmt == null || valAmt <= 0.0) {
                                        repayError = "Please write positive digit number"
                                    } else {
                                        viewModel.payLoanBill(selectedRepayLoanId!!, valAmt)
                                        // clear
                                        selectedRepayLoanId = null
                                        repayAmountInput = ""
                                        repayError = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack),
                                modifier = Modifier.testTag("repay_submit_btn")
                            ) {
                                Text("Record Pay", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- 4. SECURE BLOCK AUDIT LEDGER TAB SCREEN ---
@Composable
fun BackupTabScreen(
    viewModel: SavingsViewModel,
    lang: String,
    themeSurface: Color,
    themeText: Color
) {
    val transactionRecords by viewModel.transactionRecords.collectAsStateWithLifecycle()
    val backupHistory by viewModel.backups.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isBackupInProgress.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Backup controls area (End-to-End Cryptography illustrated)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface, RoundedCornerShape(16.dp))
                    .border(1.2.dp, MintPrimary, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Translations.get("backup_status", lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                        Text(
                            text = if (isSyncing) "Hashing and archiving logs..." else "AES-256 local ledger protection active",
                            fontSize = 11.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = GoldAccent,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = { viewModel.triggerCloudBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .testTag("cloud_backup_trigger")
                                .height(44.dp)
                        ) {
                            Text(Translations.get("trigger_backup", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Render latest backup summary
                backupHistory.firstOrNull()?.let { b ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SageNeutral.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cloud Archival SHA:", fontSize = 10.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                        Text(b.cloudSignature, fontSize = 10.sp, color = GoldAccent, fontFamily = FontFamily.Monospace)
                        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                        Text(fmt.format(Date(b.timestamp)), fontSize = 10.sp, color = LightSageText)
                    }
                }
            }
        }

        // Compliant audits item log header
        item {
            Text(
                text = Translations.get("logs", lang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = themeText
            )
        }

        if (transactionRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ledger record is empty. Enter contributions or loans to initialize.", color = themeText.copy(alpha = 0.6f))
                }
            }
        } else {
            items(transactionRecords) { rec ->
                CompliantRecordRow(rec, themeSurface, themeText)
            }
        }
    }
}


// --- 5. 24/7 AI CHAT ASSISTANT SUPPORT TAB ---
@Composable
fun ChatSupportTabScreen(
    viewModel: SavingsViewModel,
    lang: String,
    themeSurface: Color,
    themeText: Color
) {
    val messagesList by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var supportTextPrompt by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Assistant Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(8.dp))
                .background(themeSurface, RoundedCornerShape(8.dp))
                .border(0.5.dp, SageNeutral, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SageNeutral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Chuma Bot Logo",
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = Translations.get("chuma_greeting", lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                Text(
                    text = "Always accessible • Powered by Gemini AI",
                    fontSize = 10.sp,
                    color = themeText.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat logs bubble scroll area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messagesList) { msg ->
                    ChatBubbleItem(msg, themeSurface, themeText)
                }

                if (isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SageNeutral.copy(alpha = 0.4f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Chuma Bot is compiling advice...",
                                    fontSize = 12.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.W500
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large high-contrast touch sensory Chat Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = supportTextPrompt,
                onValueChange = { supportTextPrompt = it },
                placeholder = {
                    Text(
                        text = Translations.get("chat_placeholder", lang),
                        fontSize = 12.sp,
                        color = LightSageText.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ForestCard,
                    unfocusedContainerColor = ForestCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = LightSageText
                ),
                maxLines = 2
            )

            // Submit Send FAB with standard ripple
            IconButton(
                onClick = {
                    val promptToSend = supportTextPrompt
                    if (promptToSend.isNotBlank()) {
                        viewModel.sendSupportPrompt(promptToSend)
                        supportTextPrompt = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MintPrimary)
                    .testTag("chat_bubble_send_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send prompt button",
                    tint = SolidBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// --- SUPPORTIVE REUSABLE COMPONENT CARDS & VIEWS ---

@Composable
fun ContributionCard(
    contribution: Contribution,
    themeSurface: Color,
    themeText: Color
) {
    val dateString = run {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        formatter.format(Date(contribution.date))
    }

    val initials = if (contribution.memberName.isNotBlank()) {
        contribution.memberName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
    } else "M"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp))
            .background(themeSurface, RoundedCornerShape(20.dp))
            .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular initials avatar matching mockup SN/BK style
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8DEF8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color(0xFF1D192B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contribution.memberName,
                fontSize = 15.sp,
                color = if (themeText == LightSageText) Color.White else SolidBlack,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Date: $dateString",
                fontSize = 11.sp,
                color = themeText.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
            if (contribution.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${contribution.notes}",
                    fontSize = 11.sp,
                    color = MintPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "MK ${String.format(Locale.US, "%,.0f", contribution.amount * 1700)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MintPrimary
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(SageNeutral, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("SECURED", fontSize = 9.sp, color = LightSageText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoanRecordRow(
    loan: Loan,
    lang: String,
    themeSurface: Color,
    themeText: Color,
    onRepay: () -> Unit,
    onToggleNotify: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateString = run {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.format(Date(loan.dateRequested))
    }

    val totalDues = loan.totalRepaymentAmount
    val repaidAmount = loan.repaymentsPaid
    val remainingDues = loan.remainingAmount

    val initials = if (loan.memberName.isNotBlank()) {
        loan.memberName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
    } else "M"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(themeSurface, RoundedCornerShape(16.dp))
            .border(1.dp, if (loan.status == "Pending Approval") GoldAccent else SageNeutral, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular initials avatar matching mockup SN/BK style
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8DEF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color(0xFF1D192B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.memberName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (themeText == LightSageText) Color.White else SolidBlack
                )
                Text(text = "Requested: $dateString", fontSize = 11.sp, color = themeText.copy(alpha = 0.6f))
            }

            // High visible status label
            val badgeBg = when (loan.status) {
                "Approved" -> SageNeutral
                "Repaid" -> SageNeutral
                "Pending Approval" -> GoldAccent.copy(alpha = 0.2f)
                else -> TerracottaWarn.copy(alpha = 0.2f)
            }
            val badgeTextCol = when (loan.status) {
                "Approved" -> MintPrimary
                "Repaid" -> MintPrimary
                "Pending Approval" -> GoldAccent
                else -> TerracottaWarn
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = loan.status.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextCol
                )
            }
        }

        Divider(color = SageNeutral.copy(alpha = 0.4f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextColItem("Principal", "MK ${String.format(Locale.US, "%,.0f", loan.principalAmount * 1700)}", themeText)
            TextColItem("Interest Rate", "${loan.interestRatePercent}%", MintPrimary)
            TextColItem("Term", "${loan.repaymentDurationMonths} Months", themeText)
        }

        // Repayment meter ratio
        if (loan.status == "Approved" || loan.status == "Repaid") {
            val progressFactor = if (totalDues > 0) (repaidAmount / totalDues).toFloat() else 0.0f
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Paid: MK ${String.format(Locale.US, "%,.0f", repaidAmount * 1700)}", fontSize = 11.sp, color = themeText.copy(alpha = 0.6f))
                    Text("Remaining: MK ${String.format(Locale.US, "%,.0f", remainingDues * 1700)}", fontSize = 11.sp, color = MintPrimary, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = progressFactor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MintPrimary,
                    trackColor = SageNeutral
                )
            }
        }

        // Notifications reminder alerts toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggleNotify() }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Alert notifications toggle",
                    tint = if (loan.isReminderEnabled) MintPrimary else themeText.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (loan.isReminderEnabled) "Reminders Enabled" else "Self-Alerts Muted",
                    fontSize = 11.sp,
                    color = if (loan.isReminderEnabled) MintPrimary else themeText.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }

            // High interactive Action Panel options
            if (loan.status == "Pending Approval") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = SageNeutral),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("loan_reject_btn_${loan.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject", color = TerracottaWarn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SolidBlack),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("loan_approve_btn_${loan.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (loan.status == "Approved") {
                Button(
                    onClick = onRepay,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("repay_loan_action_${loan.id}")
                ) {
                    Text("Pay Repayment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CompliantRecordRow(
    record: TransactionRecord,
    themeSurface: Color,
    themeText: Color
) {
    val dateString = run {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        formatter.format(Date(record.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .background(themeSurface, RoundedCornerShape(12.dp))
            .border(1.dp, SageNeutral.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (record.type == "Contribution" || record.type == "Loan Repayment") MintPrimary else GoldAccent)
                )
                Text(
                    text = record.type.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record.type == "Contribution" || record.type == "Loan Repayment") MintPrimary else GoldAccent
                )
            }

            Text(
                text = "${if (record.type == "Contribution" || record.type == "Loan Repayment" || record.type == "Interest Payment") "+" else "-"}MK ${String.format(Locale.US, "%,.0f", record.amount * 1700)}",
                fontSize = 14.sp,
                color = if (record.type == "Contribution" || record.type == "Loan Repayment") MintPrimary else TerracottaWarn,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Text(
            text = record.description,
            fontSize = 12.sp,
            color = themeText,
            lineHeight = 16.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Audit Timestamp: $dateString", fontSize = 9.sp, color = themeText.copy(alpha = 0.6f))
            if (record.isEncrypted && record.hashSignature.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Block Signature",
                        tint = MintPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SHA: ${record.hashSignature}",
                        fontSize = 9.sp,
                        color = MintPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.W600
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: Message,
    themeSurface: Color,
    themeText: Color
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleBg = if (message.isUser) MintPrimary else SageNeutral
    val textColor = if (message.isUser) SolidBlack else LightSageText
    val roundShape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(roundShape)
                .background(bubbleBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.sp,
                color = textColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun IndicatorLegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 11.sp, color = LightSageText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TextColItem(label: String, valStr: String, col: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, color = LightSageText.copy(alpha = 0.6f))
        Text(text = valStr, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = col, modifier = Modifier.padding(top = 2.dp))
    }
}
