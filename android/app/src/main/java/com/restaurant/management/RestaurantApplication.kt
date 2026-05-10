package com.restaurant.management

import android.app.Application
import com.restaurant.management.data.DatabaseSeeder
import com.restaurant.management.data.RestaurantRepository
import com.restaurant.management.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RestaurantApplication : Application() {
    lateinit var repository: RestaurantRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        repository = RestaurantRepository(db, this)
        appScope.launch {
            DatabaseSeeder.seedIfEmpty(db)
            repository.ensureQrMenuToken()
        }
    }
}
