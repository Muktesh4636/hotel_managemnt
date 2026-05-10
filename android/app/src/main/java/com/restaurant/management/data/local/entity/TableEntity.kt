package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val section: String,
    val status: String,
)
