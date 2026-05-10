package com.restaurant.management.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.restaurant.management.data.local.dao.ExpenseDao
import com.restaurant.management.data.local.dao.InventoryDao
import com.restaurant.management.data.local.dao.MenuDao
import com.restaurant.management.data.local.dao.OrderDao
import com.restaurant.management.data.local.dao.ReservationDao
import com.restaurant.management.data.local.dao.SettingsDao
import com.restaurant.management.data.local.dao.StaffDao
import com.restaurant.management.data.local.dao.TableDao
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import com.restaurant.management.data.local.entity.ReservationEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity

@Database(
    entities = [
        TableEntity::class,
        MenuItemEntity::class,
        OrderEntity::class,
        OrderLineEntity::class,
        ReservationEntity::class,
        InventoryEntity::class,
        StaffEntity::class,
        AppSettingsEntity::class,
        ExpenseEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tableDao(): TableDao

    abstract fun menuDao(): MenuDao

    abstract fun orderDao(): OrderDao

    abstract fun reservationDao(): ReservationDao

    abstract fun inventoryDao(): InventoryDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun staffDao(): StaffDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE staff ADD COLUMN salaryCents INTEGER NOT NULL DEFAULT 0",
                    )
                }
            }

        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE app_settings ADD COLUMN qrMenuToken TEXT NOT NULL DEFAULT ''",
                    )
                }
            }

        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS expenses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            label TEXT NOT NULL,
                            amountCents INTEGER NOT NULL,
                            note TEXT,
                            createdAtEpochMillis INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }

        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE menu_items ADD COLUMN customPhotoPath TEXT")
                }
            }

        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE app_settings ADD COLUMN menuCategories TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE app_settings ADD COLUMN expenseCategories TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE app_settings ADD COLUMN modulesJson TEXT NOT NULL DEFAULT ''")
                    db.execSQL(
                        "ALTER TABLE expenses ADD COLUMN expenseCategory TEXT NOT NULL DEFAULT ''",
                    )
                }
            }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restaurant.db",
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
