package com.restaurant.management.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.restaurant.management.data.local.dao.AccountDao
import com.restaurant.management.data.local.entity.AccountEntity

@Database(
    entities = [AccountEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AccountsDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var instance: AccountsDatabase? = null

        fun getInstance(context: Context): AccountsDatabase =
            instance ?: synchronized(this) {
                instance ?:
                    Room.databaseBuilder(
                        context.applicationContext,
                        AccountsDatabase::class.java,
                        "restaurant_accounts.db",
                    ).build().also { instance = it }
            }
    }
}
