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
        return if (phoneLike) digits else t.lowercase()
    }
}
