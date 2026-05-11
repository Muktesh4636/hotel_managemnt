package com.restaurant.management.model

object TableStatus {
    const val FREE = "FREE"
    const val OCCUPIED = "OCCUPIED"
    const val NEEDS_CLEAN = "NEEDS_CLEAN"
}

object OrderStatus {
    const val OPEN = "OPEN"
    const val IN_KITCHEN = "IN_KITCHEN"
    const val READY = "READY"
    const val SERVED = "SERVED"
    const val PAID = "PAID"
    const val CANCELLED = "CANCELLED"
}

object KitchenLineStatus {
    const val QUEUED = "QUEUED"
    const val COOKING = "COOKING"
    const val READY = "READY"
}

/** Short English label for UI (stored values stay OPEN, CANCELLED, etc.). */
fun orderStatusLabel(status: String): String =
    when (status) {
        OrderStatus.OPEN -> "Open"
        OrderStatus.IN_KITCHEN -> "In kitchen"
        OrderStatus.READY -> "Ready"
        OrderStatus.SERVED -> "Served"
        OrderStatus.PAID -> "Paid"
        OrderStatus.CANCELLED -> "Cancelled"
        else -> status
    }
