package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Category from global settings (e.g. Produce, Utilities). */
    val expenseCategory: String = "",
    val label: String,
    /** Amount in paise (smallest INR unit). */
    val amountCents: Int,
    val note: String?,
    val createdAtEpochMillis: Long,
)
