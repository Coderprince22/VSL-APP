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

    suspend fun addContribution(memberName: String, amount: Double, notes: String, groupId: String = "Matope Village Bank") {
        val contribution = Contribution(
            memberName = memberName,
            amount = amount,
            notes = notes,
            groupId = groupId
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
            hashSignature = signature,
            groupId = groupId
        )
        dao.insertTransactionRecord(record)
    }

    suspend fun requestLoan(memberName: String, principalAmount: Double, interestPercent: Double, durationMonths: Int, notes: String, groupId: String = "Matope Village Bank") {
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
            notes = notes,
            groupId = groupId
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
            hashSignature = signature,
            groupId = groupId
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
                hashSignature = signature,
                groupId = loan.groupId
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
                hashSignature = signature,
                groupId = loan.groupId
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
                hashSignature = signature,
                groupId = loan.groupId
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

        // 1. Seed Matope Village Bank
        addContribution("Mariam Phiri", 250.00, "Monthly contributions for May", "Matope Village Bank")
        addContribution("Chiku Banda", 300.00, "May contributions and group tea fee", "Matope Village Bank")
        addContribution("John Mwiyo", 200.00, "Regular contribution", "Matope Village Bank")
        addContribution("Grace Chiume", 400.00, "Early contribution for June", "Matope Village Bank")
        requestLoan("Chiku Banda", 500.00, 10.0, 3, "For seed fertilizer purchase", "Matope Village Bank")
        requestLoan("Emily Mkandawire", 1000.00, 8.0, 4, "SME shop inventory expansion", "Matope Village Bank")

        // 2. Seed Chichiri Savings Group
        addContribution("Chimwemwe Mwale", 450.00, "Initial savings", "Chichiri Savings Group")
        addContribution("Limbani Gondwe", 350.00, "Group contribution", "Chichiri Savings Group")
        addContribution("Towela Nyirenda", 500.00, "May saving shares", "Chichiri Savings Group")
        requestLoan("Limbani Gondwe", 300.00, 12.0, 2, "School fees for children", "Chichiri Savings Group")

        // 3. Seed Zomba Community Fund
        addContribution("Yamikani Banda", 600.00, "Zomba share investment", "Zomba Community Fund")
        addContribution("Blessings Chimoyo", 800.00, "Large investment share", "Zomba Community Fund")
        addContribution("Chisomo Phiri", 400.00, "May regular share", "Zomba Community Fund")
        requestLoan("Chisomo Phiri", 200.00, 15.0, 1, "Water pipe installation", "Zomba Community Fund")
    }
}
