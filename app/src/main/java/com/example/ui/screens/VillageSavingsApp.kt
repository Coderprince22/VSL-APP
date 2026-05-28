package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.viewmodel.MemberDetails
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
                .verticalScroll(rememberScrollState())
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

            Spacer(modifier = Modifier.height(16.dp))

            // Pre-Login Village Bank / Group Selection (User Request: select group before entering password)
            val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestCard, RoundedCornerShape(16.dp))
                    .border(1.dp, SageNeutral.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT VILLAGE BANK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 0.5.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.availableBanks.forEach { bank ->
                        val isSelected = bank == activeBank
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MintPrimary else SageNeutral.copy(alpha = 0.4f))
                                .border(1.dp, if (isSelected) MintPrimary else SageNeutral, RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectBank(bank) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val displayName = when (bank) {
                                "Matope Village Bank" -> "Matope"
                                "Chichiri Savings Group" -> "Chichiri"
                                "Zomba Community Fund" -> "Zomba"
                                else -> bank
                            }
                            Text(
                                text = displayName,
                                color = if (isSelected) SolidBlack else LightSageText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = "Accessing: $activeBank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary,
                    textAlign = TextAlign.Center
                )
            }

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
    val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()

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
                            text = activeBank,
                            fontSize = 18.sp,
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
    val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val membersRegisteredList by viewModel.membersList.collectAsStateWithLifecycle()

    var isAddMemberOpen by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf("") }
    var newMemberParticulars by remember { mutableStateOf("") }
    var addMemberError by remember { mutableStateOf("") }

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

        item {
            val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface, RoundedCornerShape(16.dp))
                    .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ACTIVE VILLAGE BANK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        letterSpacing = 0.5.sp
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MintPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SECURE SESSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )
                    }
                }
                
                Text(
                    text = activeBank,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MintPrimary
                )
                
                Text(
                    text = "You are currently signed into this group's secure vault. To protect user privacy, other village bank records are locked. Tap the lock icon in the top right to switch bank groups.",
                    fontSize = 11.sp,
                    color = themeText.copy(alpha = 0.7f),
                    lineHeight = 15.sp
                )
            }
        }

        item {
            val membersList by viewModel.membersStats.collectAsStateWithLifecycle()
            val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
            var selectedMemberForDetail by remember { mutableStateOf<MemberDetails?>(null) }
            
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface, RoundedCornerShape(16.dp))
                    .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VILLAGE BANK MEMBERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentContext = LocalContext.current

                        Button(
                            onClick = { isAddMemberOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintPrimary,
                                contentColor = SolidBlack
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("add_member_button")
                        ) {
                            Text(
                                text = "+ Register",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Button(
                            onClick = {
                                PdfExporter.exportMembersSummaryPdf(currentContext, activeBank, membersList)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageNeutral.copy(alpha = 0.3f),
                                contentColor = MintPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Text(
                                text = "Export PDF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintPrimary
                            )
                        }
                        Text(
                            text = "Tap for Details",
                            fontSize = 10.sp,
                            color = themeText.copy(alpha = 0.5f)
                        )
                    }
                }
                
                if (membersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No registered members. Use 'Reseed' button below.",
                            color = themeText.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(membersList) { member ->
                            val initials = if (member.memberName.isNotBlank()) {
                                member.memberName.split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .map { it.first().uppercase() }
                                    .joinToString("")
                            } else "M"
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SageNeutral.copy(alpha = 0.2f))
                                    .clickable { selectedMemberForDetail = member }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MintPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        color = SolidBlack,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${member.id}. ${member.memberName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "MK " + String.format(Locale.US, "%,.0f", member.cumulativeContributions * 1700),
                                    fontSize = 9.sp,
                                    color = LightSageText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                selectedMemberForDetail?.let { member ->
                    val matchedRegMember = membersRegisteredList.find { it.name.equals(member.memberName, ignoreCase = true) }
                    val phone = matchedRegMember?.phoneNumber ?: ""
                    val particularsStr = matchedRegMember?.particulars ?: "No particulars listed in database."

                    AlertDialog(
                        onDismissRequest = { selectedMemberForDetail = null },
                        containerColor = ForestCard,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val initials = member.memberName.split(" ").filter { it.isNotBlank() }.take(2).map { it.first().uppercase() }.joinToString("")
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MintPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(initials, color = SolidBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text(
                                        text = "${member.id}. ${member.memberName}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = activeBank,
                                        fontSize = 11.sp,
                                        color = GoldAccent
                                    )
                                }
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Divider(color = SageNeutral, thickness = 0.5.dp)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cumulative Contributions", fontSize = 12.sp, color = LightSageText.copy(alpha = 0.8f))
                                    Text(
                                        "MK " + String.format(Locale.US, "%,.0f", member.cumulativeContributions * 1700),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintPrimary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Interest Paid Back", fontSize = 12.sp, color = LightSageText.copy(alpha = 0.8f))
                                    Text(
                                        "MK " + String.format(Locale.US, "%,.0f", member.interestPaid * 1700),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Outstanding Loans", fontSize = 12.sp, color = LightSageText.copy(alpha = 0.8f))
                                    Text(
                                        "MK " + String.format(Locale.US, "%,.0f", member.activeLoansRemaining * 1700),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaWarn
                                    )
                                }

                                Divider(color = SageNeutral, thickness = 0.5.dp)

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("REGISTERED PHONE", fontSize = 10.sp, color = LightSageText.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (phone.isNotBlank()) phone else "No phone registered",
                                        fontSize = 12.sp,
                                        color = if (phone.isNotBlank()) Color.White else Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("PARTICULARS", fontSize = 10.sp, color = LightSageText.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = particularsStr,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }

                                if (phone.isNotBlank()) {
                                    Divider(color = SageNeutral, thickness = 0.5.dp)
                                    Text("SEND SMS REMINDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val context = LocalContext.current
                                        if (member.activeLoansRemaining > 0) {
                                            Button(
                                                onClick = {
                                                    val unpaidLoanMk = member.activeLoansRemaining * 1700
                                                    val smsText = "Moni! Ichi ndi chikumbutso chochokera ku YSL APP chokhudza ngongole yanu yokwana MK ${String.format(Locale.US, "%,.0f", unpaidLoanMk)}. Chonde thandizani kubweza pamsonkhano wotsatira. Zikomo!"
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                        data = android.net.Uri.parse("smsto:$phone")
                                                        putExtra("sms_body", smsText)
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SageNeutral),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1.5f).height(30.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(11.dp), tint = MintPrimary)
                                                    Text("SMS Loan pay", fontSize = 8.sp, color = MintPrimary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val smsText = "Moni! Chikumbutso cholowa mu msonkhano wathu wotsatira wa gulu la YSL APP wogawana masheya ndi zokambirana zachuma. Chonde fikaniko pa nthawi. Zikomo!"
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                    data = android.net.Uri.parse("smsto:$phone")
                                                    putExtra("sms_body", smsText)
                                                }
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SageNeutral),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(11.dp), tint = GoldAccent)
                                                Text("SMS Meet", fontSize = 8.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Divider(color = SageNeutral, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SageNeutral.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("GRAND TOTAL", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Net Portfolio Share", fontSize = 9.sp, color = LightSageText.copy(alpha = 0.5f))
                                    }
                                    Text(
                                        "MK " + String.format(Locale.US, "%,.0f", member.grandTotalPortfolio * 1700),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MintPrimary
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { selectedMemberForDetail = null }
                            ) {
                                Text("Close", color = MintPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (isAddMemberOpen) {
                    AlertDialog(
                        onDismissRequest = { 
                            isAddMemberOpen = false 
                            newMemberName = ""
                            newMemberPhone = ""
                            newMemberParticulars = ""
                            addMemberError = ""
                        },
                        containerColor = ForestCard,
                        title = {
                            Text("Register New Gulu Member", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Enter the details of a new member to enroll them into this active group registry.",
                                    fontSize = 11.sp,
                                    color = LightSageText.copy(alpha = 0.7f)
                                )

                                OutlinedTextField(
                                    value = newMemberName,
                                    onValueChange = { newMemberName = it },
                                    label = { Text("Full Name", color = LightSageText) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SageNeutral.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = themeText
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("add_member_name_input")
                                )

                                OutlinedTextField(
                                    value = newMemberPhone,
                                    onValueChange = { newMemberPhone = it },
                                    label = { Text("Phone Number", color = LightSageText) },
                                    placeholder = { Text("e.g. +265 888 12 34 56", color = themeText.copy(alpha = 0.3f), fontSize = 11.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SageNeutral.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = themeText
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("add_member_phone_input")
                                )

                                OutlinedTextField(
                                    value = newMemberParticulars,
                                    onValueChange = { newMemberParticulars = it },
                                    label = { Text("Particulars & Details", color = LightSageText) },
                                    placeholder = { Text("Village, National ID, Next of Kin, etc.", color = themeText.copy(alpha = 0.3f), fontSize = 11.sp) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SageNeutral.copy(alpha = 0.2f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = themeText
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("add_member_particulars_input")
                                )

                                if (addMemberError.isNotEmpty()) {
                                    Text(addMemberError, color = TerracottaWarn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    isAddMemberOpen = false 
                                    newMemberName = ""
                                    newMemberPhone = ""
                                    newMemberParticulars = ""
                                    addMemberError = ""
                                }
                            ) {
                                Text("Cancel", color = LightSageText)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newMemberName.isBlank()) {
                                        addMemberError = "Name is required"
                                    } else {
                                        viewModel.addMember(newMemberName, newMemberPhone, newMemberParticulars)
                                        isAddMemberOpen = false
                                        newMemberName = ""
                                        newMemberPhone = ""
                                        newMemberParticulars = ""
                                        addMemberError = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack)
                            ) {
                                Text("Save Member", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
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
                    text = if (lang == "local") "Chikwama cha $activeBank" else "$activeBank Savings Pool",
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

        item {
            EmergencyFundCard(viewModel, themeSurface, themeText, lang)
        }

        item {
            ShareOutCalculatorCard(viewModel, themeSurface, themeText, lang)
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
    val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    var isFormOpen by remember { mutableStateOf(false) }

    // Form inputs state
    var inputName by remember { mutableStateOf("") }
    var inputAmt by remember { mutableStateOf("") }
    var inputNotes by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf("") }

    // VSLA specific entry states
    var entryByShares by remember { mutableStateOf(true) }
    var shareValueInput by remember { mutableStateOf("1000") } // Default K1000 per share
    var numberOfShares by remember { mutableIntStateOf(1) } // Default 1 share (Min 1, Max 5)
    var includeEmergencyFund by remember { mutableStateOf(true) } // Default true
    var emergencyFundInput by remember { mutableStateOf("500") } // Default K500 emergency fund fee

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
                            text = "$activeBank (${contributionsList.size} records)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent
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
                        .padding(8.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (lang == "local") "ZOPEREKA PAMSONKHANO" else "Record Meeting Transaction",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary
                        )

                        Divider(color = SageNeutral)

                        // Member name
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

                        // Entry Method Selector Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SageNeutral.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { entryByShares = true }
                                    .background(if (entryByShares) MintPrimary else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "By Shares (1-5)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entryByShares) SolidBlack else Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { entryByShares = false }
                                    .background(if (!entryByShares) MintPrimary else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Manual Amount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!entryByShares) SolidBlack else Color.White
                                )
                            }
                        }

                        if (entryByShares) {
                            // By Shares Input Section (VSLA standard rules)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = shareValueInput,
                                    onValueChange = { shareValueInput = it },
                                    label = { Text("Share Value (MK)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = themeText
                                    )
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = "Max Limit",
                                        fontSize = 10.sp,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "5 Shares Max",
                                        fontSize = 12.sp,
                                        color = themeText.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Share Selector Chips (1 to 5)
                            Text(
                                text = "Multiply Shares (Buy 1 to 5):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { num ->
                                    val isSelected = numberOfShares == num
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MintPrimary else SageNeutral)
                                            .border(1.dp, if (isSelected) GoldAccent else Color.Transparent, CircleShape)
                                            .clickable { numberOfShares = num },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$num",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) SolidBlack else Color.White
                                        )
                                    }
                                }
                            }

                            // Share Total Calculation Label
                            val computedTotalMk = (shareValueInput.toDoubleOrNull() ?: 1000.0) * numberOfShares
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MintPrimary.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Savings Total:", fontSize = 11.sp, color = LightSageText)
                                    Text(
                                        text = "MK " + String.format(Locale.US, "%,.0f", computedTotalMk),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintPrimary
                                    )
                                }
                            }

                            // Emergency Fund payment inline logic helper
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeEmergencyFund = !includeEmergencyFund }
                                    .background(SageNeutral.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = includeEmergencyFund,
                                    onCheckedChange = { includeEmergencyFund = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MintPrimary)
                                )
                                Column {
                                    Text(
                                        text = "Pay Emergency Fund (Thumba la Dzidzidzi)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Agreed Fee: MK $emergencyFundInput",
                                        fontSize = 10.sp,
                                        color = LightSageText.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            // Manual Entry method (allows simple Kwacha support)
                            OutlinedTextField(
                                value = inputAmt,
                                onValueChange = { inputAmt = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Contribution (MK amount)") },
                                supportingText = {
                                    val valMk = inputAmt.toDoubleOrNull() ?: 0.0
                                    val computedShares = valMk / (shareValueInput.toDoubleOrNull() ?: 1000.0)
                                    Text("Equates to: ${String.format(Locale.US, "%.2f", computedShares)} Shares", fontSize = 10.sp, color = MintPrimary)
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
                        }

                        // Custom Notes
                        OutlinedTextField(
                            value = inputNotes,
                            onValueChange = { inputNotes = it },
                            label = { Text("Optional Notes / Details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contrib_form_notes"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        if (formError.isNotEmpty()) {
                            Text(formError, color = TerracottaWarn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    val shareVal = shareValueInput.toDoubleOrNull() ?: 1000.0
                                    val mainAmtMk = if (entryByShares) {
                                        shareVal * numberOfShares
                                    } else {
                                        inputAmt.toDoubleOrNull() ?: 0.0
                                    }

                                    if (inputName.isBlank()) {
                                        formError = "Please enter member name"
                                    } else if (mainAmtMk <= 0) {
                                        formError = "Please enter valid savings amount / shares"
                                    } else if (entryByShares && (numberOfShares < 1 || numberOfShares > 5)) {
                                        formError = "VSLA rule: Buy min 1 and max 5 shares per meeting."
                                    } else {
                                        // Save Share savings
                                        val amountDbUnits = mainAmtMk / 1700.0
                                        viewModel.recordContribution(
                                            inputName,
                                            amountDbUnits,
                                            if (entryByShares) "Shares bought: $numberOfShares (K${String.format(Locale.US, "%,.0f", shareVal)}/each). $inputNotes" else "Manual transaction: MK $mainAmtMk. $inputNotes"
                                        )

                                        // Save Emergency Fund contribution if enabled
                                        if (entryByShares && includeEmergencyFund) {
                                            val emergencyMk = emergencyFundInput.toDoubleOrNull() ?: 500.0
                                            val emergencyDbUnits = emergencyMk / 1700.0
                                            viewModel.recordEmergencyContribution(
                                                inputName,
                                                emergencyDbUnits,
                                                "Emergency fund subscription MK $emergencyMk"
                                            )
                                        }

                                        // Reset fields
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
                                Text("Record Box", fontWeight = FontWeight.Bold)
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
    val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
    val membersList by viewModel.membersStats.collectAsStateWithLifecycle()
    var isRequestOpen by remember { mutableStateOf(false) }

    // Req Form states
    var reqName by remember { mutableStateOf("") }
    var reqAmt by remember { mutableStateOf("") }
    var reqInterest by remember { mutableStateOf("5") } // Default 5%
    var reqDurationIndex by remember { mutableIntStateOf(0) } // Default 0 = 2 Weeks
    var reqNotes by remember { mutableStateOf("") }
    var reqError by remember { mutableStateOf("") }

    // Quick repayment state
    var selectedRepayLoanId by remember { mutableStateOf<Int?>(null) }
    var repayAmountInput by remember { mutableStateOf("") }
    var repayError by remember { mutableStateOf("") }

    // Rollover loan state variables
    var selectedRolloverLoanId by remember { mutableStateOf<Int?>(null) }
    var rolloverInterestInput by remember { mutableStateOf("5") } // default 5% per VSLA guidelines
    var rolloverDurationIndex by remember { mutableIntStateOf(0) } // Default 0 = 2 Weeks

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
                            text = "$activeBank (${loansList.size} loans)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MintPrimary
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
                        onRollover = { selectedRolloverLoanId = loan.id },
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
                        .padding(8.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (lang == "local") "KUPEMPHA NGONGOLE" else "Request Group Loan (VSLA)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )

                        Divider(color = SageNeutral)

                        // Member name input with dynamic matching info
                        OutlinedTextField(
                            value = reqName,
                            onValueChange = { reqName = it },
                            label = { Text(Translations.get("member_name", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_name"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        // Fetch matching member to find savings and 3x limit
                        val trimmedName = reqName.trim()
                        val matchedMember = membersList.find { it.memberName.equals(trimmedName, ignoreCase = true) }
                        val memberSavingsMk = (matchedMember?.cumulativeContributions ?: 0.0) * 1700
                        val limit3xMk = memberSavingsMk * 3.0

                        if (trimmedName.isNotEmpty()) {
                            if (matchedMember != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MintPrimary.copy(alpha = 0.15f))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Matched Member: ${matchedMember.memberName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Total Share Savings: MK ${String.format(Locale.US, "%,.0f", memberSavingsMk)}",
                                            fontSize = 10.sp,
                                            color = LightSageText
                                        )
                                        Text(
                                            text = "Max Loan Limit (3x): MK ${String.format(Locale.US, "%,.0f", limit3xMk)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GoldAccent
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TerracottaWarn.copy(alpha = 0.12f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Name matches no existing records. (New members have MK 0 savings, limit: 3x shares).",
                                        fontSize = 10.sp,
                                        color = TerracottaWarn
                                    )
                                }
                            }
                        }

                        // Principal amount
                        OutlinedTextField(
                            value = reqAmt,
                            onValueChange = { reqAmt = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("Loan Principal (MK amount)") },
                            supportingText = {
                                val valMk = reqAmt.toDoubleOrNull() ?: 0.0
                                Text("Equivalent DB Units: ${String.format(Locale.US, "%.2f", valMk / 1700.0)}", fontSize = 10.sp, color = MintPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_amt"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        // Interest Rate and Duration Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = reqInterest,
                                onValueChange = { reqInterest = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Interest Rate %") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("loan_form_interest"),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = themeText
                                )
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "Standard Rate:",
                                    fontSize = 10.sp,
                                    color = LightSageText
                                )
                                Text(
                                    text = "5% Standard",
                                    fontSize = 12.sp,
                                    color = MintPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Term Duration Selector
                        Text(
                            text = "Loan Repayment Term / Period:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        val durLabels = listOf("2 Weeks", "1 Month", "2 Months", "3 Months")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            durLabels.forEachIndexed { index, label ->
                                val isSelected = reqDurationIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) GoldAccent else SageNeutral)
                                        .clickable { reqDurationIndex = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SolidBlack else Color.White
                                    )
                                }
                            }
                        }

                        // Repayment Estimate block
                        val reqAmtDouble = reqAmt.toDoubleOrNull() ?: 0.0
                        val reqInterestRate = reqInterest.toDoubleOrNull() ?: 5.0
                        val computedInterestMk = reqAmtDouble * (reqInterestRate / 100.0)
                        val totalEstimatedRepaymentMk = reqAmtDouble + computedInterestMk

                        if (reqAmtDouble > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SageNeutral.copy(alpha = 0.3f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Principal:", fontSize = 10.sp, color = LightSageText)
                                        Text("MK " + String.format(Locale.US, "%,.0f", reqAmtDouble), fontSize = 10.sp, color = Color.White)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Interest ($reqInterestRate%):", fontSize = 10.sp, color = LightSageText)
                                        Text("MK " + String.format(Locale.US, "%,.0f", computedInterestMk), fontSize = 10.sp, color = GoldAccent)
                                    }
                                    Divider(color = SageNeutral, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Estimated Repayment:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                        Text(
                                            text = "MK " + String.format(Locale.US, "%,.0f", totalEstimatedRepaymentMk),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Notes
                        OutlinedTextField(
                            value = reqNotes,
                            onValueChange = { reqNotes = it },
                            label = { Text("Application Notes / Guarantees") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("loan_form_notes"),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = themeText
                            )
                        )

                        // Strict limit error dynamic validation
                        val exceeds3x = matchedMember != null && reqAmtDouble > limit3xMk
                        if (exceeds3x) {
                            Text(
                                text = "⚠️ VSLA rule violation: Requested amount of MK ${String.format(Locale.US, "%,.0f", reqAmtDouble)} exceeds the member's 3x share savings limit of MK ${String.format(Locale.US, "%,.0f", limit3xMk)}.",
                                color = TerracottaWarn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (reqError.isNotEmpty()) {
                            Text(reqError, color = TerracottaWarn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                Text("Cancel", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val amtVal = reqAmt.toDoubleOrNull()
                                    val interestPercent = reqInterest.toDoubleOrNull() ?: 5.0
                                    val durationMonthsValue = if (reqDurationIndex == 0) 0 else if (reqDurationIndex == 1) 1 else if (reqDurationIndex == 2) 2 else 3

                                    if (reqName.isBlank()) {
                                        reqError = "Please specify member name"
                                    } else if (amtVal == null || amtVal <= 0.0) {
                                        reqError = "Please enter valid principal sum"
                                    } else if (matchedMember != null && amtVal > limit3xMk) {
                                        reqError = "Violates limit rules! Loan must be at most 3x savings."
                                    } else {
                                        // Submit loan using scaled values
                                        val amtDbValue = amtVal / 1700.0
                                        viewModel.submitLoanRequest(
                                            reqName,
                                            amtDbValue,
                                            interestPercent,
                                            durationMonthsValue,
                                            "Requested: $durLabels[$reqDurationIndex] term. $reqNotes"
                                        )

                                        // Reset
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
                                Text("Request Box", fontWeight = FontWeight.Bold)
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

        // 3.3 LOAN ROLLOVER / RENEWAL COMPLIANT DIALOG
        if (selectedRolloverLoanId != null) {
            val matchedLoan = loansList.find { it.id == selectedRolloverLoanId }
            Dialog(onDismissRequest = { selectedRolloverLoanId = null }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = themeSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(1.dp, SageNeutral, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Rollover Unpaid Loan Balance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )

                        if (matchedLoan != null) {
                            val unpaidBalanceMk = matchedLoan.remainingAmount * 1700
                            Text(
                                text = "Member: ${matchedLoan.memberName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TerracottaWarn.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "VSLA OVERDUE RULE COMPLIANCE:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaWarn
                                    )
                                    Text(
                                        text = "The remaining unpaid balance of MK ${String.format(Locale.US, "%,.0f", unpaidBalanceMk)} will become the new principal balance. A 5% rollover interest rate will be applied for the new period.",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Editable Rollover Interest Rate
                            OutlinedTextField(
                                value = rolloverInterestInput,
                                onValueChange = { rolloverInterestInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Rollover Interest Rate %") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = themeText
                                )
                            )

                            // Rollover Duration Choice
                            Text(
                                text = "Extended Term Period:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightSageText
                            )

                            val rolloverOptions = listOf("2 Weeks (Default)", "1 Month")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rolloverOptions.forEachIndexed { index, label ->
                                    val isSel = rolloverDurationIndex == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) MintPrimary else SageNeutral)
                                            .clickable { rolloverDurationIndex = index }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) SolidBlack else Color.White
                                        )
                                    }
                                }
                            }

                            // Math Preview for Rollover Loan
                            val rollInterestRate = rolloverInterestInput.toDoubleOrNull() ?: 5.0
                            val rollNewPrincipalMk = unpaidBalanceMk
                            val rollComputedInterestMk = rollNewPrincipalMk * (rollInterestRate / 100.0)
                            val rollNewTotalRepaymentMk = rollNewPrincipalMk + rollComputedInterestMk

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SageNeutral.copy(alpha = 0.2f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("New Rollover Principal:", fontSize = 10.sp, color = LightSageText)
                                        Text("MK " + String.format(Locale.US, "%,.0f", rollNewPrincipalMk), fontSize = 10.sp, color = Color.White)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("New Interest ($rollInterestRate%):", fontSize = 10.sp, color = LightSageText)
                                        Text("MK " + String.format(Locale.US, "%,.0f", rollComputedInterestMk), fontSize = 10.sp, color = GoldAccent)
                                    }
                                    Divider(color = SageNeutral, modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("New Total Repayment Obligation:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                                        Text(
                                            text = "MK " + String.format(Locale.US, "%,.0f", rollNewTotalRepaymentMk),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MintPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { selectedRolloverLoanId = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageNeutral)
                            ) {
                                Text("Cancel", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val rollInterestRate = rolloverInterestInput.toDoubleOrNull() ?: 5.0
                                    val extendedDays = if (rolloverDurationIndex == 0) 14 else 30
                                    viewModel.rolloverLoan(selectedRolloverLoanId!!, rollInterestRate, extendedDays)
                                    selectedRolloverLoanId = null
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack)
                            ) {
                                Text("Confirm Rollover", fontWeight = FontWeight.Bold)
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

        // --- GOOGLE DRIVE BACKUP & SYNCHRONIZATION CARD ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeSurface, RoundedCornerShape(16.dp))
                    .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Google Cloud Backup & Share",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = "Back up entire group ledger safely to your Google Drive",
                            fontSize = 11.sp,
                            color = themeText.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Google Sync",
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Divider(color = SageNeutral, thickness = 0.5.dp)

                Text(
                    text = "Sync group contributions, active loan schedules, and emergency welfare payouts directly. Generates a secure CSV audit document that you can instantly share and upload to your Google Drive account.",
                    fontSize = 11.sp,
                    color = LightSageText.copy(alpha = 0.8f)
                )

                val context = LocalContext.current
                val activeBank by viewModel.selectedBank.collectAsStateWithLifecycle()
                val totalSavings by viewModel.totalSavingsPool.collectAsStateWithLifecycle()
                val activeLoansSum by viewModel.activeLoansOut.collectAsStateWithLifecycle()
                val membersList by viewModel.membersStats.collectAsStateWithLifecycle()

                Button(
                    onClick = {
                        val csvBuilder = StringBuilder()
                        csvBuilder.append("YSL APP Gulu Register Backup\n")
                        csvBuilder.append("Group,${activeBank}\n")
                        csvBuilder.append("Total Savings Pool,MK ${totalSavings * 1700}\n")
                        csvBuilder.append("Total Outstanding Loans,MK ${activeLoansSum * 1700}\n\n")
                        csvBuilder.append("Member ID,Member Name,Contributions (MK),Interest Paid (MK),Loans Remaining (MK),Net Portfolio (MK)\n")
                        membersList.forEach { m ->
                            csvBuilder.append("${m.id},${m.memberName},${m.cumulativeContributions * 1700},${m.interestPaid * 1700},${m.activeLoansRemaining * 1700},${m.grandTotalPortfolio * 1700}\n")
                        }

                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Backup Google - YSL APP ($activeBank)")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Backup to Google Drive / Sheets")
                        context.startActivity(shareIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SolidBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Upload & Backup to Google Drive", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
    onRollover: () -> Unit,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (loan.remainingAmount > 0) {
                        Button(
                            onClick = onRollover,
                            colors = ButtonDefaults.buttonColors(containerColor = SageNeutral, contentColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("rollover_loan_action_${loan.id}")
                        ) {
                            Text("Rollover (5%)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

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

// --- COGNIZANT CARE VSLA EMERGENCY FUND LEDGER (THUMBA LA DZIDZIDZI) ---
@Composable
fun EmergencyFundCard(
    viewModel: SavingsViewModel,
    themeSurface: Color,
    themeText: Color,
    lang: String
) {
    val emergencyFundBalance by viewModel.emergencyFundBalance.collectAsStateWithLifecycle()
    val records by viewModel.transactionRecords.collectAsStateWithLifecycle()
    
    var isContributionDialogOpen by remember { mutableStateOf(false) }
    var isPayoutDialogOpen by remember { mutableStateOf(false) }
    
    val emergencyHistory = remember(records) {
        records.filter { it.type == "Emergency Contribution" || it.type == "Emergency Payout" }
    }
    
    val isChichewa = lang == "local"
    val cardTitle = if (isChichewa) "THUMBA LA DZIDZIDZI (EMERGENCY FUND)" else "EMERGENCY FUND (THUMBA LA DZIDZIDZI)"
    val balanceLabel = if (isChichewa) "Ndalama Zomwe Zilipo" else "Current Fund Pool"
    val contributeLabel = if (isChichewa) "Kusonkha" else "+ Contribute"
    val assistLabel = if (isChichewa) "Kuthandiza" else "Disburse Assistance"
    val historyLabel = if (isChichewa) "Mbiri ya Thumba la Dzidzidzi" else "Recent Emergency Transactions"
    val descriptionText = if (isChichewa) {
        "Ndalama zothandiza kuthana ndi mavuto adzidzidzi za thumba lapadera zisaikidwe limodzi ndizosunga zamashare. Imagwiritsidwa ntchito pothandiza pachipatala, pangozi, kapena maliro."
    } else {
        "Contingency fund holds specific assets set aside. These must not be mixed with regular shares/savings. Intended strictly for medical emergencies, disasters, or family bereavement."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeSurface, RoundedCornerShape(16.dp))
            .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Emergency Care Icon",
                    tint = TerracottaWarn,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = cardTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 0.5.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerracottaWarn.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isChichewa) "ZOTETEZEKA" else "CONTINGENCY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaWarn
                )
            }
        }
        
        Text(
            text = descriptionText,
            fontSize = 11.sp,
            color = themeText.copy(alpha = 0.7f),
            lineHeight = 15.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SageNeutral.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = balanceLabel.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeText.copy(alpha = 0.5f)
                )
                Text(
                    text = "MK " + String.format(Locale.US, "%,.0f", emergencyFundBalance * 1700),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MintPrimary
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isContributionDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintPrimary,
                        contentColor = SolidBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(contributeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { isPayoutDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerracottaWarn,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(assistLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (emergencyHistory.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = historyLabel.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                
                emergencyHistory.take(2).forEach { audit ->
                    val isPayout = audit.type == "Emergency Payout"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SageNeutral.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = audit.memberName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeText
                            )
                            Text(
                                text = if (isPayout) "Crisis Assistance Payout" else "Contraction Contribution",
                                fontSize = 9.sp,
                                color = themeText.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = (if (isPayout) "- " else "+ ") + "MK " + String.format(Locale.US, "%,.0f", audit.amount * 1700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPayout) TerracottaWarn else MintPrimary
                        )
                    }
                }
            }
        }
    }

    if (isContributionDialogOpen) {
        var nameInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { isContributionDialogOpen = false },
            containerColor = ForestCard,
            title = {
                Text(
                    text = if (isChichewa) "KUSONKHA CHIKWAMA" else "Log Emergency Contribution",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isChichewa) "Lembani m'mene mwanachama wasonka chinsinsi zopereka za dzidzidzi." else "Enter details for the emergency contingency reserve log entries.",
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.8f)
                    )
                    
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Member Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (in Unit / USD equivalent)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Purpose Details") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 10.0
                        if (nameInput.isNotBlank()) {
                            viewModel.recordEmergencyContribution(nameInput, amount, notesInput)
                            isContributionDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isContributionDialogOpen = false }) {
                    Text("Cancel", color = LightSageText)
                }
            }
        )
    }

    if (isPayoutDialogOpen) {
        var nameInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var notesInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { isPayoutDialogOpen = false },
            containerColor = ForestCard,
            title = {
                Text(
                    text = if (isChichewa) "KUTHANDIZA NKHANI YADZIDZIDZI" else "Request Emergency Assistance Payout",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isChichewa) "Ndondomeko yotulutsira ndalama za dzidzidzi kuthandizana pangozi." else "Disburse money from the emergency pool to aid a member in crisis.",
                        fontSize = 11.sp,
                        color = LightSageText.copy(alpha = 0.8f)
                    )
                    
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Recipient Member Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (in Unit / USD equivalent)") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Crisis Reason / Details") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = themeText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull() ?: 10.0
                        if (nameInput.isNotBlank()) {
                            viewModel.recordEmergencyPayout(nameInput, amount, notesInput)
                            isPayoutDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaWarn, contentColor = Color.White)
                ) {
                    Text("Disburse", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isPayoutDialogOpen = false }) {
                    Text("Cancel", color = LightSageText)
                }
            }
        )
    }
}

// --- COGNIZANT CARE VSLA END-OF-CYCLE DISTRIBUTION CALCULATOR (KUGAWANA NDALAMA) ---
@Composable
fun ShareOutCalculatorCard(
    viewModel: SavingsViewModel,
    themeSurface: Color,
    themeText: Color,
    lang: String
) {
    val totalSavings by viewModel.totalSavingsPool.collectAsStateWithLifecycle()
    val membersList by viewModel.membersStats.collectAsStateWithLifecycle()
    val emergencyFundBalance by viewModel.emergencyFundBalance.collectAsStateWithLifecycle()
    
    var isCalculatorOpen by remember { mutableStateOf(false) }
    var shareUnitInput by remember { mutableStateOf("1000") } // Agreed share value in MK, e.g. K1000
    
    val isChichewa = lang == "local"
    val title = if (isChichewa) "KUGAWANA NDALAMA (SHARE-OUT SIMULATOR)" else "YEAR-END SHARE-OUT (KUGAWANA NDALAMA)"
    val sub = if (isChichewa) {
        "Wamapando komanso mlembi amagawana ndalama zonse zamasheya ndi chiwongoladzanja pakutha pachaka choncho onse ogwirizana m'malamulo agawane moyenera."
    } else {
        "Perform year-end payouts plus dividends derived from loan interest under official CARE manual guidelines. Unpaid loan liabilities automatically settlement subtract."
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeSurface, RoundedCornerShape(16.dp))
            .border(0.5.dp, SageNeutral, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Share-out Calculator Icon",
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent,
                    letterSpacing = 0.5.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isChichewa) "KUGABA" else "DISTRIBUTION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }
        }
        
        Text(
            text = sub,
            fontSize = 11.sp,
            color = themeText.copy(alpha = 0.7f),
            lineHeight = 15.sp
        )

        Button(
            onClick = { isCalculatorOpen = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = SageNeutral,
                contentColor = GoldAccent
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_shareout_calculator"),
            contentPadding = PaddingValues(10.dp)
        ) {
            Text(
                text = if (isChichewa) "Tsegulani Mashini Ogawira ➜" else "Launch Divisible Shares Calculator ➜",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (isCalculatorOpen) {
        AlertDialog(
            onDismissRequest = { isCalculatorOpen = false },
            containerColor = ForestCard,
            title = {
                Text(
                    text = if (isChichewa) "MASHINI OGAWANA NDALAMA" else "Group Share-Out Calculator",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // Wrap in local Box to define a height limit for dialogue
                Box(modifier = Modifier.heightIn(max = 480.dp)) {
                    val shareValueKwacha = shareUnitInput.toDoubleOrNull() ?: 1000.0
                    val totalInterestPaidMk = membersList.sumOf { it.interestPaid } * 1700.0
                    val totalSavingsMk = totalSavings * 1700.0
                    
                    val totalSharesCount = if (shareValueKwacha > 0.0) totalSavingsMk / shareValueKwacha else 0.0
                    val totalEmergencyFundMk = emergencyFundBalance * 1700.0
                    val membersCount = membersList.size.coerceAtLeast(1)
                    val equalEmergencyPayoutPerMemberMk = totalEmergencyFundMk / membersCount

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Text(
                                text = if (isChichewa) {
                                    "Malinga ndi ndondomeko yogawana chuma, membala aliyense alandila: \n1) Ndalama ya m'thumba la Dzidzidzi (Yogawana Yofanana mkati mwa onse)\n2) Masheya onse ndi Chiwongoladzanja chomwe chapezeka (Gawani kufotokoza masheya ako)."
                                } else {
                                    "Based on official VSLA guidelines, distribution is computed as:\n• Cumulative personal share savings (returned fully)\n• Collected interest dividends (proportional to shares held)\n• Unused Emergency balance (shared strictly EQUALLY among all members)"
                                },
                                fontSize = 11.sp,
                                color = LightSageText.copy(alpha = 0.9f)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = shareUnitInput,
                                onValueChange = { shareUnitInput = it },
                                label = { Text("Agreed Value of 1 Share (MK)") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = SageNeutral.copy(alpha = 0.3f),
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = themeText
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SageNeutral.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Group Savings Pool (Shares)", fontSize = 11.sp, color = LightSageText)
                                    Text("MK " + String.format(Locale.US, "%,.0f", totalSavingsMk), fontSize = 11.sp, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Group Interest Pool (Collected)", fontSize = 11.sp, color = LightSageText)
                                    Text("MK " + String.format(Locale.US, "%,.0f", totalInterestPaidMk), fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining Emergency Pool", fontSize = 11.sp, color = LightSageText)
                                    Text("MK " + String.format(Locale.US, "%,.0f", totalEmergencyFundMk), fontSize = 11.sp, color = MintPrimary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Shares in Group", fontSize = 11.sp, color = LightSageText)
                                    Text(String.format(Locale.US, "%,.1f Shares", totalSharesCount), fontSize = 11.sp, color = Color.White)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Emergency Payout per Member", fontSize = 11.sp, color = LightSageText)
                                    Text("MK " + String.format(Locale.US, "%,.0f", equalEmergencyPayoutPerMemberMk), fontSize = 11.sp, color = MintPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Text(
                                text = "MEMBER BREAKDOWN REPORT:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (membersList.isEmpty()) {
                            item {
                                Text("No members to distribute to.", fontSize = 10.sp, color = LightSageText)
                            }
                        } else {
                            items(membersList) { member ->
                                val memberSavingsMk = member.cumulativeContributions * 1700.0
                                val memberShares = if (shareValueKwacha > 0.0) memberSavingsMk / shareValueKwacha else 0.0
                                
                                // Proportional Interest Dividend share
                                val proportionalInterestMk = if (totalSharesCount > 0.0) {
                                    (memberShares / totalSharesCount) * totalInterestPaidMk
                                } else 0.0
                                
                                // Total Gross: Personal Savings + Proportional Interest Dividend + Equal Emergency Reward
                                val grossPayoutMk = memberSavingsMk + proportionalInterestMk + equalEmergencyPayoutPerMemberMk
                                val unpaidLoanMk = member.activeLoansRemaining * 1700.0
                                val netpayoutMk = (grossPayoutMk - unpaidLoanMk).coerceAtLeast(0.0)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SageNeutral.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${member.id}. ${member.memberName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            "MK " + String.format(Locale.US, "%,.0f", netpayoutMk),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MintPrimary
                                        )
                                    }

                                    // Break down of details
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, top = 2.dp)
                                    ) {
                                        Text(
                                            "• Shares: ${String.format(Locale.US, "%.1f", memberShares)} (MK ${String.format(Locale.US, "%,.0f", memberSavingsMk)})",
                                            fontSize = 10.sp,
                                            color = LightSageText
                                        )
                                        Text(
                                            "• Proportional Interest Payout: MK ${String.format(Locale.US, "%,.0f", proportionalInterestMk)}",
                                            fontSize = 10.sp,
                                            color = GoldAccent
                                        )
                                        Text(
                                            "• Equal Welfare Emergency Payout: MK ${String.format(Locale.US, "%,.0f", equalEmergencyPayoutPerMemberMk)}",
                                            fontSize = 10.sp,
                                            color = MintPrimary
                                        )
                                        if (unpaidLoanMk > 0.0) {
                                            Text(
                                                "• Unpaid Loan Deduction: -MK ${String.format(Locale.US, "%,.0f", unpaidLoanMk)}",
                                                fontSize = 10.sp,
                                                color = TerracottaWarn,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            "• Gross Return: MK ${String.format(Locale.US, "%,.0f", grossPayoutMk)}",
                                            fontSize = 9.sp,
                                            color = LightSageText.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isCalculatorOpen = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary, contentColor = SolidBlack)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
