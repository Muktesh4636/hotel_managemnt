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
import com.restaurant.management.data.local.dao.StaffAbsenceDao
import com.restaurant.management.data.local.dao.StaffDao
import com.restaurant.management.data.local.dao.TableDao
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.OrderEntity
import com.restaurant.management.data.local.entity.OrderLineEntity
import com.restaurant.management.data.local.entity.ReservationEntity
import com.restaurant.management.data.local.entity.StaffAbsenceEntity
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
        StaffAbsenceEntity::class,
        AppSettingsEntity::class,
        ExpenseEntity::class,
    ],
    version = 8,
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

    abstract fun staffAbsenceDao(): StaffAbsenceDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        @Volatile
        private var instanceUserId: Long = -1L

        fun fileNameForUser(userId: Long): String = "restaurant_user_$userId.db"

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

        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS staff_absences (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            staffId INTEGER NOT NULL,
                            dayStartEpochMillis INTEGER NOT NULL,
                            note TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS index_staff_absences_staffId_dayStartEpochMillis " +
                            "ON staff_absences (staffId, dayStartEpochMillis)",
                    )
                }
            }

        /**
         * Earlier 6→7 used inline UNIQUE(...) only — Room expects a named unique index matching
         * [StaffAbsenceEntity]. Fix broken DBs; if the index already exists (fixed 6→7), no-op.
         */
        private val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val cursor =
                        db.query(
                            "SELECT 1 FROM sqlite_master WHERE type='index' " +
                                "AND name='index_staff_absences_staffId_dayStartEpochMillis'",
                        )
                    val hasRoomIndex =
                        try {
                            cursor.moveToFirst()
                        } finally {
                            cursor.close()
                        }
                    if (hasRoomIndex) return
                    db.execSQL("DROP TABLE IF EXISTS staff_absences")
                    db.execSQL(
                        """
                        CREATE TABLE staff_absences (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            staffId INTEGER NOT NULL,
                            dayStartEpochMillis INTEGER NOT NULL,
                            note TEXT
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX index_staff_absences_staffId_dayStartEpochMillis " +
                            "ON staff_absences (staffId, dayStartEpochMillis)",
                    )
                }
            }

        fun getInstance(
            context: Context,
            userId: Long,
        ): AppDatabase {
            synchronized(this) {
                if (instance != null && instanceUserId != userId) {
                    runCatching { instance?.close() }
                    instance = null
                    instanceUserId = -1L
                }
                if (instance == null) {
                    instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            fileNameForUser(userId),
                        ).addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            MIGRATION_7_8,
                        ).build()
                    instanceUserId = userId
                }
                return instance!!
            }
        }

        fun closeInstance() {
            synchronized(this) {
                runCatching { instance?.close() }
                instance = null
                instanceUserId = -1L
            }
        }
    }
}
