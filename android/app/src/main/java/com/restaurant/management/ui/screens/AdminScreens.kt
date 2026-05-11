package com.restaurant.management.ui.screens

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.restaurant.management.data.local.entity.AccountEntity
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.remote.ApiPrefs
import com.restaurant.management.data.local.entity.ExpenseEntity
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.StaffAbsenceEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.RestaurantApplication
import com.restaurant.management.model.orderStatusLabel
import com.restaurant.management.data.RestaurantRepository
import com.restaurant.management.R
import com.restaurant.management.ui.Destinations
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import com.restaurant.management.ui.visual.HubModuleBadge
import com.restaurant.management.ui.visual.InventoryItemBadge
import com.restaurant.management.ui.visual.MenuItemImageBadge
import com.restaurant.management.ui.visual.ReportCarouselPhotoTile
import com.restaurant.management.ui.util.centsToInrPlainInput
import com.restaurant.management.ui.util.formatCents
import com.restaurant.management.ui.util.hubRouteEnabled
import com.restaurant.management.ui.util.modulesToJson
import com.restaurant.management.ui.util.parseModulesJson
import com.restaurant.management.ui.util.launchSubscriptionUpiPayment
import com.restaurant.management.ui.util.resolvedExpenseCategories
import com.restaurant.management.ui.util.subscriptionBillingUpiVpa
import com.restaurant.management.ui.util.SubscriptionUpiPackages
import com.restaurant.management.ui.util.resolvedMenuCategories
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AdminScreens {
    private fun paiseToEditableInr(paise: Int): String {
        val rupees = paise / 100.0
        return if (kotlin.math.abs(rupees - rupees.toInt()) < 1e-9) {
            rupees.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", rupees)
        }
    }

    private fun isPhoneLoginId(loginId: String): Boolean =
        loginId.isNotBlank() &&
            loginId.all { it.isDigit() } &&
            loginId.length >= 10

    private fun formatIndiaMobile(loginId: String): String =
        when {
            loginId.length == 10 -> "+91 ${loginId.take(5)} ${loginId.drop(5)}"
            loginId.length > 10 -> "+$loginId"
            else -> loginId
        }

    private fun profileNameLine(loginId: String): String =
        if (isPhoneLoginId(loginId)) {
            "Team member"
        } else {
            loginId.replaceFirstChar { ch ->
                if (ch.isLowerCase()) {
                    ch.titlecase(Locale.getDefault())
                } else {
                    ch.toString()
                }
            }
        }

    private fun profilePhoneLine(loginId: String): String =
        if (isPhoneLoginId(loginId)) {
            formatIndiaMobile(loginId)
        } else {
            "—"
        }

    private fun profilePlanDescription(context: android.content.Context): String {
        val p = ApiPrefs(context)
        return if (p.isConfigured()) {
            val host = p.baseUrl.trimEnd('/')
            val short = if (host.length > 42) host.take(42) + "…" else host
            "Pro — venue syncs with your server\n$short"
        } else {
            "Standard — venue data on this device only (no cloud link)"
        }
    }

    private data class BillingPlanOffer(
        val priceInr: Int,
        val headline: String,
        val summary: String,
        val detailBullets: List<String>,
    )

    /** In-app subscription tiers (pricing guidance). Tap a row for full inclusions. */
    private val billingPlanOffers =
        listOf(
            BillingPlanOffer(
                priceInr = 299,
                headline = "Starter",
                summary = "POS and core back office — no tables module, no extra admins.",
                detailBullets =
                    listOf(
                        "Counter and quick-service workflows: orders, kitchen, menu, inventory, expenses, staff and reports on your venue account.",
                        "Does not include Tables & floor: no table layout, table assignment, or table-wise check flows.",
                        "Does not include Manage admins: one primary operator model — no separate admin logins with their own roles.",
                    ),
            ),
            BillingPlanOffer(
                priceInr = 499,
                headline = "Tables",
                summary = "Tables and floor; guest-facing website is an optional paid add-on.",
                detailBullets =
                    listOf(
                        "Everything in the Starter (₹299) tier.",
                        "Tables & floor: layout, assign orders to tables, and table-linked service as the app supports.",
                        "Suited to cafés and restaurants that run on table numbers and floor sections.",
                        "Website: optional add-on — hosted menu page or own-domain site — ask us for pricing when you subscribe with this plan.",
                    ),
            ),
            BillingPlanOffer(
                priceInr = 799,
                headline = "Manage admins",
                summary = "Multiple admins with per-person permissions; website included free.",
                detailBullets =
                    listOf(
                        "Includes the full Tables plan (₹499): floor and table workflows plus everything in the Starter (₹299) tier.",
                        "Several admin or manager accounts, each with their own login.",
                        "Granular permissions you can mix per person, for example: edit menu and prices, 86 items, inventory and stock, purchase or waste logs, expenses, payroll and staff roster, reports and exports, global settings and modules, QR menu and integrations — grant only what each role needs.",
                        "Restaurant website is included free with this plan — hosted guest-facing page and web presence; own-domain delivery where we activate it — no extra website charge.",
                    ),
            ),
            BillingPlanOffer(
                priceInr = 1599,
                headline = "Multiple restaurants",
                summary = "Several venues under one account; website included free.",
                detailBullets =
                    listOf(
                        "Run multiple restaurant locations from one subscription: coordinated operations and reporting across venues (exact rollout with us).",
                        "Staff and admin patterns aligned with the Manage admins (₹799) tier, scaled for multi-outlet teams.",
                        "Restaurant website is included free for this tier — hosted site and guest-facing web for your brand; own-domain options where we include them — no extra website charge.",
                    ),
            ),
        )

    private fun formatInrPrice(amount: Int): String =
        "₹" + String.format(Locale.US, "%,d", amount)

    @Composable
    private fun ProfileBillingPlansSection() {
        Text(
            "Subscription plans",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        Text(
            "Tap a plan for details and to pay with PhonePe, Google Pay, Paytm, or any UPI app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        var detailPlan by remember { mutableStateOf<BillingPlanOffer?>(null) }
        val profilePlanPayContext = LocalContext.current
        billingPlanOffers.forEachIndexed { index, offer ->
            if (index > 0) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { detailPlan = offer }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    formatInrPrice(offer.priceInr),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        offer.headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        offer.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(
                        "Tap for details & pay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        detailPlan?.let { plan ->
            val upiId = subscriptionBillingUpiVpa(profilePlanPayContext)
            AlertDialog(
                onDismissRequest = { detailPlan = null },
                title = {
                    Text(
                        "${formatInrPrice(plan.priceInr)} — ${plan.headline}",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        plan.detailBullets.forEach { line ->
                            Text(
                                "• $line",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            "Pay ${formatInrPrice(plan.priceInr)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "UPI: $upiId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        Text(
                            "Choose an app",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    launchSubscriptionUpiPayment(
                                        profilePlanPayContext,
                                        upiId,
                                        plan.priceInr,
                                        plan.headline,
                                        SubscriptionUpiPackages.PHONEPE,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("PhonePe")
                            }
                            Button(
                                onClick = {
                                    launchSubscriptionUpiPayment(
                                        profilePlanPayContext,
                                        upiId,
                                        plan.priceInr,
                                        plan.headline,
                                        SubscriptionUpiPackages.GOOGLE_PAY,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Google Pay")
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    launchSubscriptionUpiPayment(
                                        profilePlanPayContext,
                                        upiId,
                                        plan.priceInr,
                                        plan.headline,
                                        SubscriptionUpiPackages.PAYTM,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Paytm")
                            }
                            Button(
                                onClick = {
                                    launchSubscriptionUpiPayment(
                                        profilePlanPayContext,
                                        upiId,
                                        plan.priceInr,
                                        plan.headline,
                                        appPackage = null,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("UPI")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { detailPlan = null }) {
                        Text("Close")
                    }
                },
            )
        }
    }

    @Composable
    private fun ProfileInfoRow(
        label: String,
        value: String,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    private val hubEntriesAll =
        listOf(
            Triple("Order history", "Tap ticket for items · long-press for edit/delete", Destinations.ORDERS),
            Triple("Menu & item availability", "Edit categories, prices, 86 items", Destinations.MENU_ADMIN),
            Triple("Customer QR menu", "QR code — guests order from phone to kitchen", Destinations.QR_MENU),
            Triple("Inventory & stock", "Track ingredients and low-stock alerts", Destinations.INVENTORY),
            Triple("Expenses", "Track operating costs and running total", Destinations.EXPENSES),
            Triple("Staff", "Salaries, absent days & roster", Destinations.STAFF),
            Triple("Reports", "Revenue, expenses, salaries & net profit", Destinations.REPORTS),
            Triple("Global settings", "Modules, tax & venue name", Destinations.SETTINGS),
        )

    @Composable
    fun Hub(
        vm: RestaurantViewModel,
        onNavigate: (String) -> Unit,
    ) {
        val app = LocalContext.current.applicationContext as RestaurantApplication
        val sessionId by app.sessionUserId.collectAsState()
        var account by remember { mutableStateOf<AccountEntity?>(null) }
        var showProfile by remember { mutableStateOf(false) }
        LaunchedEffect(sessionId) {
            account =
                sessionId?.let { id ->
                    withContext(Dispatchers.IO) {
                        app.accountsRepo.getAccountById(id)
                    }
                }
        }
        val s by vm.settings.collectAsState()
        val flags = remember(s?.modulesJson) { parseModulesJson(s?.modulesJson) }
        val entries =
            remember(flags) {
                hubEntriesAll.filter { hubRouteEnabled(it.third, flags) }
            }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Operations",
                subtitle = "Back-office modules — scroll for more",
                accent = HeaderAccent.Primary,
                decorationResId = null,
                actions = {
                    IconButton(
                        onClick = { showProfile = true },
                        modifier = Modifier.size(56.dp),
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Choose a module. Use the bottom bar to return to service screens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entries.forEachIndexed { index, (title, subtitle, route) ->
                    val container =
                        when (index % 3) {
                            0 -> MaterialTheme.colorScheme.primaryContainer
                            1 -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        }
                    val onContainer =
                        when (index % 3) {
                            0 -> MaterialTheme.colorScheme.onPrimaryContainer
                            1 -> MaterialTheme.colorScheme.onSecondaryContainer
                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                        }
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigate(route) },
                        colors = CardDefaults.cardColors(containerColor = container),
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HubModuleBadge(index = index)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.SemiBold, color = onContainer)
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.88f))
                            }
                        }
                    }
                }
            }
        }

        if (showProfile) {
            val loginId = account?.loginId.orEmpty()
            val venue = s?.venueName?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
            AlertDialog(
                onDismissRequest = { showProfile = false },
                title = {
                    Text(
                        "Your profile",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (account == null) {
                            Text(
                                "Could not load account details.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            ProfileInfoRow("Name", profileNameLine(loginId))
                            ProfileInfoRow("Phone", profilePhoneLine(loginId))
                            ProfileInfoRow(
                                "Restaurant",
                                venue,
                            )
                            ProfileInfoRow("Sync", profilePlanDescription(app))
                            HorizontalDivider(
                                Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                            ProfileBillingPlansSection()
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfile = false }) {
                        Text("Close")
                    }
                },
            )
        }
    }

    @Composable
    fun MenuAdmin(vm: RestaurantViewModel) {
        val menuCtx = LocalContext.current.applicationContext
        LaunchedEffect(Unit) {
            vm.syncPullIfConnected(menuCtx)
        }
        val menu by vm.menu.collectAsState()
        val s by vm.settings.collectAsState()
        val menuCats = remember(s?.menuCategories) { resolvedMenuCategories(s?.menuCategories) }
        var name by remember { mutableStateOf("") }
        var category by remember(s?.menuCategories) {
            mutableStateOf(resolvedMenuCategories(s?.menuCategories).firstOrNull() ?: "General")
        }
        var price by remember { mutableStateOf("") }
        var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
        var showAddForm by remember { mutableStateOf(false) }
        var addCategoryExpanded by remember { mutableStateOf(false) }
        var actionMenuItem by remember { mutableStateOf<MenuItemEntity?>(null) }
        var deleteConfirmItem by remember { mutableStateOf<MenuItemEntity?>(null) }
        var editingItem by remember { mutableStateOf<MenuItemEntity?>(null) }
        var editName by remember { mutableStateOf("") }
        var editCategory by remember { mutableStateOf("") }
        var editPrice by remember { mutableStateOf("") }
        var editPhotoUri by remember { mutableStateOf<Uri?>(null) }
        var editRemovePhoto by remember { mutableStateOf(false) }
        var editCategoryExpanded by remember { mutableStateOf(false) }
        val editCategoryMenuScroll = rememberScrollState()
        val addCategoryMenuScroll = rememberScrollState()
        val pickAddPhoto =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                pendingPhotoUri = uri
            }
        val pickEditPhoto =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) {
                    editPhotoUri = uri
                    editRemovePhoto = false
                }
            }

        val navCategories =
            remember(menuCats) {
                buildList<String?> {
                    add(null)
                    addAll(menuCats)
                }
            }
        val menuListPagerState =
            rememberPagerState(
                pageCount = { navCategories.size.coerceAtLeast(1) },
            )
        val menuListScope = rememberCoroutineScope()

        LaunchedEffect(editingItem) {
            val it = editingItem
            if (it != null) {
                editName = it.name
                editCategory = it.category
                editPrice = paiseToEditableInr(it.priceCents)
                editPhotoUri = null
                editRemovePhoto = false
                editCategoryExpanded = false
            }
        }

        LaunchedEffect(menuCats) {
            if (category !in menuCats) {
                category = menuCats.firstOrNull() ?: "General"
            }
            addCategoryExpanded = false
            if (menuListPagerState.currentPage > menuCats.size) {
                menuListPagerState.scrollToPage(0)
            }
        }

        val editCategoryOptions =
            remember(menuCats, editingItem) {
                val list = menuCats.toMutableList()
                editingItem?.category?.let { c ->
                    if (c !in list) list.add(c)
                }
                if (list.isEmpty()) list.add("General")
                list
            }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Menu",
                subtitle = "Items & availability",
                accent = HeaderAccent.Secondary,
                decorationResId = R.drawable.decor_plate_meal,
                actions = {
                    IconButton(
                        onClick = { showAddForm = !showAddForm },
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription =
                                if (showAddForm) {
                                    "Close add item form"
                                } else {
                                    "Add new menu item"
                                },
                            tint = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                },
            )
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
            ) {
            actionMenuItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { actionMenuItem = null },
                    title = { Text(item.name) },
                    text = {
                        Text(
                            "Long-press actions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    editingItem = item
                                    actionMenuItem = null
                                },
                            ) {
                                Text("Edit item")
                            }
                            TextButton(
                                onClick = {
                                    deleteConfirmItem = item
                                    actionMenuItem = null
                                },
                            ) {
                                Text("Delete item")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { actionMenuItem = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            deleteConfirmItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { deleteConfirmItem = null },
                    title = { Text("Delete this item?") },
                    text = {
                        Text(
                            "It will be removed from the menu. Past tickets may still show a generic label for this dish.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.deleteMenuItem(item)
                                deleteConfirmItem = null
                                if (editingItem?.id == item.id) editingItem = null
                            },
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmItem = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            editingItem?.let { item ->
                AlertDialog(
                    onDismissRequest = { editingItem = null },
                    title = { Text("Edit menu item") },
                    text = {
                        // Category dropdown must sit outside a max-height scroll column or only the first
                        // menu row stays visible (dialog body clips children).
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Item name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    value = editCategory,
                                    onValueChange = {},
                                    label = { Text("Category") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                        )
                                    },
                                )
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { editCategoryExpanded = true },
                                )
                                DropdownMenu(
                                    expanded = editCategoryExpanded,
                                    onDismissRequest = { editCategoryExpanded = false },
                                    modifier = Modifier.heightIn(max = 320.dp),
                                ) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .heightIn(max = 300.dp)
                                                .verticalScroll(editCategoryMenuScroll),
                                    ) {
                                        editCategoryOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    editCategory = option
                                                    editCategoryExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                Modifier
                                    .verticalScroll(rememberScrollState())
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = editPrice,
                                    onValueChange = { editPrice = it },
                                    label = { Text("Price in ₹ (e.g. 299 or 149.50)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            pickEditPhoto.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                                ),
                                            )
                                        },
                                    ) {
                                        Text(
                                            if (editPhotoUri != null) {
                                                "Change photo"
                                            } else {
                                                "Choose photo (optional)"
                                            },
                                        )
                                    }
                                    val showRemovePhoto =
                                        editPhotoUri != null ||
                                            (item.customPhotoPath != null && !editRemovePhoto)
                                    if (showRemovePhoto) {
                                        TextButton(
                                            onClick = {
                                                val hadPendingNew = editPhotoUri != null
                                                editPhotoUri = null
                                                editRemovePhoto = item.customPhotoPath != null || hadPendingNew
                                            },
                                        ) {
                                            Text("Remove photo")
                                        }
                                    }
                                }
                                Text(
                                    when {
                                        editPhotoUri != null ->
                                            "New photo will be saved when you tap Save."
                                        editRemovePhoto ->
                                            "Photo will be cleared; the default image will be used."
                                        item.customPhotoPath != null ->
                                            "This item has a custom photo."
                                        else ->
                                            "Optional — uses the category default image."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.updateMenuItem(
                                    item,
                                    editName,
                                    editCategory,
                                    editPrice,
                                    editPhotoUri,
                                    editRemovePhoto,
                                )
                                editingItem = null
                            },
                            enabled = editName.isNotBlank(),
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingItem = null }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            if (showAddForm) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .zIndex(4f)
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("New item name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    Text(
                        "Tap the field and pick a category from your menu (same names as the category tabs above).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            value = category,
                            onValueChange = {},
                            label = { Text("Select category") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                )
                            },
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { addCategoryExpanded = true },
                        )
                        DropdownMenu(
                            expanded = addCategoryExpanded,
                            onDismissRequest = { addCategoryExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp),
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .heightIn(max = 300.dp)
                                        .verticalScroll(addCategoryMenuScroll),
                            ) {
                                menuCats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            addCategoryExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price in ₹ (e.g. 299 or 149.50)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickAddPhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            Text(if (pendingPhotoUri != null) "Change photo" else "Add photo (optional)")
                        }
                        if (pendingPhotoUri != null) {
                            TextButton(onClick = { pendingPhotoUri = null }) {
                                Text("Remove photo")
                            }
                        }
                    }
                    if (pendingPhotoUri != null) {
                        Text(
                            "A photo is selected — it will appear on menu lists after you add the item.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Button(
                        onClick = {
                            vm.addMenuItem(name, category, price, pendingPhotoUri)
                            name = ""
                            price = ""
                            pendingPhotoUri = null
                            showAddForm = false
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Text("Add menu item")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            Text(
                "Items — toggle 86 / availability",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "Tap a chip or swipe the list sideways — pages move smoothly like on POS.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
            ) {
                item(key = "_all") {
                    FilterChip(
                        selected = menuListPagerState.currentPage == 0,
                        onClick = {
                            menuListScope.launch {
                                menuListPagerState.animateScrollToPage(0)
                            }
                        },
                        label = { Text("All") },
                    )
                }
                items(menuCats, key = { it }) { cat ->
                    val pageIndex = 1 + menuCats.indexOf(cat)
                    FilterChip(
                        selected = menuListPagerState.currentPage == pageIndex,
                        onClick = {
                            menuListScope.launch {
                                if (menuListPagerState.currentPage == pageIndex) {
                                    menuListPagerState.animateScrollToPage(0)
                                } else {
                                    menuListPagerState.animateScrollToPage(pageIndex)
                                }
                            }
                        },
                        label = { Text(cat) },
                    )
                }
            }
            HorizontalPager(
                state = menuListPagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .zIndex(0f),
                beyondViewportPageCount = 1,
            ) { page ->
                val pageCategory = navCategories.getOrNull(page)
                val pageItems =
                    remember(menu, pageCategory, page) {
                        if (pageCategory == null) {
                            menu
                        } else {
                            menu.filter { it.category == pageCategory }
                        }
                    }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (pageItems.isEmpty()) {
                        item(key = "empty_$page") {
                            Text(
                                text =
                                    if (menu.isEmpty()) {
                                        "No menu items yet — tap + to add one."
                                    } else {
                                        "No items in this category."
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                    items(pageItems, key = { "${page}_${it.id}" }) { item ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .pointerInput(item.id) {
                                        detectTapGestures(
                                            onLongPress = { actionMenuItem = item },
                                        )
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MenuItemImageBadge(
                                    itemName = item.name,
                                    category = item.category,
                                    itemId = item.id,
                                    customPhotoPath = item.customPhotoPath,
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${item.category} · ${formatCents(item.priceCents)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (item.isAvailable) "Available" else "86'd",
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Switch(
                                    checked = item.isAvailable,
                                    onCheckedChange = { vm.setMenuAvailability(item, it) },
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    @Composable
    fun Inventory(vm: RestaurantViewModel) {
        val items by vm.inventory.collectAsState()
        var showAddForm by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf("") }
        var newQty by remember { mutableStateOf("") }
        var newUnit by remember { mutableStateOf("kg") }
        var newLow by remember { mutableStateOf("") }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Inventory",
                subtitle = "Stock levels",
                accent = HeaderAccent.Primary,
                decorationResId = R.drawable.decor_kitchen_pot,
                actions = {
                    IconButton(
                        onClick = { showAddForm = !showAddForm },
                    ) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription =
                                if (showAddForm) {
                                    "Close add stock form"
                                } else {
                                    "Add inventory stock"
                                },
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                if (showAddForm) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Item name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newQty,
                        onValueChange = { newQty = it },
                        label = { Text("Quantity on hand") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = newUnit,
                        onValueChange = { newUnit = it },
                        label = { Text("Unit (e.g. kg, L, bottles)") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                    )
                    OutlinedTextField(
                        value = newLow,
                        onValueChange = { newLow = it },
                        label = { Text("Low-stock alert below") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Button(
                        onClick = {
                            vm.addInventoryItem(newName, newQty, newUnit, newLow)
                            newName = ""
                            newQty = ""
                            newUnit = "kg"
                            newLow = ""
                            showAddForm = false
                        },
                        enabled =
                            newName.isNotBlank() &&
                                newQty.toDoubleOrNull() != null &&
                                newLow.toDoubleOrNull() != null,
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        Text("Add stock item")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { inv ->
                        InventoryRow(inv, vm)
                    }
                }
            }
        }
    }

    @Composable
    private fun InventoryRow(
        item: InventoryEntity,
        vm: RestaurantViewModel,
    ) {
        var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete stock item?") },
                text = {
                    Text(
                        "Remove \"${item.name}\" from inventory? This cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            vm.deleteInventory(item)
                        },
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InventoryItemBadge(itemName = item.name, rowId = item.id)
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        if (item.quantity <= item.lowStockThreshold) {
                            Text("LOW", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    "${item.quantity} ${item.unit} (reorder ≤ ${item.lowStockThreshold})",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                )
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Adjust quantity") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val v = qtyText.toDoubleOrNull()
                            if (v != null) {
                                vm.saveInventory(item.copy(quantity = v.coerceAtLeast(0.0)))
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Expenses(vm: RestaurantViewModel) {
        val list by vm.expenses.collectAsState()
        val s by vm.settings.collectAsState()
        val expenseCats =
            remember(s?.expenseCategories) {
                resolvedExpenseCategories(s?.expenseCategories)
            }
        var expenseCategory by remember(expenseCats) {
            mutableStateOf(expenseCats.firstOrNull() ?: "Other")
        }
        var label by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
        val totalCents = list.sumOf { it.amountCents }

        LaunchedEffect(expenseCats) {
            if (expenseCategory !in expenseCats) {
                expenseCategory = expenseCats.firstOrNull() ?: "Other"
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Expenses",
                subtitle = "Operating costs",
                accent = HeaderAccent.Secondary,
            )
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
            ) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Total recorded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        )
                        Text(
                            formatCents(totalCents),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Expense category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "Pick a category chip — defaults are built into the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(expenseCats, key = { it }) { cat ->
                        FilterChip(
                            selected = expenseCategory == cat,
                            onClick = { expenseCategory = cat },
                            label = { Text(cat) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Description (e.g. tomatoes, vendor)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount in ₹") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        vm.addExpense(expenseCategory, label, amount, note)
                        label = ""
                        amount = ""
                        note = ""
                    },
                    enabled =
                        label.isNotBlank() &&
                            amount.trim().isNotEmpty(),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Text("Add expense")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "Recent expenses",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    "Long-press an entry to edit name, category, or amount.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.id }) { e ->
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = { editingExpense = e },
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when {
                                            e.expenseCategory.isNotBlank() &&
                                                e.label.isNotBlank() ->
                                                "${e.expenseCategory} · ${e.label}"

                                            e.expenseCategory.isNotBlank() -> e.expenseCategory
                                            else -> e.label
                                        },
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        fmt(e.createdAtEpochMillis),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    e.note?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatCents(e.amountCents),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    OutlinedButton(
                                        onClick = { vm.deleteExpense(e) },
                                    ) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            editingExpense?.let { exp ->
                key(exp.id) {
                    val editCats =
                        remember(exp.expenseCategory, expenseCats) {
                            val c = exp.expenseCategory.trim()
                            if (c.isNotEmpty() && c !in expenseCats) {
                                expenseCats + c
                            } else {
                                expenseCats
                            }
                        }
                    var editCat by remember(exp.id) {
                        mutableStateOf(
                            exp.expenseCategory.trim().ifEmpty { editCats.firstOrNull() ?: "Other" }.let { ec ->
                                if (ec in editCats) ec else editCats.firstOrNull() ?: "Other"
                            },
                        )
                    }
                    var editLabel by remember(exp.id) { mutableStateOf(exp.label) }
                    var editAmount by remember(exp.id) { mutableStateOf(centsToInrPlainInput(exp.amountCents)) }
                    var editNote by remember(exp.id) { mutableStateOf(exp.note.orEmpty()) }
                    LaunchedEffect(exp.id, editCats) {
                        if (editCat !in editCats) {
                            editCat = editCats.firstOrNull() ?: "Other"
                        }
                    }
                    AlertDialog(
                        onDismissRequest = { editingExpense = null },
                        title = {
                            Text(
                                "Edit expense",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        text = {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    "Category",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                ) {
                                    items(editCats, key = { it }) { cat ->
                                        FilterChip(
                                            selected = editCat == cat,
                                            onClick = { editCat = cat },
                                            label = { Text(cat) },
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = editLabel,
                                    onValueChange = { editLabel = it },
                                    label = { Text("Name / description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = editAmount,
                                    onValueChange = { editAmount = it },
                                    label = { Text("Amount in ₹") },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                )
                                OutlinedTextField(
                                    value = editNote,
                                    onValueChange = { editNote = it },
                                    label = { Text("Note (optional)") },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    vm.updateExpense(
                                        exp,
                                        editCat,
                                        editLabel,
                                        editAmount,
                                        editNote.ifBlank { null },
                                    )
                                    editingExpense = null
                                },
                                enabled =
                                    editLabel.isNotBlank() &&
                                        editAmount.trim().isNotEmpty(),
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { editingExpense = null }) {
                                Text("Cancel")
                            }
                        },
                    )
                }
            }
        }
    }

    private fun fmt(epoch: Long): String {
        val sdf = SimpleDateFormat("EEE MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(epoch))
    }

    @Composable
    fun Staff(vm: RestaurantViewModel) {
        val staff by vm.staff.collectAsState()
        val absencesMap by vm.staffAbsencesByStaffId.collectAsState()
        var showAddForm by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("") }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Staff",
                subtitle = "Salaries & absent days",
                accent = HeaderAccent.Secondary,
                decorationResId = R.drawable.decor_chef_hat,
                actions = {
                    IconButton(onClick = { showAddForm = !showAddForm }) {
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription =
                                if (showAddForm) {
                                    "Close add staff form"
                                } else {
                                    "Add staff member"
                                },
                            tint = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
            ) {
                if (showAddForm) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newRole,
                        onValueChange = { newRole = it },
                        label = { Text("Role (e.g. Server, Chef)") },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            vm.addStaff(newName, newRole)
                            newName = ""
                            newRole = ""
                            showAddForm = false
                        },
                        enabled = newName.isNotBlank(),
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        Text("Add staff member")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(staff, key = { it.id }) { s ->
                        StaffRow(s, vm, absencesMap[s.id].orEmpty())
                    }
                }
            }
        }
    }

    @Composable
    private fun StaffRow(
        member: StaffEntity,
        vm: RestaurantViewModel,
        absences: List<StaffAbsenceEntity>,
    ) {
        var salaryInput by remember(member.id, member.salaryCents) {
            mutableStateOf(
                if (member.salaryCents <= 0) {
                    ""
                } else {
                    (member.salaryCents / 100.0).toString()
                },
            )
        }
        var daysAgoInput by remember(member.id) { mutableStateOf("0") }
        var absenceNote by remember(member.id) { mutableStateOf("") }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Remove staff member?") },
                text = {
                    Text(
                        "Delete \"${member.name}\" and their absent-day history? This cannot be undone.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            vm.deleteStaff(member)
                        },
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            member.name,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            member.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = salaryInput,
                        onValueChange = { salaryInput = it },
                        label = { Text("Monthly salary (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Button(onClick = { vm.saveStaffSalary(member, salaryInput) }) {
                        Text("Save salary")
                    }
                }
                if (member.salaryCents > 0) {
                    Text(
                        "Saved: ${formatCents(member.salaryCents)} / month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                HorizontalDivider(
                    Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f),
                )
                Text(
                    "Absent days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Days ago: 0 = today, 1 = yesterday. Duplicate calendar days are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (absences.isEmpty()) {
                    Text(
                        "No absent days recorded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    ) {
                        absences.forEach { a ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        fmtDay(a.dayStartEpochMillis),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    a.note?.let { n ->
                                        Text(
                                            n,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                                TextButton(onClick = { vm.deleteStaffAbsence(a) }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = daysAgoInput,
                        onValueChange = { daysAgoInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days ago") },
                        modifier = Modifier.weight(0.35f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = absenceNote,
                        onValueChange = { absenceNote = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                Button(
                    onClick = {
                        val d = daysAgoInput.toIntOrNull() ?: 0
                        vm.recordStaffAbsence(member.id, d, absenceNote)
                        absenceNote = ""
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Record absent day")
                }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                ) {
                    Text("Remove worker")
                }
            }
        }
    }

    private fun fmtDay(dayStartEpochMillis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(dayStartEpochMillis))
    }

    private fun millisToLocalDateIso(
        millis: Long,
        zone: ZoneId,
    ): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toString()

    private fun localDateIsoToPickerMillis(
        iso: String,
        zone: ZoneId,
    ): Long =
        runCatching {
            LocalDate.parse(iso.trim()).atStartOfDay(zone).toInstant().toEpochMilli()
        }.getOrElse {
            LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ReportPeriodControls(
        vm: RestaurantViewModel,
        zone: ZoneId,
        selectedYm: YearMonth,
        onSelectedYm: (YearMonth) -> Unit,
        useCustomRange: Boolean,
        onUseCustomRange: (Boolean) -> Unit,
        customFrom: String,
        onCustomFrom: (String) -> Unit,
        customTo: String,
        onCustomTo: (String) -> Unit,
        customError: String?,
        onCustomError: (String?) -> Unit,
        showMonthPicker: Boolean,
        onShowMonthPicker: (Boolean) -> Unit,
        monthChoices: List<YearMonth>,
    ) {
        var datePickerTarget by remember { mutableStateOf<Int?>(null) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !useCustomRange,
                onClick = {
                    datePickerTarget = null
                    onUseCustomRange(false)
                    onSelectedYm(YearMonth.now(zone))
                },
                label = { Text("Monthly") },
            )
            FilterChip(
                selected = useCustomRange,
                onClick = { onUseCustomRange(true) },
                label = { Text("Custom dates") },
            )
        }
        if (!useCustomRange) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedYm == YearMonth.now(zone),
                    onClick = {
                        onSelectedYm(YearMonth.now(zone))
                    },
                    label = { Text("This month") },
                )
                OutlinedButton(
                    onClick = { onShowMonthPicker(true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatMonthYear(selectedYm))
                }
            }
            if (showMonthPicker) {
                AlertDialog(
                    onDismissRequest = { onShowMonthPicker(false) },
                    title = { Text("Choose month") },
                    text = {
                        Column(
                            Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            monthChoices.forEach { ym ->
                                TextButton(
                                    onClick = {
                                        onSelectedYm(ym)
                                        onShowMonthPicker(false)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(formatMonthYear(ym))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { onShowMonthPicker(false) }) {
                            Text("Close")
                        }
                    },
                )
            }
        } else {
            Text(
                "Custom range — tap the calendar on each field or type YYYY-MM-DD.",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = customFrom,
                    onValueChange = {
                        onCustomFrom(it)
                        onCustomError(null)
                    },
                    label = { Text("From") },
                    isError = customError != null,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerTarget = 1 }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick start date",
                            )
                        }
                    },
                )
                OutlinedTextField(
                    value = customTo,
                    onValueChange = {
                        onCustomTo(it)
                        onCustomError(null)
                    },
                    label = { Text("To") },
                    isError = customError != null,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerTarget = 2 }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick end date",
                            )
                        }
                    },
                )
            }
            customError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(
                onClick = {
                    try {
                        val start = LocalDate.parse(customFrom.trim())
                        val end = LocalDate.parse(customTo.trim())
                        if (end.isBefore(start)) {
                            onCustomError("End date must be on or after start.")
                            return@Button
                        }
                        vm.loadCustomRangeReports(start, end)
                        onCustomError(null)
                    } catch (_: Exception) {
                        onCustomError("Use dates like 2026-05-01")
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Apply range")
            }
        }
        if (datePickerTarget != null) {
            val which = datePickerTarget!!
            key(which) {
                val initialMillis =
                    when (which) {
                        1 -> localDateIsoToPickerMillis(customFrom, zone)
                        else -> localDateIsoToPickerMillis(customTo, zone)
                    }
                val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { datePickerTarget = null },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val ms = state.selectedDateMillis
                                if (ms != null) {
                                    val s = millisToLocalDateIso(ms, zone)
                                    when (which) {
                                        1 -> onCustomFrom(s)
                                        2 -> onCustomTo(s)
                                    }
                                    onCustomError(null)
                                }
                                datePickerTarget = null
                            },
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { datePickerTarget = null }) {
                            Text("Cancel")
                        }
                    },
                ) {
                    DatePicker(state = state)
                }
            }
        }
    }

    @Composable
    private fun ReportFinancialBreakdown(summary: RestaurantViewModel.ReportSummaryUi) {
        val onPrimary = MaterialTheme.colorScheme.onPrimaryContainer
        val profitColor =
            when {
                summary.netProfitCents > 0 -> MaterialTheme.colorScheme.primary
                summary.netProfitCents < 0 -> MaterialTheme.colorScheme.error
                else -> onPrimary
            }
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Orders and income (this period)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Orders received",
                            style = MaterialTheme.typography.labelMedium,
                            color = onPrimary.copy(alpha = 0.9f),
                        )
                        Text(
                            "${summary.totalOrderCount}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = onPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "all tickets",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimary.copy(alpha = 0.82f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Paid income",
                            style = MaterialTheme.typography.labelMedium,
                            color = onPrimary.copy(alpha = 0.9f),
                        )
                        Text(
                            formatCents(summary.paidRevenueCents),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = onPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "${summary.paidOrderCount} paid orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimary.copy(alpha = 0.82f),
                        )
                    }
                }
                HorizontalDivider(
                    Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Text(
                    "Financial summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                )
                Text(
                    "Paid sales, logged expenses, and your staff salary share for this period (same logic as the dashboard).",
                    style = MaterialTheme.typography.bodySmall,
                    color = onPrimary.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Total revenue (paid sales)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onPrimary,
                    )
                    Text(
                        formatCents(summary.paidRevenueCents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onPrimary,
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Expenses (logged in period)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = onPrimary,
                    )
                    Text(
                        formatCents(summary.expenseTotalCents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onPrimary,
                    )
                }
                Column(Modifier.padding(top = 10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Staff salaries (period share)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = onPrimary,
                        )
                        Text(
                            formatCents(summary.allocatedSalaryCostCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onPrimary,
                        )
                    }
                    Text(
                        "Monthly salaries total ${formatCents(summary.monthlyStaffSalariesCents)} ÷ 30 × ${summary.periodDays} days in period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                HorizontalDivider(
                    Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Net profit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onPrimary,
                    )
                    Text(
                        formatCents(summary.netProfitCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = profitColor,
                    )
                }
                Text(
                    "Revenue − expenses − staff salary share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onPrimary.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                HorizontalDivider(
                    Modifier.padding(top = 14.dp, bottom = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                )
                Text(
                    "${summary.paidOrderCount} paid orders · ${summary.totalOrderCount} tickets in period",
                    style = MaterialTheme.typography.bodySmall,
                    color = onPrimary.copy(alpha = 0.88f),
                )
            }
        }
    }

    @Composable
    fun Reports(vm: RestaurantViewModel) {
        val summary by vm.reportSummary.collectAsState()
        val zone = remember { java.time.ZoneId.systemDefault() }
        var selectedYm by remember { mutableStateOf(YearMonth.now(zone)) }
        var showMonthPicker by remember { mutableStateOf(false) }
        var useCustomRange by remember { mutableStateOf(false) }
        var customFrom by remember { mutableStateOf("") }
        var customTo by remember { mutableStateOf("") }
        var customError by remember { mutableStateOf<String?>(null) }

        val monthChoices =
            remember(zone) {
                val now = YearMonth.now(zone)
                (0 until 24).map { now.minusMonths(it.toLong()) }
            }

        LaunchedEffect(selectedYm, useCustomRange) {
            if (!useCustomRange) {
                vm.loadMonthReports(selectedYm)
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Reports",
                subtitle = "Revenue, expenses, salaries & net profit",
                accent = HeaderAccent.Primary,
                decorationResId = R.drawable.ic_fork_knife,
            )
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                ReportPeriodControls(
                    vm = vm,
                    zone = zone,
                    selectedYm = selectedYm,
                    onSelectedYm = { selectedYm = it },
                    useCustomRange = useCustomRange,
                    onUseCustomRange = { useCustomRange = it },
                    customFrom = customFrom,
                    onCustomFrom = { customFrom = it },
                    customTo = customTo,
                    onCustomTo = { customTo = it },
                    customError = customError,
                    onCustomError = { customError = it },
                    showMonthPicker = showMonthPicker,
                    onShowMonthPicker = { showMonthPicker = it },
                    monthChoices = monthChoices,
                )

                Text(
                    text =
                        if (useCustomRange) {
                            if (customFrom.isBlank() || customTo.isBlank()) {
                                "Custom range: tap the calendar icons or type dates, then Apply."
                            } else {
                                "Custom range: $customFrom through $customTo"
                            }
                        } else {
                            "Calendar month: ${formatMonthYear(selectedYm)}"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                ReportFinancialBreakdown(summary)
                Text(
                    "Use Order history under Operations for the ticket list and line-item detail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Orders(vm: RestaurantViewModel) {
        val rows by vm.reportRows.collectAsState()
        val orderDetail by vm.reportOrderDetail.collectAsState()
        val orderItemEditor by vm.orderItemEditor.collectAsState()
        val menu by vm.menu.collectAsState()
        val orderHistoryContext = LocalContext.current
        var orderIdActionRow by remember { mutableStateOf<RestaurantRepository.ReportRow?>(null) }
        var orderDeleteConfirmRow by remember { mutableStateOf<RestaurantRepository.ReportRow?>(null) }

        LaunchedEffect(Unit) {
            vm.refreshReports()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            item(key = "order_history_header") {
                ScreenHeader(
                    title = "Order history",
                    subtitle = "Tap a ticket for line items · long-press for actions",
                    accent = HeaderAccent.Primary,
                    decorationResId = R.drawable.ic_fork_knife,
                )
            }
            item(key = "order_history_toolbar") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Orders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        OutlinedButton(onClick = { vm.refreshReports() }) {
                            Text("Refresh")
                        }
                    }
                    Text(
                        "Showing the latest tickets (newest first). Tap a card to see all items on that order. Long-press a card for actions: view line items, add or remove items, or delete the order. " +
                            "Cancelled orders cannot be edited; paid orders can be corrected (totals update).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            items(rows, key = { it.orderId }) { row ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 6.dp)
                            .combinedClickable(
                                onClick = {
                                    orderIdActionRow = null
                                    vm.dismissOrderItemEditor()
                                    vm.openReportOrder(row.orderId)
                                },
                                onLongClickLabel = "Order actions",
                                onLongClick = {
                                    vm.dismissReportOrderDetail()
                                    vm.dismissOrderItemEditor()
                                    orderIdActionRow = row
                                },
                            ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                ) {
                    Row(
                        Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Order ID: ${row.orderId}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                "${orderStatusLabel(row.status)} · ${formatCents(row.totalCents)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                "${fmt(row.createdAt)} · ${row.lineCount} lines · table ${row.tableId ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (row.linePreviews.isNotEmpty()) {
                            ReportsLineImageCarousel(
                                lines = row.linePreviews,
                                modifier = Modifier.width(220.dp),
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
        }

        orderIdActionRow?.let { row ->
            AlertDialog(
                onDismissRequest = { orderIdActionRow = null },
                title = { Text("Order #${row.orderId} — actions") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${orderStatusLabel(row.status)} · ${formatCents(row.totalCents)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                        TextButton(
                            onClick = {
                                vm.openReportOrder(row.orderId)
                                orderIdActionRow = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("View line items")
                        }
                        TextButton(
                            onClick = {
                                vm.openOrderItemEditor(row.orderId) { msg ->
                                    Toast.makeText(orderHistoryContext, msg, Toast.LENGTH_LONG).show()
                                }
                                orderIdActionRow = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Add or remove items")
                        }
                        TextButton(
                            onClick = {
                                orderDeleteConfirmRow = row
                                orderIdActionRow = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Delete entire order")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { orderIdActionRow = null }) {
                        Text("Close")
                    }
                },
            )
        }

        orderDeleteConfirmRow?.let { row ->
            AlertDialog(
                onDismissRequest = { orderDeleteConfirmRow = null },
                title = { Text("Delete entire order?") },
                text = {
                    Text(
                        "Order #${row.orderId} (${orderStatusLabel(row.status)}, ${formatCents(row.totalCents)}) will be removed with all line items. " +
                            "If this table was still occupied for this ticket, it will be marked free. This cannot be undone on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.deleteOrderFromHistory(row.orderId)
                            orderDeleteConfirmRow = null
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { orderDeleteConfirmRow = null }) {
                        Text("Cancel")
                    }
                },
            )
        }

        orderItemEditor?.let { ed ->
            val lineScroll = rememberScrollState()
            val dishes =
                remember(menu) {
                    menu.filter { it.isAvailable }.sortedBy { it.name.lowercase(Locale.getDefault()) }
                }
            AlertDialog(
                onDismissRequest = { vm.dismissOrderItemEditor() },
                title = {
                    Text(
                        "Edit order #${ed.orderId}",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                    ) {
                        Text(
                            "${orderStatusLabel(ed.detail.status)} · total ${formatCents(ed.detail.totalCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            "Current lines — use − / +. At zero the line is removed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Column(
                            modifier =
                                Modifier
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(lineScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (ed.detail.lines.isEmpty()) {
                                Text(
                                    "No lines yet — add dishes below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                ed.detail.lines.forEach { line ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(line.itemName, fontWeight = FontWeight.Medium)
                                            Text(
                                                "${line.quantity} × ${formatCents(line.unitPriceCents)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    vm.orderEditorSetLineQuantity(
                                                        ed.orderId,
                                                        line.lineId,
                                                        line.quantity - 1,
                                                    )
                                                },
                                            ) {
                                                Text("−")
                                            }
                                            Text("${line.quantity}", style = MaterialTheme.typography.titleMedium)
                                            OutlinedButton(
                                                onClick = {
                                                    vm.orderEditorSetLineQuantity(
                                                        ed.orderId,
                                                        line.lineId,
                                                        line.quantity + 1,
                                                    )
                                                },
                                            ) {
                                                Text("+")
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                        Text(
                            "Add menu item",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(dishes, key = { it.id }) { dish ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        dish.name,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            vm.orderEditorAddMenuItem(ed.orderId, dish.id)
                                        },
                                    ) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.dismissOrderItemEditor() }) {
                        Text("Done")
                    }
                },
            )
        }

        orderDetail?.let { d ->
            AlertDialog(
                onDismissRequest = { vm.dismissReportOrderDetail() },
                title = {
                    Text(
                        "Order ID: ${d.orderId}",
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (d.lines.isNotEmpty()) {
                            ReportsLineImageCarousel(
                                lines = d.lines.map { it.toLinePreview() },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            "${fmt(d.createdAt)} · ${orderStatusLabel(d.status)} · table ${d.tableId ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            "Items",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        if (d.lines.isEmpty()) {
                            Text(
                                "No line items stored for this ticket.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        } else {
                            d.lines.forEachIndexed { index, line ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                                Text(
                                    "${line.quantity} × ${line.itemName}",
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "${formatCents(line.unitPriceCents)} each · ${formatCents(line.lineTotalCents)} line total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Ticket total: ${formatCents(d.totalCents)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    vm.printOrderBill(orderHistoryContext, d.orderId)
                                },
                            ) {
                                Text("Print bill")
                            }
                        }
                        Text(
                            "Print opens Android’s print screen — pick your receipt printer if it’s set up under Settings → Connected devices → Printing (or your device’s printing menu).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.dismissReportOrderDetail() }) {
                        Text("Close")
                    }
                },
            )
        }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ReportsLineImageCarousel(
        lines: List<RestaurantRepository.ReportLinePreview>,
        modifier: Modifier = Modifier,
        labelColor: Color,
    ) {
        if (lines.isEmpty()) return
        val pagerState = rememberPagerState(pageCount = { lines.size })
        HorizontalPager(
            state = pagerState,
            modifier = modifier.height(188.dp),
            // Wider inset so neighbours peek in — hints that the row scrolls.
            contentPadding = PaddingValues(horizontal = 56.dp),
            pageSpacing = 10.dp,
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            val line = lines[page]
            val offset =
                (pagerState.currentPage - page).toFloat() + pagerState.currentPageOffsetFraction
            val emphasis = (1f - kotlin.math.abs(offset).coerceIn(0f, 1f))
            val scale = 0.74f + 0.26f * emphasis
            // Side tiles stay fairly clear; only a light blur hints they’re off-centre.
            val alphaV = 0.74f + 0.26f * emphasis
            val showLabel = emphasis >= 0.72f
            val sideAmount = (1f - emphasis).coerceIn(0f, 1f)
            // Light blur only (API 31+) — heavy blur made dishes hard to see.
            val blurRadiusDp =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && sideAmount > 0.12f) {
                    kotlin.math.min(2.2f * sideAmount, 2.8f).dp
                } else {
                    0.dp
                }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = alphaV
                        }
                        .then(
                            if (blurRadiusDp > 0.dp) {
                                Modifier.blur(blurRadiusDp)
                            } else {
                                Modifier
                            },
                        ),
            ) {
                ReportCarouselPhotoTile(
                    menuItemId = line.menuItemId,
                    itemName = line.itemName,
                    category = line.category,
                    customPhotoPath = line.customPhotoPath,
                    quantity = line.quantity,
                    photoSize = 76.dp,
                    highlighted = emphasis >= 0.82f,
                )
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .height(38.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    if (showLabel) {
                        Text(
                            text = line.itemName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = labelColor,
                        )
                    }
                }
            }
        }
    }

    private val monthYearFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    private fun formatMonthYear(ym: YearMonth): String = ym.format(monthYearFormatter)

    @Composable
    fun Settings(vm: RestaurantViewModel) {
        val app = LocalContext.current.applicationContext as RestaurantApplication
        val context = LocalContext.current
        val s by vm.settings.collectAsState()
        var venue by remember(s?.venueName) { mutableStateOf(s?.venueName ?: "") }
        var tax by remember(s?.taxPercent) { mutableStateOf(s?.taxPercent?.toString() ?: "5") }
        var svc by remember(s?.serviceChargePercent) { mutableStateOf(s?.serviceChargePercent?.toString() ?: "0") }
        var modules by remember(s?.modulesJson) { mutableStateOf(parseModulesJson(s?.modulesJson)) }

        LaunchedEffect(s) {
            venue = s?.venueName ?: ""
            tax = s?.taxPercent?.toString() ?: "5"
            svc = s?.serviceChargePercent?.toString() ?: "0"
            modules = parseModulesJson(s?.modulesJson)
        }

        LaunchedEffect(Unit) {
            vm.syncPullIfConnected(app)
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Global settings",
                subtitle = "Venue, tax & modules",
                accent = HeaderAccent.Tertiary,
                decorationResId = R.drawable.decor_plate_meal,
            )
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                fun persistGlobalSettingsForm() {
                    val t = tax.toDoubleOrNull() ?: 0.0
                    val sc = svc.toDoubleOrNull() ?: 0.0
                    vm.saveSettings(
                        AppSettingsEntity(
                            venueName = venue.ifBlank { "My Restaurant" },
                            taxPercent = t,
                            serviceChargePercent = sc,
                            qrMenuToken = s?.qrMenuToken ?: "",
                            menuCategories = s?.menuCategories ?: "",
                            expenseCategories = s?.expenseCategories ?: "",
                            modulesJson = modulesToJson(modules),
                        ),
                    )
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Restaurant name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tax,
                    onValueChange = { tax = it },
                    label = { Text("Tax % (GST / VAT)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = svc,
                    onValueChange = { svc = it },
                    label = { Text("Service charge %") },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    "Restaurant modules",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "Turn off what you do not use — hidden from Operations and the Kitchen tab. " +
                        "Reports, Order history, and Global settings always stay listed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Kitchen (bottom bar)", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.kitchen,
                        onCheckedChange = { modules = modules.copy(kitchen = it) },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Customer QR menu", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.qrMenu,
                        onCheckedChange = { modules = modules.copy(qrMenu = it) },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Inventory", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.inventory,
                        onCheckedChange = { modules = modules.copy(inventory = it) },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Staff", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.staff,
                        onCheckedChange = { modules = modules.copy(staff = it) },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Expenses", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.expenses,
                        onCheckedChange = { modules = modules.copy(expenses = it) },
                    )
                }
                Button(
                    onClick = {
                        persistGlobalSettingsForm()
                        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Save all settings")
                }
                OutlinedButton(
                    onClick = { app.logout() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                ) {
                    Text("Sign out")
                }
            }
        }
    }
}
