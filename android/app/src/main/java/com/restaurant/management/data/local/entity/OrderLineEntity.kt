package com.restaurant.management.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_lines",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MenuItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId"), Index("menuItemId")],
)
data class OrderLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val menuItemId: Long,
    val quantity: Int,
    val unitPriceCents: Int,
    val kitchenStatus: String,
)
