package com.restaurant.management.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
@Immutable
data class MenuItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    /** Stored in paise (smallest INR unit); divide by 100 for rupees. */
    val priceCents: Int,
    val isAvailable: Boolean,
    /** App-private file path to a custom photo, or null to use default food images. */
    val customPhotoPath: String? = null,
)
