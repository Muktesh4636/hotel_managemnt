package com.restaurant.management.data.remote

import androidx.room.withTransaction
import com.restaurant.management.data.local.AppDatabase
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import com.restaurant.management.data.local.entity.StaffAbsenceEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Replaces local venue tables with the Django `/api/v1/sync/full/` snapshot (server ids preserved).
 */
object VenueFullSync {
    suspend fun applyFromJson(
        db: AppDatabase,
        json: String,
    ) {
        val root = JSONObject(json)
        val tables = root.optJSONArray("tables") ?: JSONArray()
        val menuItems = root.optJSONArray("menu_items") ?: JSONArray()
        val orders = root.optJSONArray("orders") ?: JSONArray()
        val inventory = root.optJSONArray("inventory") ?: JSONArray()
        val staff = root.optJSONArray("staff") ?: JSONArray()
        val absences = root.optJSONArray("staff_absences") ?: JSONArray()
        val expenses = root.optJSONArray("expenses") ?: JSONArray()
        val settings = root.optJSONObject("settings")

        db.withTransaction {
            db.orderDao().deleteAllLines()
            db.orderDao().deleteAllOrders()
            db.staffAbsenceDao().deleteAll()
            db.staffDao().deleteAll()
            db.expenseDao().deleteAll()
            db.inventoryDao().deleteAll()
            db.menuDao().deleteAll()
            db.tableDao().deleteAll()

            val tableRows =
                buildList {
                    for (i in 0 until tables.length()) {
                        val o = tables.getJSONObject(i)
                        add(
                            TableEntity(
                                id = o.getLong("id"),
                                label = o.getString("label"),
                                section = o.optString("section", "Main"),
                                status = o.getString("status"),
                            ),
                        )
                    }
                }
            if (tableRows.isNotEmpty()) {
                db.tableDao().insertAll(tableRows)
            }

            val menuRows =
                buildList {
                    for (i in 0 until menuItems.length()) {
                        val o = menuItems.getJSONObject(i)
                        add(
                            MenuItemEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                category = o.getString("category"),
                                priceCents = o.getInt("price_cents"),
                                isAvailable = o.optBoolean("is_available", true),
                                customPhotoPath = o.optString("custom_photo_url").ifBlank { null },
                            ),
                        )
                    }
                }
            if (menuRows.isNotEmpty()) {
                db.menuDao().insertAll(menuRows)
            }

            val staffRows =
                buildList {
                    for (i in 0 until staff.length()) {
                        val o = staff.getJSONObject(i)
                        add(
                            StaffEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                role = o.optString("role", "Staff"),
                                onShift = o.optBoolean("on_shift", false),
                                salaryCents = o.optInt("salary_cents", 0),
                            ),
                        )
                    }
                }
            if (staffRows.isNotEmpty()) {
                db.staffDao().insertAll(staffRows)
            }

            val invRows =
                buildList {
                    for (i in 0 until inventory.length()) {
                        val o = inventory.getJSONObject(i)
                        add(
                            InventoryEntity(
                                id = o.getLong("id"),
                                name = o.getString("name"),
                                quantity = o.getDouble("quantity"),
                                unit = o.optString("unit", "kg"),
                                lowStockThreshold = o.optDouble("low_stock_threshold", 0.0),
                            ),
                        )
                    }
                }
            if (invRows.isNotEmpty()) {
                db.inventoryDao().insertAll(invRows)
            }

            val expRows =
                buildList {
                    for (i in 0 until expenses.length()) {
                        val o = expenses.getJSONObject(i)
                        add(
                            ExpenseEntity(
                                id = o.getLong("id"),
                                expenseCategory = o.optString("expense_category", ""),
                                label = o.getString("label"),
                                amountCents = o.getInt("amount_cents"),
                                note = o.optString("note").ifBlank { null },
                                createdAtEpochMillis = o.getLong("created_at_epoch_millis"),
                            ),
                        )
                    }
                }
            for (e in expRows) {
                db.expenseDao().insert(e)
            }

            val absRows =
                buildList {
                    for (i in 0 until absences.length()) {
                        val o = absences.getJSONObject(i)
                        add(
                            StaffAbsenceEntity(
                                id = o.getLong("id"),
                                staffId = o.getLong("staff_id"),
                                dayStartEpochMillis = o.getLong("day_start_epoch_millis"),
                                note = o.optString("note").ifBlank { null },
                            ),
                        )
                    }
                }
            for (a in absRows) {
                db.staffAbsenceDao().insert(a)
            }

            for (i in 0 until orders.length()) {
                val o = orders.getJSONObject(i)
                val orderId = o.getLong("id")
                db.orderDao().insertOrder(
                    OrderEntity(
                        id = orderId,
                        tableId = if (o.isNull("table_id")) null else o.getLong("table_id"),
                        status = o.getString("status"),
                        createdAtEpochMillis = o.getLong("created_at_epoch_millis"),
                        totalCents = o.getInt("total_cents"),
                        notes = o.optString("notes").ifBlank { null },
                    ),
                )
                val lines = o.optJSONArray("lines") ?: JSONArray()
                for (j in 0 until lines.length()) {
                    val ln = lines.getJSONObject(j)
                    db.orderDao().insertLine(
                        OrderLineEntity(
                            id = ln.getLong("id"),
                            orderId = orderId,
                            menuItemId = ln.getLong("menu_item_id"),
                            quantity = ln.getInt("quantity"),
                            unitPriceCents = ln.getInt("unit_price_cents"),
                            kitchenStatus = ln.optString("kitchen_status", "QUEUED"),
                        ),
                    )
                }
            }

            if (settings != null) {
                db.settingsDao().upsert(
                    AppSettingsEntity(
                        id = 1,
                        venueName = settings.optString("venue_name", "My Restaurant"),
                        taxPercent = settings.optDouble("tax_percent", 5.0),
                        serviceChargePercent = settings.optDouble("service_charge_percent", 0.0),
                        qrMenuToken = settings.optString("qr_menu_token", ""),
                        menuCategories = settings.optString("menu_categories", ""),
                        expenseCategories = settings.optString("expense_categories", ""),
                        modulesJson = settings.optString("modules_json", ""),
                    ),
                )
            }
        }
    }
}
