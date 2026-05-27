package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupSavingsDao {

    // --- Contributions ---
    @Query("SELECT * FROM contributions ORDER BY date DESC")
    fun getAllContributions(): Flow<List<Contribution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: Contribution): Long

    @Query("SELECT SUM(amount) FROM contributions")
    fun getTotalContributions(): Flow<Double?>

    // --- Loans ---
    @Query("SELECT * FROM loans ORDER BY dateRequested DESC")
    fun getAllLoans(): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Int): Loan?

    @Update
    suspend fun updateLoan(loan: Loan)

    @Query("SELECT SUM(principalAmount) FROM loans WHERE status = 'Approved'")
    fun getActiveLoansSum(): Flow<Double?>

    // --- Transaction Records ---
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactionRecords(): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionRecord(record: TransactionRecord): Long

    @Query("DELETE FROM contributions")
    suspend fun clearContributions()

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM transaction_records")
    suspend fun clearTransactionRecords()
}
