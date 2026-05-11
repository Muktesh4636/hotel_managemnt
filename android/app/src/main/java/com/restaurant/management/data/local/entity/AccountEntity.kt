package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["loginId"], unique = true)],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Normalized phone (digits) or lowercased username. */
    val loginId: String,
    val passwordSaltB64: String,
    val passwordHashB64: String,
    val createdAtEpochMillis: Long,
)
