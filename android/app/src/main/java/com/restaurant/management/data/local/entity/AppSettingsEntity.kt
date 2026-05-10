package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val venueName: String,
    val taxPercent: Double,
    val serviceChargePercent: Double,
    /** Unique per install / venue — encoded in customer-menu QR deep link. */
    val qrMenuToken: String = "",
    /** Pipe-separated menu categories for POS / menu admin pickers. Empty → app defaults. */
    val menuCategories: String = "",
    /** Pipe-separated expense categories for the Expenses screen. Empty → app defaults. */
    val expenseCategories: String = "",
    /** JSON flags: kitchen, reservations, qrMenu, inventory, staff, reports, expenses. */
    val modulesJson: String = "",
)
