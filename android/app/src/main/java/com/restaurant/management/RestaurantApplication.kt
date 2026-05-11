package com.restaurant.management

import android.app.Application
import com.restaurant.management.data.AccountRepository
import com.restaurant.management.data.DatabaseSeeder
import com.restaurant.management.data.NetworkConnectivityMonitor
import com.restaurant.management.data.RestaurantRepository
import com.restaurant.management.data.remote.ApiPrefs
import com.restaurant.management.data.local.AccountsDatabase
import com.restaurant.management.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RestaurantApplication : Application() {
    lateinit var accountsRepo: AccountRepository
        private set

    lateinit var networkMonitor: NetworkConnectivityMonitor
        private set

    private var _restaurantRepository: RestaurantRepository? = null

    private val _sessionUserId = MutableStateFlow<Long?>(null)
    val sessionUserId: StateFlow<Long?> = _sessionUserId.asStateFlow()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        networkMonitor = NetworkConnectivityMonitor(this)
        ApiPrefs(this).ensureDefaultBackendUrlPersisted()
        val accDb = AccountsDatabase.getInstance(this)
        accountsRepo = AccountRepository(accDb.accountDao(), this)
        if (BuildConfig.DEBUG) {
            runBlocking(Dispatchers.IO) {
                accountsRepo.ensureAccountExistsIfAbsent("9182351381", "123456")
            }
        }
        val saved = accountsRepo.getSavedUserId()
        if (saved != null) {
            openRestaurantForUser(saved)
        }
    }

    fun requireRestaurantRepository(): RestaurantRepository =
        checkNotNull(_restaurantRepository) { "Not signed in" }

    fun openRestaurantForUser(userId: Long) {
        val db = AppDatabase.getInstance(this, userId)
        _restaurantRepository = RestaurantRepository(db, this)
        accountsRepo.saveUserId(userId)
        _sessionUserId.value = userId
        appScope.launch {
            DatabaseSeeder.seedIfEmpty(db)
            _restaurantRepository?.ensureQrMenuToken()
        }
    }

    fun logout() {
        _sessionUserId.value = null
        accountsRepo.clearSavedUserId()
        _restaurantRepository = null
        AppDatabase.closeInstance()
    }

    /**
     * Guest QR menu uses the last signed-in staff account's venue database on this device.
     */
    fun ensureRestaurantRepositoryForGuest(): Boolean {
        val id = accountsRepo.getSavedUserId() ?: return false
        openRestaurantForUser(id)
        return true
    }
}
