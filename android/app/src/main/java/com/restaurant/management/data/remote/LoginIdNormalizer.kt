package com.restaurant.management.data.remote

object LoginIdNormalizer {
    fun normalize(raw: String): String {
        val t = raw.trim()
        if (t.isEmpty()) return ""
        val digits = t.filter { it.isDigit() }
        val phoneLike =
            digits.length >= 10 &&
                t.all { ch ->
                    ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' || ch == '(' || ch == ')'
                }
        if (!phoneLike) return t.lowercase()
        // Match Django API: +91XXXXXXXXXX → 10-digit domestic (username in DB).
        if (digits.length == 12 && digits.startsWith("91")) return digits.drop(2)
        if (digits.length == 11 && digits.startsWith("0")) {
            val rest = digits.drop(1)
            if (rest.length == 10) return rest
        }
        return digits
    }
}
