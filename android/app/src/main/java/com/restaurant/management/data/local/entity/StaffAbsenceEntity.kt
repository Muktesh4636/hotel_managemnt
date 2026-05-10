package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_absences",
    indices = [
        Index(value = ["staffId", "dayStartEpochMillis"], unique = true),
    ],
)
data class StaffAbsenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    /** Start of that calendar day in the device default zone (epoch millis). */
    val dayStartEpochMillis: Long,
    val note: String?,
)
