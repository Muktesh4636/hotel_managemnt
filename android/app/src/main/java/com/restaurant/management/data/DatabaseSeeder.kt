package com.restaurant.management.data

import com.restaurant.management.data.local.AppDatabase
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity
import com.restaurant.management.model.TableStatus
import java.util.UUID

object DatabaseSeeder {
    suspend fun seedIfEmpty(database: AppDatabase) {
        if (database.tableDao().count() > 0) return
        seed(database)
    }

    suspend fun seed(database: AppDatabase) {
        val tables =
            listOf(
                "Main" to listOf("T1", "T2", "T3", "T4", "T5", "T6"),
                "Patio" to listOf("P1", "P2", "P3", "P4"),
                "Bar" to listOf("B1", "B2"),
            ).flatMap { (section, labels) ->
                labels.map { label ->
                    TableEntity(label = label, section = section, status = TableStatus.FREE)
                }
            }
        database.tableDao().insertAll(tables)

        // price field = paise (₹1 = 100 paise); amounts below are sample INR menu prices
        val menu =
            listOf(
                Triple("Starters", "Caesar Salad", 249_00),
                Triple("Starters", "Soup of the Day", 189_00),
                Triple("Starters", "Bruschetta", 229_00),
                Triple("Mains", "Grilled Salmon", 549_00),
                Triple("Mains", "Ribeye Steak", 699_00),
                Triple("Mains", "Pasta Primavera", 389_00),
                Triple("Mains", "Chicken Curry", 349_00),
                Triple("Mains", "Margherita Pizza", 329_00),
                Triple("Desserts", "Cheesecake", 199_00),
                Triple("Desserts", "Brownie Sundae", 229_00),
                Triple("Drinks", "Fresh Lime Soda", 99_00),
                Triple("Drinks", "House Wine", 399_00),
                Triple("Drinks", "Craft Beer", 299_00),
                Triple("Drinks", "Espresso", 149_00),
            ).map { (cat, name, paise) ->
                MenuItemEntity(name = name, category = cat, priceCents = paise, isAvailable = true)
            }
        database.menuDao().insertAll(menu)

        val inventory =
            listOf(
                InventoryEntity(name = "Tomatoes", quantity = 24.0, unit = "kg", lowStockThreshold = 5.0),
                InventoryEntity(name = "Chicken Breast", quantity = 18.0, unit = "kg", lowStockThreshold = 6.0),
                InventoryEntity(name = "Olive Oil", quantity = 12.0, unit = "L", lowStockThreshold = 2.0),
                InventoryEntity(name = "Wine (house)", quantity = 36.0, unit = "bottles", lowStockThreshold = 6.0),
                InventoryEntity(name = "Coffee Beans", quantity = 8.0, unit = "kg", lowStockThreshold = 2.0),
            )
        database.inventoryDao().insertAll(inventory)

        val staff =
            listOf(
                StaffEntity(name = "Alex Rivera", role = "General Manager", onShift = true),
                StaffEntity(name = "Jordan Lee", role = "Head Chef", onShift = true),
                StaffEntity(name = "Sam Patel", role = "Server", onShift = true),
                StaffEntity(name = "Casey Morgan", role = "Host", onShift = false),
                StaffEntity(name = "Riley Chen", role = "Bartender", onShift = true),
            )
        database.staffDao().insertAll(staff)

        database.settingsDao().upsert(
            AppSettingsEntity(
                venueName = "My Restaurant",
                taxPercent = 5.0,
                serviceChargePercent = 0.0,
                qrMenuToken = UUID.randomUUID().toString(),
                menuCategories = "",
                expenseCategories = "",
                modulesJson = "",
            ),
        )
    }
}
