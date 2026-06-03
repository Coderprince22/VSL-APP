package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiContent
import com.example.data.api.GeminiPart
import com.example.repository.GroupSavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackupLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val fileSizeKb: Double,
    val status: String, // "Success", "Pending", "Failed"
    val cloudSignature: String
)

data class MemberDetails(
    val id: Int,
    val memberName: String,
    val cumulativeContributions: Double,
    val interestPaid: Double,
    val activeLoansRemaining: Double,
    val grandTotalPortfolio: Double
)

sealed class AuthState {
    object Locked : AuthState()
    object Authenticating : AuthState()
    object Authenticated : AuthState()
    class Error(val message: String) : AuthState()
}

class SavingsViewModel(
    private val repository: GroupSavingsRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("village_savings_prefs", android.content.Context.MODE_PRIVATE)

    // --- SharedPreferences Preferences State Flows ---
    private val _reminderTemplate = MutableStateFlow("")
    val reminderTemplate = _reminderTemplate.asStateFlow()

    private val _meetingInviteTemplate = MutableStateFlow("")
    val meetingInviteTemplate = _meetingInviteTemplate.asStateFlow()

    private val _defaultInterestRate = MutableStateFlow(10.0)
    val defaultInterestRate = _defaultInterestRate.asStateFlow()

    private val _maxLoanMultiplier = MutableStateFlow(3.0)
    val maxLoanMultiplier = _maxLoanMultiplier.asStateFlow()

    private val _customPin = MutableStateFlow("")
    val customPin = _customPin.asStateFlow()

    private val _availableBanks = MutableStateFlow<List<String>>(emptyList())
    val availableBanks: StateFlow<List<String>> = _availableBanks.asStateFlow()

    private fun loadPreferencesForGroup(bankName: String) {
        val defaultPinVal = when (bankName) {
            "Matope Village Bank" -> "1111"
            "Chichiri Savings Group" -> "2222"
            "Zomba Community Fund" -> "3333"
            else -> "1234"
        }
        
        _customPin.value = prefs.getString("pref_${bankName}_pin", defaultPinVal) ?: defaultPinVal
        
        val defaultReminder = "Moni [MEMBER_NAME]! Tikukumbutsani kuti muli ndi ngongole ku gulu la $bankName yokwana MK [AMOUNT] yomwe muyenera kulipira pofika tsiku la [DUE_DATE]. Zikomo!"
        _reminderTemplate.value = prefs.getString("pref_${bankName}_reminder", defaultReminder) ?: defaultReminder
        
        val defaultInvite = "Moni nonse m'gulu la $bankName! Mukukumbutsidwa za msonkhano wathu womwe udzachitike pa [DATE_TIME]. Chonde bwerani ndi zopereka zanu. Zikomo!"
        _meetingInviteTemplate.value = prefs.getString("pref_${bankName}_invite", defaultInvite) ?: defaultInvite
        
        _defaultInterestRate.value = prefs.getFloat("pref_${bankName}_interest", 10.0f).toDouble()
        _maxLoanMultiplier.value = prefs.getFloat("pref_${bankName}_multiplier", 3.0f).toDouble()
    }

    fun savePreferences(
        pin: String,
        reminder: String,
        invite: String,
        interest: Double,
        multiplier: Double
    ) {
        val bankName = _selectedBank.value
        prefs.edit().apply {
            putString("pref_${bankName}_pin", pin)
            putString("pref_${bankName}_reminder", reminder)
            putString("pref_${bankName}_invite", invite)
            putFloat("pref_${bankName}_interest", interest.toFloat())
            putFloat("pref_${bankName}_multiplier", multiplier.toFloat())
            apply()
        }
        loadPreferencesForGroup(bankName)
    }

    // --- All Raw Database State Listeners ---
    val allContributions: StateFlow<List<Contribution>> = repository.contributions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLoans: StateFlow<List<Loan>> = repository.loans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionRecords: StateFlow<List<TransactionRecord>> = repository.transactionRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Village Bank State Selection ---
    private val _selectedBank = MutableStateFlow("")
    val selectedBank: StateFlow<String> = _selectedBank.asStateFlow()

    fun addNewBankGroup(
        bankName: String,
        pin: String,
        interest: Double,
        multiplier: Double,
        adminName: String
    ) {
        val trimmed = bankName.trim()
        if (trimmed.isNotBlank() && !_availableBanks.value.contains(trimmed)) {
            val newList = _availableBanks.value + trimmed
            _availableBanks.value = newList
            prefs.edit().putString("custom_app_bank_list", newList.joinToString(";")).apply()

            val finalPin = if (pin.isNotBlank()) pin else "1234"
            val defaultReminder = "Moni [MEMBER_NAME]! Tikukumbutsani kuti muli ndi ngongole ku gulu la $trimmed yokwana MK [AMOUNT] yomwe muyenera kulipira pofika tsiku la [DUE_DATE]. Zikomo!"
            val defaultInvite = "Moni nonse m'gulu la $trimmed! Mukukumbutsidwa za msonkhano wathu womwe udzachitike pa [DATE_TIME]. Chonde bwerani ndi zopereka zanu. Zikomo!"

            prefs.edit().apply {
                putString("pref_${trimmed}_pin", finalPin)
                putString("pref_${trimmed}_reminder", defaultReminder)
                putString("pref_${trimmed}_invite", defaultInvite)
                putFloat("pref_${trimmed}_interest", interest.toFloat())
                putFloat("pref_${trimmed}_multiplier", multiplier.toFloat())
                apply()
            }

            if (adminName.isNotBlank()) {
                viewModelScope.launch {
                    repository.addMember(adminName.trim(), "", "Group Administrator (Creator)", trimmed)
                }
            }

            selectBank(trimmed)
        }
    }

    fun selectBank(bankName: String) {
        if (_selectedBank.value != bankName) {
            _selectedBank.value = bankName
            loadPreferencesForGroup(bankName)
            // Automatically lock the app so the new group PIN is required to open this group's secure vault
            _authState.value = AuthState.Locked
        }
    }

    // --- Filtered Reactively State Listeners ---
    val contributions: StateFlow<List<Contribution>> = combine(allContributions, _selectedBank) { list, bank ->
        list.filter { it.groupId == bank }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<Loan>> = combine(allLoans, _selectedBank) { list, bank ->
        list.filter { it.groupId == bank }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionRecords: StateFlow<List<TransactionRecord>> = combine(allTransactionRecords, _selectedBank) { list, bank ->
        list.filter { it.groupId == bank }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavingsPool: StateFlow<Double> = contributions.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeLoansOut: StateFlow<Double> = loans.map { list ->
        list.filter { it.status == "Approved" }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val emergencyFundBalance: StateFlow<Double> = transactionRecords.map { list ->
        val contribs = list.filter { it.type == "Emergency Contribution" }.sumOf { it.amount }
        val payouts = list.filter { it.type == "Emergency Payout" }.sumOf { it.amount }
        (contribs - payouts).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Member Statistics & Portfolio Details (Cumulative stats per member in active bank) ---
    val allRawMembers: StateFlow<List<Member>> = repository.members
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val membersList: StateFlow<List<Member>> = combine(allRawMembers, _selectedBank) { list, bank ->
        list.filter { it.groupId == bank }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val membersStats: StateFlow<List<MemberDetails>> = combine(contributions, loans, membersList) { contribs, loansList, regMembers ->
        val names = (contribs.map { it.memberName } + loansList.map { it.memberName } + regMembers.map { it.name })
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        names.mapIndexed { index, name ->
            val totalContributions = contribs.filter { it.memberName.equals(name, ignoreCase = true) }.sumOf { it.amount }
            val memberLoans = loansList.filter { it.memberName.equals(name, ignoreCase = true) }
            val totalInterestPaid = memberLoans.sumOf { loan ->
                val totalInterest = loan.principalAmount * (loan.interestRatePercent / 100.0)
                val totalDue = loan.totalRepaymentAmount
                if (totalDue > 0) {
                    (loan.repaymentsPaid / totalDue) * totalInterest
                } else {
                    0.0
                }
            }
            val remainingLoans = memberLoans.sumOf { it.remainingAmount }
            val grandTotal = (totalContributions + totalInterestPaid) - remainingLoans
            MemberDetails(
                id = index + 1,
                memberName = name,
                cumulativeContributions = totalContributions,
                interestPaid = totalInterestPaid,
                activeLoansRemaining = remainingLoans,
                grandTotalPortfolio = grandTotal
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMember(name: String, phone: String, particulars: String) {
        viewModelScope.launch {
            repository.addMember(name, phone, particulars, _selectedBank.value)
        }
    }

    fun deleteMember(memberId: Int) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
        }
    }

    fun updateMemberDetails(memberId: Int, name: String, phone: String, particulars: String) {
        viewModelScope.launch {
            repository.updateMember(memberId, name, phone, particulars, _selectedBank.value)
        }
    }

    fun getAILoanAssessment(memberName: String, requestAmount: Double, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val personalContributions = repository.contributions.first()
                .filter { it.groupId == _selectedBank.value && it.memberName.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount } * 1700.0
            
            val outstandingLoans = repository.loans.first()
                .filter { it.groupId == _selectedBank.value && it.memberName.equals(memberName, ignoreCase = true) && it.status == "Approved" }
                .sumOf { it.remainingAmount } * 1700.0

            val promptHistory = listOf(
                com.example.data.api.GeminiContent(
                    parts = listOf(
                        com.example.data.api.GeminiPart(
                            text = """
                                Evaluate a loan application for VSLA member $memberName.
                                Personal Savings (Cumulative Contributions): MK ${String.format(java.util.Locale.US, "%,.0f", personalContributions)}
                                Outstanding active loan balance: MK ${String.format(java.util.Locale.US, "%,.0f", outstandingLoans)}
                                Requested Loan Amount: MK ${String.format(java.util.Locale.US, "%,.0f", requestAmount)}
                                Limit: Maximum ${_maxLoanMultiplier.value}x times of savings is allowed.
                                
                                Provide a concise, highly professional 2-sentence assessment. 
                                Sentence 1: Creditworthiness check and ratio calculation versus cumulative savings.
                                Sentence 2: Clear recommendation (Approve/Reject) with risk warning or safety confirmation based on their portfolio.
                            """.trimIndent()
                        )
                    )
                )
            )
            val result = try {
                com.example.data.api.GeminiApiClient.generateSupportResponse(promptHistory)
            } catch (e: Exception) {
                ""
            }
            if (result.isNotBlank() && !result.contains("offline", ignoreCase = true) && !result.contains("error", ignoreCase = true)) {
                onResult(result)
            } else {
                val limitMk = personalContributions * _maxLoanMultiplier.value
                val isCompliant = requestAmount <= limitMk && outstandingLoans == 0.0
                val ratio = if (personalContributions > 0.0) requestAmount / personalContributions else _maxLoanMultiplier.value + 1.0
                val fallBackStr = if (isCompliant) {
                    "Khobili Advisor: $memberName is highly compliant with ${String.format(java.util.Locale.US, "%.1f", ratio)}x leverage on savings (Limit: ${_maxLoanMultiplier.value.toInt()}x). Having no other active debt, this loan is recommended for cash disbursal."
                } else {
                    "Khobili Alert: Requested MK ${String.format(java.util.Locale.US, "%,.0f", requestAmount)} is problematic. " +
                    (if (requestAmount > limitMk) "It exceeds the ${_maxLoanMultiplier.value.toInt()}x threshold by MK ${String.format(java.util.Locale.US, "%,.0f", requestAmount - limitMk)}. " else "") +
                    (if (outstandingLoans > 0.0) "The applicant has unpaid active group debt of MK ${String.format(java.util.Locale.US, "%,.0f", outstandingLoans)}. " else "") +
                    "Rejection is highly advised to avoid liquidity stress."
                }
                onResult(fallBackStr)
            }
        }
    }

    fun getAIMemberAudit(memberName: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val personalContributions = repository.contributions.first()
                .filter { it.groupId == _selectedBank.value && it.memberName.equals(memberName, ignoreCase = true) }
                .sumOf { it.amount } * 1700.0
            
            val unpaidLoans = repository.loans.first()
                .filter { it.groupId == _selectedBank.value && it.memberName.equals(memberName, ignoreCase = true) && it.status == "Approved" }
                .sumOf { it.remainingAmount } * 1700.0

            val promptHistory = listOf(
                com.example.data.api.GeminiContent(
                    parts = listOf(
                        com.example.data.api.GeminiPart(
                            text = """
                                Provide a brief financial audit of VSLA group member $memberName.
                                Total Accumulated Savings: MK ${String.format(java.util.Locale.US, "%,.0f", personalContributions)}
                                Unpaid Outstanding Loans: MK ${String.format(java.util.Locale.US, "%,.0f", unpaidLoans)}
                                Give a 2-sentence encouraging profile review. Assess saving consistency, credit risk, and give 1 specialized tip (e.g., about agribusiness inventory or fertilizer purchases) for Matope village banks.
                            """.trimIndent()
                        )
                    )
                )
            )
            val result = try {
                com.example.data.api.GeminiApiClient.generateSupportResponse(promptHistory)
            } catch (e: Exception) {
                ""
            }
            if (result.isNotBlank() && !result.contains("offline", ignoreCase = true) && !result.contains("error", ignoreCase = true)) {
                onResult(result)
            } else {
                val netWorth = personalContributions - unpaidLoans
                val advisorTip = if (unpaidLoans > 0) {
                    "Prioritize resolving the outstanding balance of MK ${String.format(java.util.Locale.US, "%,.0f", unpaidLoans)} before borrowing further to optimize share payouts."
                } else {
                    "Excellent portfolio with MK ${String.format(java.util.Locale.US, "%,.0f", personalContributions)} saved. Consider purchasing seed inventory early to hedge rising fertilizer prices."
                }
                onResult("Khobili Audit: $memberName has a net portfolio backing of MK ${String.format(java.util.Locale.US, "%,.0f", netWorth)}. $advisorTip")
            }
        }
    }

    // --- App Navigation State ---
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    // --- Language Translation States ("en" for English, "ch" for Chichewa/Swahili/Local) ---
    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
    }

    // --- Biometric & PIN Security States ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Locked)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun authenticateWithPin(pin: String): Boolean {
        val expectedPin = _customPin.value
        return if (pin == expectedPin) {
            _authState.value = AuthState.Authenticated
            true
        } else {
            _authState.value = AuthState.Error("Incorrect PIN for ${_selectedBank.value}.")
            false
        }
    }

    fun authenticateWithBiometric() {
        _authState.value = AuthState.Authenticating
        // Simulate local biometric check delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _authState.value = AuthState.Authenticated
        }
    }

    fun lockApp() {
        _authState.value = AuthState.Locked
    }

    // --- Dark & Light Mode States ---
    private val _isDarkMode = MutableStateFlow(true) // Start in cool dark accessibility mode
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // --- Backup & Cloud Sync States ---
    private val _backups = MutableStateFlow<List<BackupLog>>(
        listOf(
            BackupLog(
                timestamp = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                fileSizeKb = 142.6,
                status = "Success",
                cloudSignature = "VSG-BCK-A8E21D"
            )
        )
    )
    val backups: StateFlow<List<BackupLog>> = _backups.asStateFlow()

    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    fun triggerCloudBackup() {
        if (_isBackupInProgress.value) return
        _isBackupInProgress.value = true
        viewModelScope.launch {
            // Emulate secure end-to-end cloud serialization
            kotlinx.coroutines.delay(2000)
            val randomSig = "VSG-BCK-" + UUID.randomUUID().toString().take(6).uppercase()
            val newBackup = BackupLog(
                timestamp = System.currentTimeMillis(),
                fileSizeKb = 150.0 + (Random().nextDouble() * 20.0),
                status = "Success",
                cloudSignature = randomSig
            )
            _backups.value = listOf(newBackup) + _backups.value
            _isBackupInProgress.value = false
        }
    }

    // --- AI Chatbot States (24/7 Support with history) ---
    private val _chatMessages = MutableStateFlow<List<Message>>(
        listOf(
            Message("welcome-bot", "Hello! I am Khobili bot, your 24/7 digital group savings assistant. Let me know if you have questions about interest calculations, group backing, or transactions!", false)
        )
    )
    val chatMessages: StateFlow<List<Message>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendSupportPrompt(promptText: String) {
        if (promptText.isBlank()) return
        
        val userMsg = Message(text = promptText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        _isChatLoading.value = true
        viewModelScope.launch {
            // Format recent chat logs for Gemini API client
            val apiHistory = _chatMessages.value.takeLast(6).map { msg ->
                GeminiContent(
                    parts = listOf(GeminiPart(text = msg.text)),
                    role = if (msg.isUser) "user" else "model"
                )
            }

            val botResponseText = GeminiApiClient.generateSupportResponse(apiHistory)
            val botMsg = Message(text = botResponseText, isUser = false)
            _chatMessages.value = _chatMessages.value + botMsg
            _isChatLoading.value = false
        }
    }

    // --- Action Methods ---

    fun recordContribution(name: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addContribution(name, amount, notes, _selectedBank.value)
        }
    }

    fun recordEmergencyContribution(name: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addEmergencyContribution(name, amount, notes, _selectedBank.value)
        }
    }

    fun recordEmergencyPayout(name: String, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addEmergencyPayout(name, amount, notes, _selectedBank.value)
        }
    }

    fun approveRequest(loanId: Int) {
        viewModelScope.launch {
            repository.approveLoan(loanId)
        }
    }

    fun rejectRequest(loanId: Int) {
        viewModelScope.launch {
            repository.rejectLoan(loanId)
        }
    }

    fun submitLoanRequest(name: String, principal: Double, interestPercent: Double, months: Int, notes: String) {
        viewModelScope.launch {
            repository.requestLoan(name, principal, interestPercent, months, notes, _selectedBank.value)
        }
    }

    fun payLoanBill(loanId: Int, amt: Double) {
        viewModelScope.launch {
            repository.payLoanRepayment(loanId, amt)
        }
    }

    fun rolloverLoan(loanId: Int, rolloverInterestRate: Double = 5.0, durationDays: Int = 14) {
        viewModelScope.launch {
            repository.rolloverLoan(loanId, rolloverInterestRate, durationDays)
        }
    }

    fun toggleLoanNotification(loanId: Int) {
        viewModelScope.launch {
            repository.toggleReminder(loanId)
        }
    }

    fun restoreSampleDatabase() {
        viewModelScope.launch {
            repository.reseedSampleDataForGroup(_selectedBank.value)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearGroupData(_selectedBank.value)
        }
    }

    init {
        val defaultList = ""
        val storedString = prefs.getString("custom_app_bank_list", defaultList) ?: defaultList
        val list = storedString.split(";").filter { it.isNotBlank() }
        _availableBanks.value = list
        if (list.isNotEmpty()) {
            _selectedBank.value = list.first()
        } else {
            _selectedBank.value = ""
        }
        loadPreferencesForGroup(_selectedBank.value)
    }
}
