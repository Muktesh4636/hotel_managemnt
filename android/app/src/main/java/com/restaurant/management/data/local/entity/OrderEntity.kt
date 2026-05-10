package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("tableId")],
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long?,
    val status: String,
    val createdAtEpochMillis: Long,
    val totalCents: Int,
    val notes: String?,
)
