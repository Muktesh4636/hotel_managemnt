package com.restaurant.management.ui.util

import java.text.NumberFormat
import java.util.Locale

/** Stored amounts are in smallest INR unit (paise); display uses Indian Rupees (₹). */
private val InrDisplayLocale = Locale.forLanguageTag("en-IN")

fun formatCents(cents: Int): String {
    val nf =
        NumberFormat.getCurrencyInstance(InrDisplayLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    return nf.format(cents / 100.0)
}

/** Plain rupees text for amount fields (e.g. `150` or `149.50`). */
fun centsToInrPlainInput(cents: Int): String {
    val paise = cents % 100
    if (paise == 0) return (cents / 100).toString()
    return String.format(Locale.US, "%.2f", cents / 100.0)
}
