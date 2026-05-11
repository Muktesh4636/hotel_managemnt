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
    val qrMenu: Boolean = true,
    val inventory: Boolean = true,
    val staff: Boolean = true,
    val expenses: Boolean = true,
    /** Optional modules (default off until enabled in Global settings). */
    val tablesFloor: Boolean = false,
    val reservations: Boolean = false,
    val suppliersPo: Boolean = false,
    val wasteLog: Boolean = false,
    val cashDrawer: Boolean = false,
    val customerFeedback: Boolean = false,
)

fun parseModulesJson(raw: String?): ModuleFlags {
    val s = raw?.trim().orEmpty()
    if (s.isEmpty()) return ModuleFlags()
    return try {
        val o = JSONObject(s)
        ModuleFlags(
            kitchen = o.optBoolean("kitchen", true),
            qrMenu = o.optBoolean("qrMenu", true),
            inventory = o.optBoolean("inventory", true),
            staff = o.optBoolean("staff", true),
            expenses = o.optBoolean("expenses", true),
            tablesFloor = o.optBoolean("tablesFloor", false),
            reservations = o.optBoolean("reservations", false),
            suppliersPo = o.optBoolean("suppliersPo", false),
            wasteLog = o.optBoolean("wasteLog", false),
            cashDrawer = o.optBoolean("cashDrawer", false),
            customerFeedback = o.optBoolean("customerFeedback", false),
        )
    } catch (_: Exception) {
        ModuleFlags()
    }
}

fun modulesToJson(flags: ModuleFlags): String =
    JSONObject().apply {
        put("kitchen", flags.kitchen)
        put("qrMenu", flags.qrMenu)
        put("inventory", flags.inventory)
        put("staff", flags.staff)
        put("expenses", flags.expenses)
        put("tablesFloor", flags.tablesFloor)
        put("reservations", flags.reservations)
        put("suppliersPo", flags.suppliersPo)
        put("wasteLog", flags.wasteLog)
        put("cashDrawer", flags.cashDrawer)
        put("customerFeedback", flags.customerFeedback)
    }.toString()

fun hubRouteEnabled(
    route: String,
    flags: ModuleFlags,
): Boolean =
    when (route) {
        Destinations.MENU_ADMIN,
        Destinations.SETTINGS,
        Destinations.REPORTS,
        Destinations.ORDERS,
        -> true
        Destinations.QR_MENU -> flags.qrMenu
        Destinations.INVENTORY -> flags.inventory
        Destinations.EXPENSES -> flags.expenses
        Destinations.STAFF -> flags.staff
        Destinations.TABLES_FLOOR -> flags.tablesFloor
        Destinations.RESERVATIONS -> flags.reservations
        Destinations.SUPPLIERS_PO -> flags.suppliersPo
        Destinations.WASTE_LOG -> flags.wasteLog
        Destinations.CASH_DRAWER -> flags.cashDrawer
        Destinations.CUSTOMER_FEEDBACK -> flags.customerFeedback
        else -> true
    }
