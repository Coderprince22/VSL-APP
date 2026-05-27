package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contributions")
data class Contribution(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberName: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val groupId: String = "Main Village Group"
)

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberName: String,
    val principalAmount: Double,
    val interestRatePercent: Double = 10.0, // e.g. 10%
    val repaymentDurationMonths: Int = 3,
    val repaymentsPaid: Double = 0.0,
    val dateRequested: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000 * 3), // default 3 months
    val status: String = "Pending Approval", // "Pending Approval", "Approved", "Repaid", "Overdue"
    val notes: String = "",
    val isReminderEnabled: Boolean = true
) {
    val totalRepaymentAmount: Double
        get() = principalAmount * (1.0 + (interestRatePercent / 100.0))

    val remainingAmount: Double
        get() = (totalRepaymentAmount - repaymentsPaid).coerceAtLeast(0.0)
}

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Contribution", "Loan Disbursal", "Loan Repayment"
    val memberName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val isEncrypted: Boolean = true,
    val hashSignature: String = "" // cryptographic-like verification indicator
)
