package com.restaurant.management.data

import android.content.Context
import android.net.Uri
import com.restaurant.management.data.local.AppDatabase
import com.restaurant.management.data.local.OrderWithLines
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import com.restaurant.management.data.local.entity.StaffAbsenceEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity
import com.restaurant.management.data.remote.BackendGateway
import com.restaurant.management.data.remote.DjangoApiClient
import com.restaurant.management.model.KitchenLineStatus
import com.restaurant.management.model.OrderStatus
import com.restaurant.management.model.TableStatus
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import java.util.UUID
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RestaurantRepository(
    private val db: AppDatabase,
    private val appContext: Context,
) {
    private val tables = db.tableDao()
    private val menu = db.menuDao()
    private val orders = db.orderDao()
    private val inventory = db.inventoryDao()
    private val expenses = db.expenseDao()
    private val staff = db.staffDao()
    private val staffAbsences = db.staffAbsenceDao()
    private val settings = db.settingsDao()

    private fun remoteGateway(): BackendGateway? = BackendGateway.fromPrefs(appContext, db)

    /** True for transport errors (offline, DNS, timeout). HTTP 4xx/5xx are not treated as offline. */
    private fun isNetworkFailure(e: Throwable): Boolean {
        var t: Throwable? = e
        while (t != null) {
            when (t) {
                is DjangoApiClient.ApiException -> return false
                is java.io.IOException -> return true
                else -> {}
            }
            t = t.cause
        }
        return false
    }

    /** Pulls latest venue snapshot from Django when URL + token are configured. */
    suspend fun backendSyncNow() {
        try {
            remoteGateway()?.syncPull()
        } catch (e: Exception) {
            if (!isNetworkFailure(e)) throw e
        }
    }

    fun observeTables(): Flow<List<TableEntity>> = tables.observeTables()

    fun observeMenu(): Flow<List<MenuItemEntity>> = menu.observeMenu()

    fun observeOpenOrders() = orders.observeOpenOrders()

    fun observeInventory(): Flow<List<InventoryEntity>> = inventory.observeInventory()

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenses.observeExpenses()

    fun observeStaff(): Flow<List<StaffEntity>> = staff.observeStaff()

    fun observeStaffAbsencesByStaff(): Flow<Map<Long, List<StaffAbsenceEntity>>> =
        staffAbsences.observeAll().map { rows ->
            rows
                .groupBy { it.staffId }
                .mapValues { (_, list) ->
                    list.sortedByDescending { it.dayStartEpochMillis }
                }
        }

    fun observeSettings(): Flow<AppSettingsEntity?> = settings.observeSettings()

    suspend fun getSettingsOnce(): AppSettingsEntity? = settings.getOnce()

    suspend fun ensureQrMenuToken() {
        val remote = remoteGateway()
        if (remote != null) {
            try {
                remote.syncPull()
                val cur = settings.getOnce() ?: return
                if (cur.qrMenuToken.isNotBlank()) return
                remote.updateSettings(cur.copy(qrMenuToken = UUID.randomUUID().toString()))
                return
            } catch (e: Exception) {
                if (!isNetworkFailure(e)) throw e
            }
        }
        val cur = settings.getOnce() ?: return
        if (cur.qrMenuToken.isNotBlank()) return
        settings.upsert(cur.copy(qrMenuToken = UUID.randomUUID().toString()))
    }

    suspend fun validateQrMenuToken(token: String): Boolean {
        val cur = settings.getOnce() ?: return false
        return cur.qrMenuToken.isNotBlank() && cur.qrMenuToken == token
    }

    fun observeDashboardToday(zone: ZoneId = ZoneId.systemDefault()): Flow<DashboardStats> {
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            orders.observePaidRevenueSince(start),
            orders.observeActiveOrderCount(),
            expenses.observeExpenseTotalSince(start),
            staff.observeStaff().map { list -> list.sumOf { it.salaryCents } },
        ) { revenueCents, activeOrders, todayExpenseCents, monthlySalaryCents ->
            val dailySalaryShareCents = monthlySalaryCents / 30
            val netProfitCents = revenueCents - todayExpenseCents - dailySalaryShareCents
            DashboardStats(
                todayNetProfitCents = netProfitCents,
                activeOrders = activeOrders,
            )
        }
    }

    suspend fun placeOrder(
        tableId: Long?,
        cart: Map<Long, Int>,
    ) {
        if (cart.isEmpty()) return
        val filtered =
            cart.filter { (menuItemId, qty) ->
                qty > 0 && (menu.getById(menuItemId)?.isAvailable == true)
            }
        if (filtered.isEmpty()) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.placeOrder(tableId, filtered)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        db.withTransaction {
            val created =
                OrderEntity(
                    tableId = tableId,
                    status = OrderStatus.OPEN,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    totalCents = 0,
                    notes = null,
                )
            val orderId = orders.insertOrder(created)
            var total = 0
            for ((menuItemId, qty) in filtered) {
                if (qty <= 0) continue
                val item = menu.getById(menuItemId) ?: continue
                if (!item.isAvailable) continue
                total += item.priceCents * qty
                orders.insertLine(
                    OrderLineEntity(
                        orderId = orderId,
                        menuItemId = menuItemId,
                        quantity = qty,
                        unitPriceCents = item.priceCents,
                        kitchenStatus = KitchenLineStatus.QUEUED,
                    ),
                )
            }
            val saved = orders.getOrder(orderId) ?: return@withTransaction
            orders.updateOrder(saved.copy(totalCents = total))
            if (tableId != null) {
                val table = tables.getById(tableId) ?: return@withTransaction
                tables.update(table.copy(status = TableStatus.OCCUPIED))
            }
        }
    }

    /** Guest orders from QR menu go straight to the kitchen queue. */
    suspend fun placeCustomerMenuOrder(cart: Map<Long, Int>) {
        if (cart.isEmpty()) return
        val filtered =
            cart.filter { (menuItemId, qty) ->
                qty > 0 && (menu.getById(menuItemId)?.isAvailable == true)
            }
        if (filtered.isEmpty()) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.placeCustomerMenuOrder(filtered)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        db.withTransaction {
            val created =
                OrderEntity(
                    tableId = null,
                    status = OrderStatus.IN_KITCHEN,
                    createdAtEpochMillis = System.currentTimeMillis(),
                    totalCents = 0,
                    notes = "QR menu",
                )
            val orderId = orders.insertOrder(created)
            var total = 0
            for ((menuItemId, qty) in filtered) {
                if (qty <= 0) continue
                val item = menu.getById(menuItemId) ?: continue
                if (!item.isAvailable) continue
                total += item.priceCents * qty
                orders.insertLine(
                    OrderLineEntity(
                        orderId = orderId,
                        menuItemId = menuItemId,
                        quantity = qty,
                        unitPriceCents = item.priceCents,
                        kitchenStatus = KitchenLineStatus.QUEUED,
                    ),
                )
            }
            val saved = orders.getOrder(orderId) ?: return@withTransaction
            orders.updateOrder(saved.copy(totalCents = total))
        }
    }

    suspend fun sendOrderToKitchen(orderId: Long) {
        val order = orders.getOrder(orderId) ?: return
        if (order.status == OrderStatus.CANCELLED || order.status == OrderStatus.PAID) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.sendOrderToKitchen(orderId)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        orders.updateOrder(order.copy(status = OrderStatus.IN_KITCHEN))
    }

    suspend fun advanceKitchenLine(lineId: Long) {
        val line = orders.getLine(lineId) ?: return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.advanceKitchenLine(lineId)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        val next =
            when (line.kitchenStatus) {
                KitchenLineStatus.QUEUED -> KitchenLineStatus.COOKING
                KitchenLineStatus.COOKING -> KitchenLineStatus.READY
                else -> KitchenLineStatus.READY
            }
        orders.updateLine(line.copy(kitchenStatus = next))
        val all = orders.getLinesForOrder(line.orderId)
        if (all.isNotEmpty() && all.all { it.kitchenStatus == KitchenLineStatus.READY }) {
            val order = orders.getOrder(line.orderId) ?: return
            if (order.status != OrderStatus.PAID && order.status != OrderStatus.CANCELLED) {
                orders.updateOrder(order.copy(status = OrderStatus.READY))
            }
        }
    }

    suspend fun markOrderServed(orderId: Long) {
        val order = orders.getOrder(orderId) ?: return
        if (order.status == OrderStatus.PAID || order.status == OrderStatus.CANCELLED) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.markOrderServed(orderId)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        orders.updateOrder(order.copy(status = OrderStatus.SERVED))
    }

    suspend fun payOrder(orderId: Long) {
        val remote = remoteGateway()
        if (remote != null) {
            val order = orders.getOrder(orderId) ?: return
            if (order.status == OrderStatus.CANCELLED) return
            val remoteOk =
                runCatching {
                    remote.payOrder(orderId)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        db.withTransaction {
            val order = orders.getOrder(orderId) ?: return@withTransaction
            if (order.status == OrderStatus.CANCELLED) return@withTransaction
            orders.updateOrder(order.copy(status = OrderStatus.PAID))
            val tableId = order.tableId
            if (tableId != null) {
                val table = tables.getById(tableId)
                if (table != null) {
                    tables.update(table.copy(status = TableStatus.FREE))
                }
            }
        }
    }

    suspend fun cancelOrder(orderId: Long) {
        val remote = remoteGateway()
        if (remote != null) {
            val order = orders.getOrder(orderId) ?: return
            if (order.status == OrderStatus.PAID) return
            val remoteOk =
                runCatching {
                    remote.cancelOrder(orderId)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        db.withTransaction {
            val order = orders.getOrder(orderId) ?: return@withTransaction
            if (order.status == OrderStatus.PAID) return@withTransaction
            orders.updateOrder(order.copy(status = OrderStatus.CANCELLED))
            val tableId = order.tableId
            if (tableId != null) {
                val table = tables.getById(tableId)
                if (table != null && table.status == TableStatus.OCCUPIED) {
                    tables.update(table.copy(status = TableStatus.FREE))
                }
            }
        }
    }

    /**
     * Removes the order and its lines from the local database (order history, kitchen, POS).
     * Frees the table if it was still marked occupied for this ticket (same idea as cancel).
     * There is no backend hard-delete API yet; if the server is connected, a later full sync may restore this order.
     */
    suspend fun deleteOrderPermanently(orderId: Long) {
        db.withTransaction {
            val order = orders.getOrder(orderId) ?: return@withTransaction
            val tableId = order.tableId
            if (tableId != null) {
                val table = tables.getById(tableId)
                if (table != null && table.status == TableStatus.OCCUPIED) {
                    tables.update(table.copy(status = TableStatus.FREE))
                }
            }
            orders.deleteOrderById(orderId)
        }
    }

    /** Cancelled tickets cannot be edited. Paid orders can still be corrected in order history (totals recomputed). */
    suspend fun canEditOrderItems(orderId: Long): Boolean {
        val order = orders.getOrder(orderId) ?: return false
        return order.status != OrderStatus.CANCELLED
    }

    private suspend fun recomputeOrderTotalCents(orderId: Long) {
        val lines = orders.getLinesForOrder(orderId)
        val total = lines.sumOf { it.quantity * it.unitPriceCents }
        val o = orders.getOrder(orderId) ?: return
        orders.updateOrder(o.copy(totalCents = total))
    }

    /** Add qty to an existing line for the same menu item, or insert a new line (local DB only). */
    suspend fun addOrIncrementOrderLine(
        orderId: Long,
        menuItemId: Long,
        addQty: Int,
    ) {
        if (addQty <= 0) return
        val remote = remoteGateway()
        if (remote != null) return
        db.withTransaction {
            val order = orders.getOrder(orderId) ?: return@withTransaction
            if (order.status == OrderStatus.CANCELLED) return@withTransaction
            val item = menu.getById(menuItemId) ?: return@withTransaction
            if (!item.isAvailable) return@withTransaction
            val existing =
                orders.getLinesForOrder(orderId).firstOrNull { it.menuItemId == menuItemId }
            if (existing != null) {
                orders.updateLine(existing.copy(quantity = existing.quantity + addQty))
            } else {
                orders.insertLine(
                    OrderLineEntity(
                        orderId = orderId,
                        menuItemId = menuItemId,
                        quantity = addQty,
                        unitPriceCents = item.priceCents,
                        kitchenStatus = KitchenLineStatus.QUEUED,
                    ),
                )
            }
            recomputeOrderTotalCents(orderId)
        }
    }

    /** Set line quantity; [newQuantity] <= 0 removes the line. */
    suspend fun setOrderLineQuantity(
        orderId: Long,
        lineId: Long,
        newQuantity: Int,
    ) {
        val remote = remoteGateway()
        if (remote != null) return
        db.withTransaction {
            val order = orders.getOrder(orderId) ?: return@withTransaction
            if (order.status == OrderStatus.CANCELLED) return@withTransaction
            val line = orders.getLine(lineId) ?: return@withTransaction
            if (line.orderId != orderId) return@withTransaction
            if (newQuantity <= 0) {
                orders.deleteLine(line)
            } else {
                orders.updateLine(line.copy(quantity = newQuantity))
            }
            recomputeOrderTotalCents(orderId)
        }
    }

    suspend fun setTableStatus(
        tableId: Long,
        status: String,
    ) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.setTableStatus(tableId, status)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        val table = tables.getById(tableId) ?: return
        tables.update(table.copy(status = status))
    }

    suspend fun setMenuItemAvailability(
        item: MenuItemEntity,
        available: Boolean,
    ) {
        if (item.isAvailable == available) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.setMenuItemAvailability(item, available)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        menu.update(item.copy(isAvailable = available))
    }

    suspend fun addMenuItem(
        name: String,
        category: String,
        priceInInr: String,
        photoUri: Uri? = null,
    ) {
        val cents = parseMoneyToPaise(priceInInr) ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    val id = remote.createMenuItem(trimmedName, category, cents)
                    if (photoUri != null) {
                        persistMenuPhoto(photoUri, id)?.let { path ->
                            remote.patchMenuItemCustomPhoto(id, path)
                        }
                    } else {
                        remote.syncPull()
                    }
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        val id =
            menu.insert(
                MenuItemEntity(
                    name = trimmedName,
                    category = category.trim().ifEmpty { "General" },
                    priceCents = cents,
                    isAvailable = true,
                    customPhotoPath = null,
                ),
            )
        if (photoUri != null && id > 0) {
            val path = persistMenuPhoto(photoUri, id)
            if (path != null) {
                val row = menu.getById(id) ?: return
                menu.update(row.copy(customPhotoPath = path))
            }
        }
    }

    suspend fun updateMenuItem(
        item: MenuItemEntity,
        name: String,
        category: String,
        priceInInr: String,
        photoUri: Uri?,
        removeCustomPhoto: Boolean,
    ) {
        val cents = parseMoneyToPaise(priceInInr) ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val newCustomPath: String? =
            when {
                photoUri != null -> persistMenuPhoto(photoUri, item.id) ?: item.customPhotoPath
                removeCustomPhoto -> {
                    item.customPhotoPath?.let { path ->
                        withContext(Dispatchers.IO) {
                            runCatching { File(path).delete() }
                        }
                    }
                    null
                }
                else -> item.customPhotoPath
            }

        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.updateMenuItem(
                        item,
                        trimmedName,
                        category,
                        cents,
                        customPhotoUrl = newCustomPath,
                        clearCustomPhoto = removeCustomPhoto && photoUri == null,
                    )
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }

        menu.update(
            item.copy(
                name = trimmedName,
                category = category.trim().ifEmpty { "General" },
                priceCents = cents,
                customPhotoPath = newCustomPath,
            ),
        )
    }

    suspend fun deleteMenuItem(item: MenuItemEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    item.customPhotoPath?.let { path ->
                        withContext(Dispatchers.IO) {
                            runCatching { File(path).delete() }
                        }
                    }
                    remote.deleteMenuItem(item)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        item.customPhotoPath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching { File(path).delete() }
            }
        }
        menu.delete(item)
    }

    private suspend fun persistMenuPhoto(uri: Uri, menuItemId: Long): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(appContext.filesDir, "menu_photos").apply { mkdirs() }
                val outFile = File(dir, "$menuItemId.jpg")
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                } ?: return@withContext null
                outFile.absolutePath
            }.getOrNull()
        }

    suspend fun updateInventory(item: InventoryEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.updateInventory(item)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        inventory.update(item)
    }

    suspend fun deleteInventory(item: InventoryEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.deleteInventory(item)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        inventory.delete(item)
    }

    suspend fun addInventoryItem(
        name: String,
        quantity: String,
        unit: String,
        lowStockThreshold: String,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val q = quantity.toDoubleOrNull()?.coerceAtLeast(0.0) ?: return
        val threshold = lowStockThreshold.toDoubleOrNull()?.coerceAtLeast(0.0) ?: return
        val u = unit.trim().ifEmpty { "units" }
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.addInventoryItem(trimmedName, q, u, threshold)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        inventory.insert(
            InventoryEntity(
                name = trimmedName,
                quantity = q,
                unit = u,
                lowStockThreshold = threshold,
            ),
        )
    }

    suspend fun addExpense(
        expenseCategory: String,
        label: String,
        amountInInr: String,
        note: String?,
    ) {
        val cents = parseMoneyToPaise(amountInInr) ?: return
        if (cents <= 0) return
        val cat = expenseCategory.trim().ifEmpty { "General" }
        val title = label.trim().ifEmpty { cat }
        val createdAt = System.currentTimeMillis()
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.addExpense(cat, title, cents, note?.trim()?.takeIf { it.isNotEmpty() }, createdAt)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        expenses.insert(
            ExpenseEntity(
                expenseCategory = cat,
                label = title,
                amountCents = cents,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMillis = createdAt,
            ),
        )
    }

    suspend fun deleteExpense(e: ExpenseEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.deleteExpense(e.id)
                    true
                }.getOrElse { ex ->
                    if (isNetworkFailure(ex)) false else throw ex
                }
            if (remoteOk) return
        }
        expenses.delete(e)
    }

    suspend fun updateExpense(
        existing: ExpenseEntity,
        expenseCategory: String,
        label: String,
        amountInInr: String,
        note: String?,
    ) {
        val cents = parseMoneyToPaise(amountInInr) ?: return
        if (cents <= 0) return
        val cat = expenseCategory.trim().ifEmpty { "General" }
        val title = label.trim().ifEmpty { cat }
        val trimmedNote = note?.trim()?.takeIf { it.isNotEmpty() }
        val updated =
            existing.copy(
                expenseCategory = cat,
                label = title,
                amountCents = cents,
                note = trimmedNote,
            )
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.updateExpense(updated)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        expenses.update(updated)
    }

    suspend fun setStaffShift(
        member: StaffEntity,
        onShift: Boolean,
    ) {
        if (member.onShift == onShift) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.setStaffShift(member, onShift)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        staff.update(member.copy(onShift = onShift))
    }

    suspend fun saveStaffSalary(
        member: StaffEntity,
        salaryInInr: String,
    ) {
        val normalized = salaryInInr.trim().replace(",", "")
        val cents =
            if (normalized.isEmpty()) {
                0
            } else {
                parseMoneyToPaise(salaryInInr) ?: return
            }
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.saveStaffSalary(member.copy(salaryCents = cents))
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        staff.update(member.copy(salaryCents = cents))
    }

    suspend fun addStaff(
        name: String,
        role: String,
    ) {
        val n = name.trim()
        if (n.isEmpty()) return
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.addStaff(n, role)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        staff.insert(
            StaffEntity(
                name = n,
                role = role.trim().ifEmpty { "Staff" },
                onShift = false,
                salaryCents = 0,
            ),
        )
    }

    suspend fun deleteStaff(member: StaffEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.deleteStaff(member.id)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        db.withTransaction {
            staffAbsences.deleteAllForStaff(member.id)
            staff.delete(member)
        }
    }

    suspend fun addStaffAbsence(
        staffId: Long,
        daysAgoFromToday: Int,
        note: String?,
    ) {
        val zone = ZoneId.systemDefault()
        val day =
            LocalDate.now(zone).minusDays(daysAgoFromToday.coerceAtLeast(0).toLong())
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.addStaffAbsence(staffId, start, note?.trim()?.takeIf { it.isNotEmpty() })
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        if (staffAbsences.countForDay(staffId, start) > 0) return
        staffAbsences.insert(
            StaffAbsenceEntity(
                staffId = staffId,
                dayStartEpochMillis = start,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    suspend fun deleteStaffAbsence(row: StaffAbsenceEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.deleteStaffAbsence(row.id)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        staffAbsences.delete(row)
    }

    suspend fun updateSettings(entity: AppSettingsEntity) {
        val remote = remoteGateway()
        if (remote != null) {
            val remoteOk =
                runCatching {
                    remote.updateSettings(entity)
                    true
                }.getOrElse { e ->
                    if (isNetworkFailure(e)) false else throw e
                }
            if (remoteOk) return
        }
        settings.upsert(entity)
    }

    suspend fun recentReports(limit: Int = 50): List<ReportRow> {
        val rows = orders.recentOrders(limit)
        return buildList(rows.size) {
            for (ow in rows) add(reportRowFromOrderWithLines(ow))
        }
    }

    private suspend fun reportRowFromOrderWithLines(ow: OrderWithLines): ReportRow {
        val previews = linePreviewsForOrder(ow)
        return ReportRow(
            orderId = ow.order.id,
            createdAt = ow.order.createdAtEpochMillis,
            status = ow.order.status,
            totalCents = ow.order.totalCents,
            tableId = ow.order.tableId,
            lineCount = ow.lines.size,
            linePreviews = previews,
        )
    }

    private suspend fun linePreviewsForOrder(ow: OrderWithLines): List<ReportLinePreview> =
        ow.lines.map { line ->
            val item = menu.getById(line.menuItemId)
            ReportLinePreview(
                menuItemId = line.menuItemId,
                itemName = item?.name ?: "Menu item #${line.menuItemId}",
                category = item?.category ?: "",
                customPhotoPath = item?.customPhotoPath,
                quantity = line.quantity,
            )
        }

    /** Half-open range: [fromMillis, toMillisExclusive). */
    suspend fun reportsForDateRange(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): ReportBundle {
        val list = orders.ordersBetween(fromMillis, toMillisExclusive)
        val rows = buildList(list.size) {
            for (ow in list) add(reportRowFromOrderWithLines(ow))
        }
        val paidRev =
            orders.sumPaidRevenueBetween(fromMillis, toMillisExclusive)
        val paidCnt =
            orders.countPaidOrdersBetween(fromMillis, toMillisExclusive)
        val totalCnt =
            orders.countOrdersBetween(fromMillis, toMillisExclusive)
        val expenseTotal =
            expenses.sumBetween(fromMillis, toMillisExclusive)
        val monthlyStaffSalaries = staff.sumMonthlySalaries()
        val zone = ZoneId.systemDefault()
        val periodDays = inclusionDayCount(fromMillis, toMillisExclusive, zone)
        val allocatedSalaryCost =
            if (monthlyStaffSalaries <= 0 || periodDays <= 0) {
                0
            } else {
                (monthlyStaffSalaries * periodDays / 30.0).roundToInt()
            }
        val netProfit = paidRev - expenseTotal - allocatedSalaryCost
        return ReportBundle(
            rows = rows,
            paidRevenueCents = paidRev,
            paidOrderCount = paidCnt,
            totalOrderCount = totalCnt,
            expenseTotalCents = expenseTotal,
            monthlyStaffSalariesCents = monthlyStaffSalaries,
            periodDays = periodDays,
            allocatedSalaryCostCents = allocatedSalaryCost,
            netProfitCents = netProfit,
        )
    }

    data class ReportBundle(
        val rows: List<ReportRow>,
        val paidRevenueCents: Int,
        val paidOrderCount: Int,
        val totalOrderCount: Int,
        val expenseTotalCents: Int,
        val monthlyStaffSalariesCents: Int,
        val periodDays: Long,
        val allocatedSalaryCostCents: Int,
        val netProfitCents: Int,
    )

    private fun inclusionDayCount(
        fromMillis: Long,
        toMillisExclusive: Long,
        zone: ZoneId,
    ): Long {
        if (toMillisExclusive <= fromMillis) return 1L
        val start = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
        val endInclusive =
            Instant.ofEpochMilli(toMillisExclusive - 1L).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(start, endInclusive) + 1L
    }

    fun taxMultiplierFlow(): Flow<Double> =
        settings.observeSettings().map { s ->
            val tax = s?.taxPercent ?: 5.0
            1.0 + tax / 100.0
        }

    /** Parses typed INR amount (e.g. 299 or 299.50) into paise. */
    private fun parseMoneyToPaise(input: String): Int? {
        val normalized = input.trim().replace(",", "")
        val value = normalized.toDoubleOrNull() ?: return null
        return (value * 100.0).toInt().coerceAtLeast(0)
    }

    data class DashboardStats(
        /** Paid sales today minus today's logged expenses and a 1/30 share of monthly salaries (paise). */
        val todayNetProfitCents: Int,
        val activeOrders: Int,
    )

    data class ReportLinePreview(
        val menuItemId: Long,
        val itemName: String,
        val category: String,
        val customPhotoPath: String?,
        val quantity: Int,
    )

    data class ReportRow(
        val orderId: Long,
        val createdAt: Long,
        val status: String,
        val totalCents: Int,
        val tableId: Long?,
        val lineCount: Int,
        val linePreviews: List<ReportLinePreview> = emptyList(),
    )

    data class ReportLineDetail(
        val lineId: Long,
        val menuItemId: Long,
        val itemName: String,
        val category: String,
        val customPhotoPath: String?,
        val quantity: Int,
        val unitPriceCents: Int,
        val lineTotalCents: Int,
    ) {
        fun toLinePreview(): ReportLinePreview =
            ReportLinePreview(
                menuItemId = menuItemId,
                itemName = itemName,
                category = category,
                customPhotoPath = customPhotoPath,
                quantity = quantity,
            )
    }

    data class ReportOrderDetail(
        val orderId: Long,
        val createdAt: Long,
        val status: String,
        val totalCents: Int,
        val tableId: Long?,
        val lines: List<ReportLineDetail>,
    )

    suspend fun getReportOrderDetail(orderId: Long): ReportOrderDetail? {
        val ow = orders.getOrderWithLines(orderId) ?: return null
        val lines =
            ow.lines.map { line ->
                val item = menu.getById(line.menuItemId)
                val name = item?.name ?: "Menu item #${line.menuItemId}"
                val lineTotal = line.quantity * line.unitPriceCents
                ReportLineDetail(
                    lineId = line.id,
                    menuItemId = line.menuItemId,
                    itemName = name,
                    category = item?.category ?: "",
                    customPhotoPath = item?.customPhotoPath,
                    quantity = line.quantity,
                    unitPriceCents = line.unitPriceCents,
                    lineTotalCents = lineTotal,
                )
            }
        return ReportOrderDetail(
            orderId = ow.order.id,
            createdAt = ow.order.createdAtEpochMillis,
            status = ow.order.status,
            totalCents = ow.order.totalCents,
            tableId = ow.order.tableId,
            lines = lines,
        )
    }
}
