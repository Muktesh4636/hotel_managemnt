package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_reservations")
data class TableReservationEntity(
    @PrimaryKey val id: Long,
    val tableId: Long?,
    val guestName: String,
    val phone: String,
    val partySize: Int,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val status: String,
    val notes: String?,
    val createdAtEpochMillis: Long,
)
