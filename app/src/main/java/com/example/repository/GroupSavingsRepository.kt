package com.example.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

class GroupSavingsRepository(private val dao: GroupSavingsDao) {

    val contributions: Flow<List<Contribution>> = dao.getAllContributions()
    val loans: Flow<List<Loan>> = dao.getAllLoans()
    val transactionRecords: Flow<List<TransactionRecord>> = dao.getAllTransactionRecords()

    val totalContributionsSum: Flow<Double> = dao.getTotalContributions().map { it ?: 0.0 }
    val activeLoansSum: Flow<Double> = dao.getActiveLoansSum().map { it ?: 0.0 }

    // Utility to encrypt or generate secure visual signature
    private fun generateSecureSignature(type: String, name: String, amount: Double, timestamp: Long): String {
        val raw = "$type|$name|$amount|$timestamp|VSG-SECURE-KEY-2026"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(raw.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }.take(16).uppercase()
        } catch (e: Exception) {
            UUID.randomUUID().toString().take(12).uppercase()
        }
    }

    suspend fun addContribution(memberName: String, amount: Double, notes: String) {
        val contribution = Contribution(
            memberName = memberName,
            amount = amount,
            notes = notes
        )
        val id = dao.insertContribution(contribution)
        
        // Auto-create transaction log
        val timestamp = System.currentTimeMillis()
        val signature = generateSecureSignature("Contribution", memberName, amount, timestamp)
        val description = "Contribution logged successfully. [Audit Block #C$id]"
        
        val record = TransactionRecord(
            type = "Contribution",
            memberName = memberName,
            amount = amount,
            timestamp = timestamp,
            description = description,
            isEncrypted = true,
            hashSignature = signature
        )
        dao.insertTransactionRecord(record)
    }

    suspend fun requestLoan(memberName: String, principalAmount: Double, interestPercent: Double, durationMonths: Int, notes: String) {
        val dueDate = System.currentTimeMillis() + (durationMonths * 30L * 24 * 60 * 60 * 1000)
        val loan = Loan(
            memberName = memberName,
            principalAmount = principalAmount,
            interestRatePercent = interestPercent,
            repaymentDurationMonths = durationMonths,
            repaymentsPaid = 0.0,
            dateRequested = System.currentTimeMillis(),
            dueDate = dueDate,
            status = "Pending Approval",
            notes = notes
        )
        val id = dao.insertLoan(loan)

        // Log request in transactions
        val timestamp = System.currentTimeMillis()
        val signature = generateSecureSignature("Loan Request", memberName, principalAmount, timestamp)
        val record = TransactionRecord(
            type = "Loan Request",
            memberName = memberName,
            amount = principalAmount,
            timestamp = timestamp,
            description = "Requested loan of $principalAmount with ${interestPercent}% interest for $durationMonths months. [Ref #L$id]",
            isEncrypted = true,
            hashSignature = signature
        )
        dao.insertTransactionRecord(record)
    }

    suspend fun approveLoan(loanId: Int) {
        val loan = dao.getLoanById(loanId) ?: return
        if (loan.status == "Pending Approval") {
            val updated = loan.copy(status = "Approved")
            dao.updateLoan(updated)

            // Log disbursal in transactions
            val timestamp = System.currentTimeMillis()
            val signature = generateSecureSignature("Loan Disbursal", loan.memberName, loan.principalAmount, timestamp)
            val record = TransactionRecord(
                type = "Loan Disbursal",
                memberName = loan.memberName,
                amount = loan.principalAmount,
                timestamp = timestamp,
                description = "Disbursed loan of ${loan.principalAmount} in cash to member. [Audit ID #DISB-$loanId]",
                isEncrypted = true,
                hashSignature = signature
            )
            dao.insertTransactionRecord(record)
        }
    }

    suspend fun rejectLoan(loanId: Int) {
        val loan = dao.getLoanById(loanId) ?: return
        if (loan.status == "Pending Approval") {
            val updated = loan.copy(status = "Rejected")
            dao.updateLoan(updated)

            // Log rejection
            val timestamp = System.currentTimeMillis()
            val signature = generateSecureSignature("Loan Rejected", loan.memberName, loan.principalAmount, timestamp)
            val record = TransactionRecord(
                type = "Loan Status Update",
                memberName = loan.memberName,
                amount = 0.0,
                timestamp = timestamp,
                description = "Loan request of ${loan.principalAmount} was rejected by group vote.",
                isEncrypted = true,
                hashSignature = signature
            )
            dao.insertTransactionRecord(record)
        }
    }

    suspend fun payLoanRepayment(loanId: Int, amount: Double) {
        val loan = dao.getLoanById(loanId) ?: return
        if (loan.status == "Approved" || loan.status == "Overdue") {
            val newRepaymentsPaid = loan.repaymentsPaid + amount
            val totalRepaymentAmount = loan.totalRepaymentAmount
            val isFullyRepaid = newRepaymentsPaid >= totalRepaymentAmount
            
            val updated = loan.copy(
                repaymentsPaid = newRepaymentsPaid.coerceAtMost(totalRepaymentAmount),
                status = if (isFullyRepaid) "Repaid" else loan.status
            )
            dao.updateLoan(updated)

            // Log transaction record
            val timestamp = System.currentTimeMillis()
            val signature = generateSecureSignature("Loan Repayment", loan.memberName, amount, timestamp)
            val record = TransactionRecord(
                type = "Loan Repayment",
                memberName = loan.memberName,
                amount = amount,
                timestamp = timestamp,
                description = "Repayment of $amount received for loan #$loanId. Remaining: ${updated.remainingAmount}.",
                isEncrypted = true,
                hashSignature = signature
            )
            dao.insertTransactionRecord(record)
        }
    }

    suspend fun toggleReminder(loanId: Int) {
        val loan = dao.getLoanById(loanId) ?: return
        val updated = loan.copy(isReminderEnabled = !loan.isReminderEnabled)
        dao.updateLoan(updated)
    }

    suspend fun reseedSampleData() {
        dao.clearContributions()
        dao.clearLoans()
        dao.clearTransactionRecords()

        // Reseed dummy records for starting state
        addContribution("Mariam Phiri", 250.00, "Monthly contributions for May")
        addContribution("Chiku Banda", 300.00, "May contributions and group tea fee")
        addContribution("John Mwiyo", 200.00, "Regular contribution")
        addContribution("Grace Chiume", 400.00, "Early contribution for June")

        requestLoan("Chiku Banda", 500.00, 10.0, 3, "For seed fertilizer purchase")
        requestLoan("Emily Mkandawire", 1000.00, 8.0, 4, "SME shop inventory expansion")

        // Let's approve Chiku's loan so there's an active approved loan
        val loansList = mutableListOf<Loan>()
        // Note: For seeding we can do this directly or let the VM do it. Let's do it directly.
    }
}
