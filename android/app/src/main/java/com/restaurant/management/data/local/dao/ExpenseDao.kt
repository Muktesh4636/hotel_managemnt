package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.restaurant.management.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAtEpochMillis DESC LIMIT 500")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM expenses WHERE createdAtEpochMillis >= :fromEpochMillis",
    )
    fun observeExpenseTotalSince(fromEpochMillis: Long): Flow<Int>

    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Delete
    suspend fun delete(e: ExpenseEntity)
}
