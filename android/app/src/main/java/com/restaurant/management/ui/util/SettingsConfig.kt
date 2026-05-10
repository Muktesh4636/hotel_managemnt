package com.restaurant.management.ui.util

import com.restaurant.management.ui.Destinations
import org.json.JSONObject

/** Default menu categories when none are configured in settings. */
val DefaultMenuCategories =
    listOf("Starters", "Mains", "Desserts", "Drinks")

/** Default expense categories when none are configured in settings. */
val DefaultExpenseCategories =
    listOf(
        "Food & supplies",
        "Produce",
        "Utilities",
        "Rent & lease",
        "Water & beverages",
        "Equipment",
        "Other",
    )

fun resolvedMenuCategories(pipeSerialized: String?): List<String> =
    parsePipeCategoryList(pipeSerialized, DefaultMenuCategories)

fun resolvedExpenseCategories(pipeSerialized: String?): List<String> =
    parsePipeCategoryList(pipeSerialized, DefaultExpenseCategories)

fun parsePipeCategoryList(
    pipeSerialized: String?,
    defaults: List<String>,
): List<String> {
    val raw = pipeSerialized?.trim().orEmpty()
    if (raw.isEmpty()) return defaults
    val parsed =
        raw.split('|').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    return parsed.ifEmpty { defaults }
}

/** Multiline editor text ↔ pipe-separated DB value */
fun pipeListToMultiline(pipeSerialized: String?): String =
    parsePipeCategoryList(pipeSerialized, emptyList()).joinToString("\n")

fun multilineToPipeList(multiline: String): String =
    multiline
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString("|")

data class ModuleFlags(
    val kitchen: Boolean = true,
    val reservations: Boolean = true,
    val qrMenu: Boolean = true,
    val inventory: Boolean = true,
    val staff: Boolean = true,
    val reports: Boolean = true,
    val expenses: Boolean = true,
)

fun parseModulesJson(raw: String?): ModuleFlags {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return ModuleFlags()
    return try {
        val o = JSONObject(s)
        ModuleFlags(
            kitchen = o.optBoolean("kitchen", true),
            reservations = o.optBoolean("reservations", true),
            qrMenu = o.optBoolean("qrMenu", true),
            inventory = o.optBoolean("inventory", true),
            staff = o.optBoolean("staff", true),
            reports = o.optBoolean("reports", true),
            expenses = o.optBoolean("expenses", true),
        )
    } catch (_: Exception) {
        ModuleFlags()
    }
}

fun modulesToJson(flags: ModuleFlags): String =
    JSONObject().apply {
        put("kitchen", flags.kitchen)
        put("reservations", flags.reservations)
        put("qrMenu", flags.qrMenu)
        put("inventory", flags.inventory)
        put("staff", flags.staff)
        put("reports", flags.reports)
        put("expenses", flags.expenses)
    }.toString()

fun hubRouteEnabled(
    route: String,
    flags: ModuleFlags,
): Boolean =
    when (route) {
        Destinations.MENU_ADMIN,
        Destinations.SETTINGS,
        -> true
        Destinations.QR_MENU -> flags.qrMenu
        Destinations.INVENTORY -> flags.inventory
        Destinations.EXPENSES -> flags.expenses
        Destinations.RESERVATIONS -> flags.reservations
        Destinations.STAFF -> flags.staff
        Destinations.REPORTS -> flags.reports
        else -> true
    }
