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

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountEntity?

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Query(
        "UPDATE accounts SET passwordSaltB64 = :saltB64, passwordHashB64 = :hashB64 WHERE id = :id",
    )
    suspend fun updatePasswordHashes(
        id: Long,
        saltB64: String,
        hashB64: String,
    )
}
