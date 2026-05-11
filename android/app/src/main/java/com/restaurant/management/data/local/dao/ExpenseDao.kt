package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Query(
        "SELECT IFNULL(SUM(amountCents), 0) FROM expenses WHERE createdAtEpochMillis >= :fromMillis AND createdAtEpochMillis < :toMillisExclusive",
    )
    suspend fun sumBetween(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): Int

    @Insert
    suspend fun insert(e: ExpenseEntity): Long

    @Delete
    suspend fun delete(e: ExpenseEntity)

    @Update
    suspend fun update(e: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
