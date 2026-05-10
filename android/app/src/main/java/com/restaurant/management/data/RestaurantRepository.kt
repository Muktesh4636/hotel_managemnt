package com.restaurant.management.data

import android.content.Context
import android.net.Uri
import com.restaurant.management.data.local.AppDatabase
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import com.restaurant.management.data.local.entity.ReservationEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity
import com.restaurant.management.model.KitchenLineStatus
import com.restaurant.management.model.OrderStatus
import com.restaurant.management.model.TableStatus
import androidx.room.withTransaction
import java.time.LocalDate
import java.time.ZoneId
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
    private val reservations = db.reservationDao()
    private val inventory = db.inventoryDao()
    private val expenses = db.expenseDao()
    private val staff = db.staffDao()
    private val settings = db.settingsDao()

    fun observeTables(): Flow<List<TableEntity>> = tables.observeTables()

    fun observeMenu(): Flow<List<MenuItemEntity>> = menu.observeMenu()

    fun observeOpenOrders() = orders.observeOpenOrders()

    fun observeReservations(): Flow<List<ReservationEntity>> = reservations.observeReservations()

    fun observeInventory(): Flow<List<InventoryEntity>> = inventory.observeInventory()

    fun observeExpenses(): Flow<List<ExpenseEntity>> = expenses.observeExpenses()

    fun observeStaff(): Flow<List<StaffEntity>> = staff.observeStaff()

    fun observeSettings(): Flow<AppSettingsEntity?> = settings.observeSettings()

    suspend fun ensureQrMenuToken() {
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
            for ((menuItemId, qty) in cart) {
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
            for ((menuItemId, qty) in cart) {
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
        orders.updateOrder(order.copy(status = OrderStatus.IN_KITCHEN))
    }

    suspend fun advanceKitchenLine(lineId: Long) {
        val line = orders.getLine(lineId) ?: return
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
        orders.updateOrder(order.copy(status = OrderStatus.SERVED))
    }

    suspend fun payOrder(orderId: Long) {
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

    suspend fun setTableStatus(
        tableId: Long,
        status: String,
    ) {
        val table = tables.getById(tableId) ?: return
        tables.update(table.copy(status = status))
    }

    suspend fun setMenuItemAvailability(
        item: MenuItemEntity,
        available: Boolean,
    ) {
        if (item.isAvailable != available) {
            menu.update(item.copy(isAvailable = available))
        }
    }

    suspend fun addMenuItem(
        name: String,
        category: String,
        priceInInr: String,
        photoUri: Uri? = null,
    ) {
        val cents = parseMoneyToPaise(priceInInr) ?: return
        val id =
            menu.insert(
                MenuItemEntity(
                    name = name.trim(),
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
        inventory.update(item)
    }

    suspend fun deleteInventory(item: InventoryEntity) {
        inventory.delete(item)
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
        expenses.insert(
            ExpenseEntity(
                expenseCategory = cat,
                label = title,
                amountCents = cents,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteExpense(e: ExpenseEntity) {
        expenses.delete(e)
    }

    suspend fun addReservation(
        guestName: String,
        phone: String,
        partySize: Int,
        atEpochMillis: Long,
        notes: String?,
    ) {
        reservations.insert(
            ReservationEntity(
                guestName = guestName.trim(),
                phone = phone.trim(),
                partySize = partySize.coerceAtLeast(1),
                atEpochMillis = atEpochMillis,
                notes = notes?.trim(),
            ),
        )
    }

    suspend fun deleteReservation(r: ReservationEntity) {
        reservations.delete(r)
    }

    suspend fun setStaffShift(
        member: StaffEntity,
        onShift: Boolean,
    ) {
        if (member.onShift != onShift) {
            staff.update(member.copy(onShift = onShift))
        }
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
        staff.update(member.copy(salaryCents = cents))
    }

    suspend fun updateSettings(entity: AppSettingsEntity) {
        settings.upsert(entity)
    }

    suspend fun recentReports(limit: Int = 50): List<ReportRow> {
        val rows = orders.recentOrders(limit)
        return rows.map { ow ->
            ReportRow(
                orderId = ow.order.id,
                createdAt = ow.order.createdAtEpochMillis,
                status = ow.order.status,
                totalCents = ow.order.totalCents,
                tableId = ow.order.tableId,
                lineCount = ow.lines.size,
            )
        }
    }

    /** Half-open range: [fromMillis, toMillisExclusive). */
    suspend fun reportsForDateRange(
        fromMillis: Long,
        toMillisExclusive: Long,
    ): ReportBundle {
        val list = orders.ordersBetween(fromMillis, toMillisExclusive)
        val rows =
            list.map { ow ->
                ReportRow(
                    orderId = ow.order.id,
                    createdAt = ow.order.createdAtEpochMillis,
                    status = ow.order.status,
                    totalCents = ow.order.totalCents,
                    tableId = ow.order.tableId,
                    lineCount = ow.lines.size,
                )
            }
        val paidRev =
            orders.sumPaidRevenueBetween(fromMillis, toMillisExclusive)
        val paidCnt =
            orders.countPaidOrdersBetween(fromMillis, toMillisExclusive)
        val totalCnt =
            orders.countOrdersBetween(fromMillis, toMillisExclusive)
        return ReportBundle(
            rows = rows,
            paidRevenueCents = paidRev,
            paidOrderCount = paidCnt,
            totalOrderCount = totalCnt,
        )
    }

    data class ReportBundle(
        val rows: List<ReportRow>,
        val paidRevenueCents: Int,
        val paidOrderCount: Int,
        val totalOrderCount: Int,
    )

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

    data class ReportRow(
        val orderId: Long,
        val createdAt: Long,
        val status: String,
        val totalCents: Int,
        val tableId: Long?,
        val lineCount: Int,
    )
}
