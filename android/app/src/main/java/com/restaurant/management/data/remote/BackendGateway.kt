package com.restaurant.management.data.remote

import com.restaurant.management.data.local.AppDatabase
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.model.KitchenLineStatus
import com.restaurant.management.model.OrderStatus
import com.restaurant.management.model.TableStatus
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class BackendGateway(
    private val client: DjangoApiClient,
    private val db: AppDatabase,
) {
    suspend fun syncPull() {
        val json = client.get("/api/v1/sync/full/")
        VenueFullSync.applyFromJson(db, json)
    }

    suspend fun placeOrder(
        tableId: Long?,
        cart: Map<Long, Int>,
    ) {
        val lines = JSONArray()
        for ((menuItemId, qty) in cart) {
            if (qty <= 0) continue
            lines.put(
                JSONObject()
                    .put("menu_item_id", menuItemId)
                    .put("quantity", qty),
            )
        }
        val root = JSONObject().put("lines", lines)
        root.put("status", OrderStatus.OPEN)
        if (tableId != null) {
            root.put("table_id", tableId)
        }
        client.post("/api/v1/orders/", root.toString())
        syncPull()
    }

    /**
     * Creates a QR guest ticket on the server. Returns the server order id when the API
     * response includes it (DRF returns `id` on create).
     */
    suspend fun placeCustomerMenuOrder(cart: Map<Long, Int>): Long? {
        val lines = JSONArray()
        for ((menuItemId, qty) in cart) {
            if (qty <= 0) continue
            lines.put(
                JSONObject()
                    .put("menu_item_id", menuItemId)
                    .put("quantity", qty),
            )
        }
        val root =
            JSONObject()
                .put("lines", lines)
                .put("status", OrderStatus.IN_KITCHEN)
                .put("notes", "QR menu")
        val body = client.post("/api/v1/orders/", root.toString())
        val serverId =
            runCatching {
                val o = JSONObject(body)
                val id = o.optLong("id", -1L)
                if (id > 0L) id else null
            }.getOrNull()
        syncPull()
        return serverId
    }

    suspend fun sendOrderToKitchen(orderId: Long) {
        client.patch(
            "/api/v1/orders/$orderId/",
            JSONObject().put("status", OrderStatus.IN_KITCHEN).toString(),
        )
        syncPull()
    }

    suspend fun payOrder(orderId: Long) {
        val order = db.orderDao().getOrder(orderId)
        client.patch(
            "/api/v1/orders/$orderId/",
            JSONObject().put("status", OrderStatus.PAID).toString(),
        )
        val tid = order?.tableId
        if (tid != null) {
            runCatching {
                client.patch(
                    "/api/v1/tables/$tid/",
                    JSONObject().put("status", TableStatus.FREE).toString(),
                )
            }
        }
        syncPull()
    }

    suspend fun cancelOrder(orderId: Long) {
        val order = db.orderDao().getOrder(orderId)
        client.patch(
            "/api/v1/orders/$orderId/",
            JSONObject().put("status", OrderStatus.CANCELLED).toString(),
        )
        val tid = order?.tableId
        if (tid != null) {
            runCatching {
                client.patch(
                    "/api/v1/tables/$tid/",
                    JSONObject().put("status", TableStatus.FREE).toString(),
                )
            }
        }
        syncPull()
    }

    suspend fun advanceKitchenLine(lineId: Long) {
        val line = db.orderDao().getLine(lineId) ?: return
        val orderId = line.orderId
        val next =
            when (line.kitchenStatus) {
                KitchenLineStatus.QUEUED -> KitchenLineStatus.COOKING
                KitchenLineStatus.COOKING -> KitchenLineStatus.READY
                else -> KitchenLineStatus.READY
            }
        client.patch(
            "/api/v1/order-lines/$lineId/",
            JSONObject().put("kitchen_status", next).toString(),
        )
        syncPull()
        val all = db.orderDao().getLinesForOrder(orderId)
        if (all.isNotEmpty() && all.all { it.kitchenStatus == KitchenLineStatus.READY }) {
            client.patch(
                "/api/v1/orders/$orderId/",
                JSONObject().put("status", OrderStatus.READY).toString(),
            )
            syncPull()
        }
    }

    suspend fun createOrderLine(
        orderId: Long,
        menuItemId: Long,
        quantity: Int,
    ) {
        val body =
            JSONObject()
                .put("order_id", orderId)
                .put("menu_item_id", menuItemId)
                .put("quantity", quantity)
        client.post("/api/v1/order-lines/", body.toString())
        syncPull()
    }

    suspend fun patchOrderLineQuantity(
        lineId: Long,
        quantity: Int,
    ) {
        client.patch(
            "/api/v1/order-lines/$lineId/",
            JSONObject().put("quantity", quantity).toString(),
        )
        syncPull()
    }

    suspend fun deleteOrderLine(lineId: Long) {
        client.delete("/api/v1/order-lines/$lineId/")
        syncPull()
    }

    suspend fun markOrderServed(orderId: Long) {
        client.patch(
            "/api/v1/orders/$orderId/",
            JSONObject().put("status", OrderStatus.SERVED).toString(),
        )
        syncPull()
    }

    suspend fun setTableStatus(
        tableId: Long,
        status: String,
    ) {
        client.patch(
            "/api/v1/tables/$tableId/",
            JSONObject().put("status", status).toString(),
        )
        syncPull()
    }

    suspend fun setMenuItemAvailability(
        item: MenuItemEntity,
        available: Boolean,
    ) {
        client.patch(
            "/api/v1/menu-items/${item.id}/",
            JSONObject().put("is_available", available).toString(),
        )
        syncPull()
    }

    /** Creates a menu row on the server; does not run sync (caller runs sync after optional photo patch). */
    suspend fun createMenuItem(
        name: String,
        category: String,
        priceCents: Int,
    ): Long {
        val body =
            JSONObject()
                .put("name", name.trim())
                .put("category", category.trim().ifEmpty { "General" })
                .put("price", priceCents)
                .put("is_available", true)
                .put("custom_photo_url", "")
        val resp = client.post("/api/v1/menu-items/", body.toString())
        return JSONObject(resp).getLong("id")
    }

    suspend fun patchMenuItemCustomPhoto(
        menuItemId: Long,
        localFilePath: String,
    ) {
        val f = File(localFilePath)
        if (!f.isFile) {
            syncPull()
            return
        }
        val mediaType =
            when (f.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
        client.postMultipart(
            "/api/v1/menu-items/$menuItemId/photo/",
            "file",
            f,
            mediaType,
        )
        syncPull()
    }

    suspend fun updateMenuItem(
        item: MenuItemEntity,
        name: String,
        category: String,
        priceCents: Int,
        customPhotoUrl: String?,
        clearCustomPhoto: Boolean,
    ) {
        val o =
            JSONObject()
                .put("name", name.trim())
                .put("category", category.trim().ifEmpty { "General" })
                .put("price", priceCents)
        when {
            clearCustomPhoto -> o.put("custom_photo_url", "")
            customPhotoUrl != null -> o.put("custom_photo_url", customPhotoUrl)
        }
        client.patch("/api/v1/menu-items/${item.id}/", o.toString())
        syncPull()
    }

    suspend fun deleteMenuItem(item: MenuItemEntity) {
        client.delete("/api/v1/menu-items/${item.id}/")
        syncPull()
    }

    suspend fun updateInventory(item: InventoryEntity) {
        val body =
            JSONObject()
                .put("name", item.name)
                .put("quantity", item.quantity)
                .put("unit", item.unit)
                .put("low_stock_threshold", item.lowStockThreshold)
        client.patch("/api/v1/inventory/${item.id}/", body.toString())
        syncPull()
    }

    suspend fun deleteInventory(item: InventoryEntity) {
        client.delete("/api/v1/inventory/${item.id}/")
        syncPull()
    }

    suspend fun addInventoryItem(
        name: String,
        quantity: Double,
        unit: String,
        lowStockThreshold: Double,
    ) {
        val body =
            JSONObject()
                .put("name", name.trim())
                .put("quantity", quantity)
                .put("unit", unit.ifBlank { "units" })
                .put("low_stock_threshold", lowStockThreshold)
        client.post("/api/v1/inventory/", body.toString())
        syncPull()
    }

    suspend fun addExpense(
        expenseCategory: String,
        label: String,
        amountCents: Int,
        note: String?,
        createdAt: Long,
    ) {
        val body =
            JSONObject()
                .put("expense_category", expenseCategory.ifBlank { "General" })
                .put("label", label.ifBlank { expenseCategory })
                .put("amount_cents", amountCents)
                .put("created_at_epoch_millis", createdAt)
        if (!note.isNullOrBlank()) {
            body.put("note", note)
        }
        client.post("/api/v1/expenses/", body.toString())
        syncPull()
    }

    suspend fun deleteExpense(id: Long) {
        client.delete("/api/v1/expenses/$id/")
        syncPull()
    }

    suspend fun updateExpense(e: ExpenseEntity) {
        val body =
            JSONObject()
                .put("expense_category", e.expenseCategory.ifBlank { "General" })
                .put("label", e.label.ifBlank { e.expenseCategory })
                .put("amount_cents", e.amountCents)
                .put("created_at_epoch_millis", e.createdAtEpochMillis)
        if (!e.note.isNullOrBlank()) {
            body.put("note", e.note)
        } else {
            body.put("note", JSONObject.NULL)
        }
        client.patch("/api/v1/expenses/${e.id}/", body.toString())
        syncPull()
    }

    suspend fun setStaffShift(
        member: StaffEntity,
        onShift: Boolean,
    ) {
        client.patch(
            "/api/v1/staff/${member.id}/",
            JSONObject().put("on_shift", onShift).toString(),
        )
        syncPull()
    }

    suspend fun saveStaffSalary(member: StaffEntity) {
        client.patch(
            "/api/v1/staff/${member.id}/",
            JSONObject().put("salary_cents", member.salaryCents).toString(),
        )
        syncPull()
    }

    suspend fun addStaff(
        name: String,
        role: String,
    ) {
        val body =
            JSONObject()
                .put("name", name.trim())
                .put("role", role.trim().ifEmpty { "Staff" })
                .put("on_shift", false)
                .put("salary_cents", 0)
        client.post("/api/v1/staff/", body.toString())
        syncPull()
    }

    suspend fun deleteStaff(id: Long) {
        client.delete("/api/v1/staff/$id/")
        syncPull()
    }

    suspend fun addStaffAbsence(
        staffId: Long,
        dayStartEpochMillis: Long,
        note: String?,
    ) {
        val body =
            JSONObject()
                .put("staff_id", staffId)
                .put("day_start_epoch_millis", dayStartEpochMillis)
        if (!note.isNullOrBlank()) {
            body.put("note", note)
        }
        client.post("/api/v1/staff-absences/", body.toString())
        syncPull()
    }

    suspend fun deleteStaffAbsence(id: Long) {
        client.delete("/api/v1/staff-absences/$id/")
        syncPull()
    }

    suspend fun updateSettings(entity: AppSettingsEntity) {
        val body =
            JSONObject()
                .put("venue_name", entity.venueName)
                .put("tax_percent", entity.taxPercent)
                .put("service_charge_percent", entity.serviceChargePercent)
                .put("qr_menu_token", entity.qrMenuToken)
                .put("menu_categories", entity.menuCategories)
                .put("expense_categories", entity.expenseCategories)
                .put("modules_json", entity.modulesJson)
        client.put("/api/v1/settings/venue/", body.toString())
        syncPull()
    }

    companion object {
        fun fromPrefs(
            context: android.content.Context,
            db: AppDatabase,
        ): BackendGateway? {
            val prefs = ApiPrefs(context)
            if (!prefs.isConfigured()) return null
            return BackendGateway(
                DjangoApiClient(prefs.baseUrl, prefs.token),
                db,
            )
        }
    }
}
