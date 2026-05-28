package com.example.ui.theme

object Translations {
    private val en = mapOf(
        "app_title" to "YSL APP",
        "total_savings" to "Total Savings Capital",
        "active_loans" to "Active Disbursed Loans",
        "dashboard" to "Dashboard",
        "contributions" to "Contributions",
        "loans" to "Loans",
        "logs" to "Ledger Logs",
        "sync_backup" to "Backup",
        "chat_support" to "AI Support",
        "hello_member" to "Welcome back, Member!",
        "offline_notice" to "Offline-First Mode Active",
        "recent_activity" to "Recent Transactions",
        "savings_pool" to "Group Savings Pool",
        "enter_pin" to "Enter Security PIN to Unlock",
        "secure_locked" to "Uncompromising Bank Vault Shield",
        "pin_hint" to "Enter 4-Digit group code (Hint: 1234)",
        "finger_unlock" to "Biometric Passkey Entry",
        "submit_contribution" to "Log New Contribution",
        "member_name" to "Member Full Name",
        "amount" to "Amount",
        "notes" to "Purpose / Notes",
        "add_loan" to "Request Group Loan",
        "apr_rate" to "Interest Rate",
        "duration_months" to "Repayment Duration",
        "due_date" to "Maturity Due Date",
        "backup_status" to "Cloud Sync Server Backup",
        "trigger_backup" to "Sync to Encrypted Cloud",
        "chat_placeholder" to "Ask Chuma Bot about interest dues, dates...",
        "chuma_greeting" to "Chuma Bot 24/7 Group Consultant",
        "overdue" to "Overdue Alert",
        "fully_repaid" to "Repaid",
        "group_analytics" to "Group Capital Analytics",
        "add_record_success" to "Log successfully archived in local Room ledger.",
        "gdpr_compliance" to "GDPR Compliance & E2E Encrypted"
    )

    private val local = mapOf(
        "app_title" to "YSL APP",
        "total_savings" to "Ndalama Zonse Zosungidwa",
        "active_loans" to "Ngongole Zimene Zikugwira Ntchito",
        "dashboard" to "Mwanzo",
        "contributions" to "Zopereka",
        "loans" to "Ngongole",
        "logs" to "Zolembedwa",
        "sync_backup" to "Kusunga Mbiri",
        "chat_support" to "Thandizo la AI",
        "hello_member" to "Takulandirani Mwanachama!",
        "offline_notice" to "Popanda Intaneti (Offline)",
        "recent_activity" to "Zochitika Zaposachedwa",
        "savings_pool" to "Chikwama cha Matope Village",
        "enter_pin" to "Lowetsani PIN Yachitetezo",
        "secure_locked" to "Chitetezo Chokhazikika ca Vault",
        "pin_hint" to "Nambala ya gulu ya digit 4 (Mfundo: 1234)",
        "finger_unlock" to "Kugwiritsa Ntchito Chala",
        "submit_contribution" to "Lembani Zopereka Zatsopano",
        "member_name" to "Dzina Lathunthu la Mwanachama",
        "amount" to "Kuchuluka kwa Ndalama",
        "notes" to "Chifukwa / Zolemba",
        "add_loan" to "Funsani Ngongole ya Gulu",
        "apr_rate" to "Chiwongoladzanja",
        "duration_months" to "Nthawi Zobwezera",
        "due_date" to "Tsiku Lobwezera",
        "backup_status" to "Kusunga ku Mtambo Wotetezedwa",
        "trigger_backup" to "Sungani ku Mtambo",
        "chat_placeholder" to "Funsani Chuma Bot za chiwongoladzanja, masiku...",
        "chuma_greeting" to "Chuma Bot Wothandizira Gulu 24/7",
        "overdue" to "Dala! Nthawi Yadutsa",
        "fully_repaid" to "Imelipiridwa",
        "group_analytics" to "Thandizo la Ndalama Zachuma",
        "add_record_success" to "Zolemba zasungidwa bwino mu chipangizo chanu cha Room.",
        "gdpr_compliance" to "Zili mlandu ndi GDPR komanso Chitetezo Chokhazikika"
    )

    fun get(key: String, lang: String): String {
        return if (lang == "local") {
            local[key] ?: en[key] ?: key
        } else {
            en[key] ?: key
        }
    }
}
