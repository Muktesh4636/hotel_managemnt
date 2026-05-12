package com.restaurant.management.data.local.entity

/**
 * Scan-to-order tickets (Android guest flow or web QR menu) that go straight to the kitchen queue.
 * These are hidden from the POS "Active orders" strip so staff handle them on Kitchen only.
 */
fun OrderEntity.isQrDirectKitchenOrder(): Boolean {
    val n = (notes ?: "").trim()
    return n == "QR menu" ||
        n.equals("QR guest web", ignoreCase = true) ||
        n.startsWith("QR guest", ignoreCase = true)
}
