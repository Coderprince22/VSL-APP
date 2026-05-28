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
    val members: Flow<List<Member>> = dao.getAllMembers()

    suspend fun addMember(name: String, phoneNumber: String, particulars: String, groupId: String = "Matope Village Bank") {
        val member = Member(
            name = name,
            phoneNumber = phoneNumber,
            particulars = particulars,
            groupId = groupId
        )
        dao.insertMember(member)
    }

    suspend fun deleteMember(memberId: Int) {
        dao.deleteMemberById(memberId)
    }

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

    suspend fun addEmergencyContribution(memberName: String, amount: Double, notes: String, groupId: String = "Matope Village Bank") {
        val timestamp = System.currentTimeMillis()
        val signature = generateSecureSignature("Emergency Contribution", memberName, amount, timestamp)
        val record = TransactionRecord(
            type = "Emergency Contribution",
            memberName = memberName,
            amount = amount,
            timestamp = timestamp,
            description = "Contributed $amount to Emergency Fund (Thumba la Dzidzidzi). Notes: $notes",
            isEncrypted = true,
            hashSignature = signature,
            groupId = groupId
        )
        dao.insertTransactionRecord(record)
    }

    suspend fun addEmergencyPayout(memberName: String, amount: Double, notes: String, groupId: String = "Matope Village Bank") {
        val timestamp = System.currentTimeMillis()
        val signature = generateSecureSignature("Emergency Payout", memberName, amount, timestamp)
        val record = TransactionRecord(
            type = "Emergency Payout",
            memberName = memberName,
            amount = amount,
            timestamp = timestamp,
            description = "Disbursed $amount from Emergency Fund (Thumba la Dzidzidzi) for support. Notes: $notes",
            isEncrypted = true,
            hashSignature = signature,
            groupId = groupId
        )
        dao.insertTransactionRecord(record)
    }

    suspend fun requestLoan(memberName: String, principalAmount: Double, interestPercent: Double, durationMonths: Int, notes: String, groupId: String = "Matope Village Bank") {
        val durationDays = if (durationMonths <= 0) 14 else (durationMonths * 30)
        val dueDate = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)
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

    suspend fun rolloverLoan(loanId: Int, rolloverInterestRate: Double = 5.0, durationDays: Int = 14) {
        val loan = dao.getLoanById(loanId) ?: return
        if (loan.status == "Approved" || loan.status == "Overdue") {
            val unpaidAmount = loan.remainingAmount
            // The remaining amount (principal + interest minus any repayments) becomes the new principal.
            // Reset repaymentsPaid to 0.0 for the new rollover period.
            // Update the dueDate to +durationDays (e.g., 2 weeks = 14 days, or custom).
            val newDueDate = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)
            val updated = loan.copy(
                principalAmount = unpaidAmount,
                interestRatePercent = rolloverInterestRate,
                repaymentDurationMonths = if (durationDays == 14) 0 else (durationDays / 30),
                repaymentsPaid = 0.0,
                dueDate = newDueDate,
                status = "Approved",
                notes = "Loan rolled over. Previous unpaid dues of ${loan.remainingAmount} became the new principal with ${rolloverInterestRate}% interest."
            )
            dao.updateLoan(updated)

            // Log event in transactions
            val timestamp = System.currentTimeMillis()
            val signature = generateSecureSignature("Loan Rollover", loan.memberName, unpaidAmount, timestamp)
            val record = TransactionRecord(
                type = "Loan Rollover",
                memberName = loan.memberName,
                amount = unpaidAmount,
                timestamp = timestamp,
                description = "Loan #$loanId rolled over. New Principal: $unpaidAmount, Interest: ${rolloverInterestRate}%. Due in $durationDays days.",
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
        dao.clearMembers()

        // Seed Members for Matope Village Bank
        addMember("Mariam Phiri", "+265888123456", "Village: Matope, ID: MP-991, Next of Kin: John Phiri (Husband)", "Matope Village Bank")
        addMember("Chiku Banda", "+265999881122", "Village: Matope, ID: CB-842, Next of Kin: Sarah Banda (Sister)", "Matope Village Bank")
        addMember("John Mwiyo", "+265881442211", "Village: Matope, ID: JM-332, Next of Kin: Helen Mwiyo (Wife)", "Matope Village Bank")
        addMember("Grace Chiume", "+265882334455", "Village: Matope, ID: GC-411, Next of Kin: Frank Chiume (Brother)", "Matope Village Bank")
        addMember("Emily Mkandawire", "+265991234567", "Village: Matope, ID: EM-701, Next of Kin: Gift Mkandawire (Son)", "Matope Village Bank")

        // 1. Seed Matope Village Bank
        addContribution("Mariam Phiri", 250.00, "Monthly contributions for May", "Matope Village Bank")
        addContribution("Chiku Banda", 300.00, "May contributions and group tea fee", "Matope Village Bank")
        addContribution("John Mwiyo", 200.00, "Regular contribution", "Matope Village Bank")
        addContribution("Grace Chiume", 400.00, "Early contribution for June", "Matope Village Bank")
        requestLoan("Chiku Banda", 500.00, 10.0, 3, "For seed fertilizer purchase", "Matope Village Bank")
        requestLoan("Emily Mkandawire", 1000.00, 8.0, 4, "SME shop inventory expansion", "Matope Village Bank")
        
        // Seed Emergency Fund (Thumba la Dzidzidzi)
        addEmergencyContribution("Mariam Phiri", 50.00, "Yearly central reserve fee", "Matope Village Bank")
        addEmergencyContribution("Chiku Banda", 50.00, "May crisis reserve contribution", "Matope Village Bank")
        addEmergencyContribution("John Mwiyo", 50.00, "Emergency pool contribution", "Matope Village Bank")
        addEmergencyPayout("John Mwiyo", 30.00, "Medical clinic support fee", "Matope Village Bank")

        // 2. Seed Chichiri Savings Group
        addMember("Chimwemwe Mwale", "+265888252525", "Village: Chichiri, ID: CW-012, Kin: Linda Mwale", "Chichiri Savings Group")
        addMember("Limbani Gondwe", "+265999334411", "Village: Chichiri, ID: LG-505, Kin: Janet Gondwe", "Chichiri Savings Group")
        addMember("Towela Nyirenda", "+265881002299", "Village: Chichiri, ID: TN-311, Kin: Alice Nyirenda", "Chichiri Savings Group")

        addContribution("Chimwemwe Mwale", 450.00, "Initial savings", "Chichiri Savings Group")
        addContribution("Limbani Gondwe", 350.00, "Group contribution", "Chichiri Savings Group")
        addContribution("Towela Nyirenda", 500.00, "May saving shares", "Chichiri Savings Group")
        requestLoan("Limbani Gondwe", 300.00, 12.0, 2, "School fees for children", "Chichiri Savings Group")
        
        addEmergencyContribution("Chimwemwe Mwale", 60.00, "Chichiri emergency start pool", "Chichiri Savings Group")
        addEmergencyContribution("Towela Nyirenda", 60.00, "Emergency crisis help fee", "Chichiri Savings Group")

        // 3. Seed Zomba Community Fund
        addMember("Yamikani Banda", "+265882141414", "Village: Zomba, ID: YB-902, Kin: Chiza Banda", "Zomba Community Fund")
        addMember("Blessings Chimoyo", "+265995556677", "Village: Zomba, ID: BC-112, Kin: Maggie Chimoyo", "Zomba Community Fund")
        addMember("Chisomo Phiri", "+265884443322", "Village: Zomba, ID: CP-007, Kin: Peter Phiri", "Zomba Community Fund")

        addContribution("Yamikani Banda", 600.00, "Zomba share investment", "Zomba Community Fund")
        addContribution("Blessings Chimoyo", 800.00, "Large investment share", "Zomba Community Fund")
        addContribution("Chisomo Phiri", 400.00, "May regular share", "Zomba Community Fund")
        requestLoan("Chisomo Phiri", 200.00, 15.0, 1, "Water pipe installation", "Zomba Community Fund")
        
        addEmergencyContribution("Yamikani Banda", 80.00, "Zomba trust emergency start", "Zomba Community Fund")
        addEmergencyPayout("Blessings Chimoyo", 40.00, "Disaster roof repair assistance", "Zomba Community Fund")
    }
}
