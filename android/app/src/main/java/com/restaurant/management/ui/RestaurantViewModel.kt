package com.restaurant.management.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.restaurant.management.data.RestaurantRepository
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.ReservationEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.data.local.entity.TableEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch
import com.restaurant.management.ui.util.ReportDateUtils

class RestaurantViewModel(
    private val repo: RestaurantRepository,
) : ViewModel() {
    val dashboard =
        repo.observeDashboardToday().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            RestaurantRepository.DashboardStats(todayNetProfitCents = 0, activeOrders = 0),
        )

    val tables =
        repo.observeTables().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val menu =
        repo.observeMenu().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val openOrders =
        repo.observeOpenOrders().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val reservations =
        repo.observeReservations().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val inventory =
        repo.observeInventory().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val expenses =
        repo.observeExpenses().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val staff =
        repo.observeStaff().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val settings =
        repo.observeSettings().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null,
        )

    private val _cart = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val cart: StateFlow<Map<Long, Int>> = _cart.asStateFlow()

    /** Cart for QR / guest ordering (separate from POS cart). */
    private val _guestCart = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val guestCart: StateFlow<Map<Long, Int>> = _guestCart.asStateFlow()

    private val _selectedTableId = MutableStateFlow<Long?>(null)
    val selectedTableId: StateFlow<Long?> = _selectedTableId.asStateFlow()

    fun setSelectedTable(id: Long?) {
        _selectedTableId.value = id
    }

    fun addToCart(
        menuItemId: Long,
        delta: Int,
    ) {
        val current = _cart.value.toMutableMap()
        val q = (current[menuItemId] ?: 0) + delta
        if (q <= 0) {
            current.remove(menuItemId)
        } else {
            current[menuItemId] = q
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun addToGuestCart(
        menuItemId: Long,
        delta: Int,
    ) {
        val current = _guestCart.value.toMutableMap()
        val q = (current[menuItemId] ?: 0) + delta
        if (q <= 0) {
            current.remove(menuItemId)
        } else {
            current[menuItemId] = q
        }
        _guestCart.value = current
    }

    fun clearGuestCart() {
        _guestCart.value = emptyMap()
    }

    suspend fun validateQrMenuToken(token: String): Boolean = repo.validateQrMenuToken(token)

    fun placeGuestMenuOrder(onFinished: (() -> Unit)? = null) =
        viewModelScope.launch {
            repo.placeCustomerMenuOrder(_guestCart.value)
            clearGuestCart()
            onFinished?.invoke()
        }

    fun placeOrder() =
        viewModelScope.launch {
            repo.placeOrder(_selectedTableId.value, _cart.value)
            clearCart()
        }

    fun sendToKitchen(orderId: Long) =
        viewModelScope.launch {
            repo.sendOrderToKitchen(orderId)
        }

    fun advanceLine(lineId: Long) =
        viewModelScope.launch {
            repo.advanceKitchenLine(lineId)
        }

    fun markServed(orderId: Long) =
        viewModelScope.launch {
            repo.markOrderServed(orderId)
        }

    fun pay(orderId: Long) =
        viewModelScope.launch {
            repo.payOrder(orderId)
        }

    fun cancel(orderId: Long) =
        viewModelScope.launch {
            repo.cancelOrder(orderId)
        }

    fun setTableStatus(
        tableId: Long,
        status: String,
    ) = viewModelScope.launch { repo.setTableStatus(tableId, status) }

    fun setMenuAvailability(
        item: MenuItemEntity,
        available: Boolean,
    ) = viewModelScope.launch {
        repo.setMenuItemAvailability(item, available)
    }

    fun addMenuItem(
        name: String,
        category: String,
        priceInInr: String,
        photoUri: Uri? = null,
    ) = viewModelScope.launch { repo.addMenuItem(name, category, priceInInr, photoUri) }

    fun saveInventory(item: InventoryEntity) =
        viewModelScope.launch {
            repo.updateInventory(item)
        }

    fun deleteInventory(item: InventoryEntity) =
        viewModelScope.launch {
            repo.deleteInventory(item)
        }

    fun addExpense(
        expenseCategory: String,
        label: String,
        amountInInr: String,
        note: String?,
    ) = viewModelScope.launch {
        repo.addExpense(expenseCategory, label, amountInInr, note)
    }

    fun deleteExpense(e: ExpenseEntity) =
        viewModelScope.launch {
            repo.deleteExpense(e)
        }

    fun addReservation(
        name: String,
        phone: String,
        party: Int,
        atMillis: Long,
        notes: String?,
    ) = viewModelScope.launch {
        repo.addReservation(name, phone, party, atMillis, notes)
    }

    fun deleteReservation(r: ReservationEntity) =
        viewModelScope.launch {
            repo.deleteReservation(r)
        }

    fun setStaffShift(
        member: StaffEntity,
        onShift: Boolean,
    ) = viewModelScope.launch {
        repo.setStaffShift(member, onShift)
    }

    fun saveStaffSalary(
        member: StaffEntity,
        salaryInInr: String,
    ) = viewModelScope.launch {
        repo.saveStaffSalary(member, salaryInInr)
    }

    fun saveSettings(s: AppSettingsEntity) =
        viewModelScope.launch {
            repo.updateSettings(s)
        }

    private val _reportRows =
        MutableStateFlow<List<RestaurantRepository.ReportRow>>(emptyList())
    val reportRows = _reportRows.asStateFlow()

    private val _reportSummary =
        MutableStateFlow(ReportSummaryUi(0, 0, 0))
    val reportSummary = _reportSummary.asStateFlow()

    data class ReportSummaryUi(
        val paidRevenueCents: Int,
        val paidOrderCount: Int,
        val totalOrderCount: Int,
    )

    fun loadReportsForRange(
        fromMillis: Long,
        toMillisExclusive: Long,
    ) = viewModelScope.launch {
        val b = repo.reportsForDateRange(fromMillis, toMillisExclusive)
        _reportRows.value = b.rows
        _reportSummary.value =
            ReportSummaryUi(
                paidRevenueCents = b.paidRevenueCents,
                paidOrderCount = b.paidOrderCount,
                totalOrderCount = b.totalOrderCount,
            )
    }

    fun loadThisMonthReports() {
        val zone = ReportDateUtils.systemZone()
        val (from, to) = ReportDateUtils.thisMonthBounds(zone)
        loadReportsForRange(from, to)
    }

    fun loadMonthReports(ym: YearMonth) {
        val zone = ReportDateUtils.systemZone()
        val (from, to) = ReportDateUtils.yearMonthBounds(ym, zone)
        loadReportsForRange(from, to)
    }

    fun loadCustomRangeReports(
        start: LocalDate,
        end: LocalDate,
    ) {
        val zone = ReportDateUtils.systemZone()
        val (from, to) = ReportDateUtils.inclusiveLocalDateBounds(start, end, zone)
        loadReportsForRange(from, to)
    }

    /** Last N orders (no date filter); kept for compatibility if needed. */
    fun refreshReports() =
        viewModelScope.launch {
            _reportRows.value = repo.recentReports(80)
            _reportSummary.value = ReportSummaryUi(0, 0, 0)
        }
}

class RestaurantViewModelFactory(
    private val repository: RestaurantRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RestaurantViewModel::class.java)) {
            return RestaurantViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
