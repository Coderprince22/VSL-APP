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

sealed class AuthState {
    object Locked : AuthState()
    object Authenticating : AuthState()
    object Authenticated : AuthState()
    class Error(val message: String) : AuthState()
}

class SavingsViewModel(private val repository: GroupSavingsRepository) : ViewModel() {

    // --- State Listeners ---
    val contributions: StateFlow<List<Contribution>> = repository.contributions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<Loan>> = repository.loans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionRecords: StateFlow<List<TransactionRecord>> = repository.transactionRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavingsPool: StateFlow<Double> = repository.totalContributionsSum
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val activeLoansOut: StateFlow<Double> = repository.activeLoansSum
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
        return if (pin == "1234") {
            _authState.value = AuthState.Authenticated
            true
        } else {
            _authState.value = AuthState.Error("Incorrect Security PIN. Please try again or use Biometric entry.")
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
            Message("welcome-bot", "Hello! I am Chuma Bot, your 24/7 digital group savings assistant. Let me know if you have questions about interest calculations, group backing, or transactions!", false)
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
            repository.addContribution(name, amount, notes)
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
            repository.requestLoan(name, principal, interestPercent, months, notes)
        }
    }

    fun payLoanBill(loanId: Int, amt: Double) {
        viewModelScope.launch {
            repository.payLoanRepayment(loanId, amt)
        }
    }

    fun toggleLoanNotification(loanId: Int) {
        viewModelScope.launch {
            repository.toggleReminder(loanId)
        }
    }

    fun restoreSampleDatabase() {
        viewModelScope.launch {
            repository.reseedSampleData()
        }
    }

    init {
        // Seed some initial records if database is empty when initialized
        viewModelScope.launch {
            repository.contributions.first().let { list ->
                if (list.isEmpty()) {
                    repository.reseedSampleData()
                }
            }
        }
    }
}
