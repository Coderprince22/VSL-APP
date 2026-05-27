package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Contribution::class, Loan::class, TransactionRecord::class],
    version = 1,
    exportSchema = false
)
abstract class GroupSavingsDatabase : RoomDatabase() {
    abstract fun savingsDao(): GroupSavingsDao

    companion object {
        @Volatile
        private var INSTANCE: GroupSavingsDatabase? = null

        fun getDatabase(context: Context): GroupSavingsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroupSavingsDatabase::class.java,
                    "village_savings_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
