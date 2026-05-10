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
