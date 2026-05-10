package com.restaurant.management.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity

data class OrderWithLines(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId",
        entity = OrderLineEntity::class,
    )
    val lines: List<OrderLineEntity>,
)
