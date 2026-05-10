package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guestName: String,
    val phone: String,
    val partySize: Int,
    val atEpochMillis: Long,
    val notes: String?,
)
