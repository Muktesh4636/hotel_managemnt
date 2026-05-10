package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val onShift: Boolean,
    /** Monthly salary in paise (smallest INR unit), same as menu `priceCents`. */
    val salaryCents: Int = 0,
)
