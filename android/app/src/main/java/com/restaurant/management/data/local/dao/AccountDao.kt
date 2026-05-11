package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.restaurant.management.data.local.entity.AccountEntity

@Dao
interface AccountDao {
    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Query("SELECT * FROM accounts WHERE loginId = :loginId LIMIT 1")
    suspend fun getByLoginId(loginId: String): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long
}
