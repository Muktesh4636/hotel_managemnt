package com.restaurant.management.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.restaurant.management.data.local.entity.AppSettingsEntity
import com.restaurant.management.data.local.entity.InventoryEntity
import com.restaurant.management.data.local.entity.ReservationEntity
import com.restaurant.management.data.local.entity.StaffEntity
import com.restaurant.management.R
import com.restaurant.management.ui.Destinations
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import com.restaurant.management.ui.visual.HubModuleBadge
import com.restaurant.management.ui.visual.InventoryItemBadge
import com.restaurant.management.ui.visual.MenuItemImageBadge
import com.restaurant.management.ui.util.formatCents
import com.restaurant.management.ui.util.hubRouteEnabled
import com.restaurant.management.ui.util.modulesToJson
import com.restaurant.management.ui.util.multilineToPipeList
import com.restaurant.management.ui.util.parseModulesJson
import com.restaurant.management.ui.util.pipeListToMultiline
import com.restaurant.management.ui.util.resolvedExpenseCategories
import com.restaurant.management.ui.util.resolvedMenuCategories
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object AdminScreens {
    private val hubEntriesAll =
        listOf(
            Triple("Menu & item availability", "Edit categories, prices, 86 items", Destinations.MENU_ADMIN),
            Triple("Customer QR menu", "QR code — guests order from phone to kitchen", Destinations.QR_MENU),
            Triple("Inventory & stock", "Track ingredients and low-stock alerts", Destinations.INVENTORY),
            Triple("Expenses", "Track operating costs and running total", Destinations.EXPENSES),
            Triple("Reservations", "Guest bookings and party size", Destinations.RESERVATIONS),
            Triple("Staff & shifts", "Salaries, roles, and on-shift toggles", Destinations.STAFF),
            Triple("Reports & orders", "Recent tickets and revenue audit", Destinations.REPORTS),
            Triple("Global settings", "Categories, modules, tax & venue name", Destinations.SETTINGS),
        )

    @Composable
    fun Hub(
        vm: RestaurantViewModel,
        onNavigate: (String) -> Unit,
    ) {
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
                subtitle = "Back-office modules",
                accent = HeaderAccent.Primary,
                decorationResId = R.drawable.decor_chef_hat,
            )
            Column(
                modifier = Modifier.padding(16.dp),
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
    }

    @Composable
    fun MenuAdmin(vm: RestaurantViewModel) {
        val menu by vm.menu.collectAsState()
        val s by vm.settings.collectAsState()
        val menuCats = remember(s?.menuCategories) { resolvedMenuCategories(s?.menuCategories) }
        var name by remember { mutableStateOf("") }
        var category by remember(menuCats) { mutableStateOf(menuCats.firstOrNull() ?: "General") }
        var price by remember { mutableStateOf("") }
        var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
        val pickPhoto =
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                pendingPhotoUri = uri
            }

        LaunchedEffect(menuCats) {
            if (category !in menuCats) {
                category = menuCats.firstOrNull() ?: "General"
            }
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
            )
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
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
                "Set categories under Global settings. Tap a chip to choose.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(menuCats, key = { it }) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) },
                    )
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
                        pickPhoto.launch(
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
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Text("Add menu item")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                "Items — toggle 86 / availability",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(menu, key = { it.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
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
                        Row {
                            Text(if (item.isAvailable) "Available" else "86'd", modifier = Modifier.padding(end = 8.dp))
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

    @Composable
    fun Inventory(vm: RestaurantViewModel) {
        val items by vm.inventory.collectAsState()
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
            )
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { inv ->
                    InventoryRow(inv, vm)
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

    @Composable
    fun Reservations(vm: RestaurantViewModel) {
        val list by vm.reservations.collectAsState()
        var guest by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var party by remember { mutableStateOf("2") }
        var hours by remember { mutableStateOf("2") }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Reservations",
                subtitle = "Guest bookings",
                accent = HeaderAccent.Tertiary,
                decorationResId = R.drawable.decor_dining_table,
            )
            Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = guest,
                onValueChange = { guest = it },
                label = { Text("Guest name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = party,
                onValueChange = { party = it },
                label = { Text("Party size") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it },
                label = { Text("Hours from now") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val h = hours.toDoubleOrNull()
                    if (h != null && guest.isNotBlank()) {
                        val p = party.toIntOrNull() ?: 2
                        val at = System.currentTimeMillis() + (h * 3600_000).toLong()
                        vm.addReservation(guest, phone, p, at, null)
                        guest = ""
                        phone = ""
                    }
                },
                enabled = guest.isNotBlank(),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Text("Book")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn {
                items(list, key = { it.id }) { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(r.guestName, fontWeight = FontWeight.Medium)
                            Text(
                                "${r.partySize} guests · ${fmt(r.atEpochMillis)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(r.phone, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { vm.deleteReservation(r) }) {
                            Text("Remove")
                        }
                    }
                }
            }
            }
        }
    }

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
                    "Configure the list under Global settings.",
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
                                    OutlinedButton(onClick = { vm.deleteExpense(e) }) {
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Staff",
                subtitle = "Salaries & shifts",
                accent = HeaderAccent.Secondary,
                decorationResId = R.drawable.decor_chef_hat,
            )
            LazyColumn(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(staff, key = { it.id }) { s ->
                    StaffRow(s, vm)
                }
            }
        }
    }

    @Composable
    private fun StaffRow(
        member: StaffEntity,
        vm: RestaurantViewModel,
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
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (member.onShift) "On shift" else "Off",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Switch(
                            checked = member.onShift,
                            onCheckedChange = { vm.setStaffShift(member, it) },
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
            }
        }
    }

    @Composable
    fun Reports(vm: RestaurantViewModel) {
        val rows by vm.reportRows.collectAsState()
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
                subtitle = "Monthly & custom date ranges",
                accent = HeaderAccent.Primary,
                decorationResId = R.drawable.ic_fork_knife,
            )
            Column(
                Modifier
                    .padding(16.dp)
                    .weight(1f),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !useCustomRange,
                        onClick = {
                            useCustomRange = false
                            selectedYm = YearMonth.now(zone)
                        },
                        label = { Text("Monthly") },
                    )
                    FilterChip(
                        selected = useCustomRange,
                        onClick = { useCustomRange = true },
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
                                selectedYm = YearMonth.now(zone)
                            },
                            label = { Text("This month") },
                        )
                        OutlinedButton(
                            onClick = { showMonthPicker = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(formatMonthYear(selectedYm))
                        }
                    }
                    if (showMonthPicker) {
                        AlertDialog(
                            onDismissRequest = { showMonthPicker = false },
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
                                                selectedYm = ym
                                                showMonthPicker = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(formatMonthYear(ym))
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showMonthPicker = false }) {
                                    Text("Close")
                                }
                            },
                        )
                    }
                } else {
                    Text(
                        "Custom date range (YYYY-MM-DD)",
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
                                customFrom = it
                                customError = null
                            },
                            label = { Text("From") },
                            isError = customError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = customTo,
                            onValueChange = {
                                customTo = it
                                customError = null
                            },
                            label = { Text("To") },
                            isError = customError != null,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
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
                                    customError = "End date must be on or after start."
                                    return@Button
                                }
                                vm.loadCustomRangeReports(start, end)
                                customError = null
                            } catch (_: Exception) {
                                customError = "Use dates like 2026-05-01"
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Apply range")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Paid revenue (period)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        )
                        Text(
                            formatCents(summary.paidRevenueCents),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            "${summary.paidOrderCount} paid orders · ${summary.totalOrderCount} tickets total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Orders in period",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    OutlinedButton(
                        onClick = {
                            if (useCustomRange) {
                                try {
                                    val start = LocalDate.parse(customFrom.trim())
                                    val end = LocalDate.parse(customTo.trim())
                                    if (!end.isBefore(start)) {
                                        vm.loadCustomRangeReports(start, end)
                                    }
                                } catch (_: Exception) {
                                    /* keep current */
                                }
                            } else {
                                vm.loadMonthReports(selectedYm)
                            }
                        },
                    ) {
                        Text("Reload")
                    }
                }
                Text(
                    "Ticket list matches the selected period above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(rows, key = { it.orderId }) { row ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "#${row.orderId} · ${row.status} · ${formatCents(row.totalCents)}",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    "${fmt(row.createdAt)} · ${row.lineCount} lines · table ${row.tableId ?: "—"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f),
                                )
                            }
                        }
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
        val s by vm.settings.collectAsState()
        var venue by remember(s?.venueName) { mutableStateOf(s?.venueName ?: "") }
        var tax by remember(s?.taxPercent) { mutableStateOf(s?.taxPercent?.toString() ?: "5") }
        var svc by remember(s?.serviceChargePercent) { mutableStateOf(s?.serviceChargePercent?.toString() ?: "0") }
        var menuCatText by remember { mutableStateOf("") }
        var expenseCatText by remember { mutableStateOf("") }
        var modules by remember(s?.modulesJson) { mutableStateOf(parseModulesJson(s?.modulesJson)) }

        LaunchedEffect(s) {
            venue = s?.venueName ?: ""
            tax = s?.taxPercent?.toString() ?: "5"
            svc = s?.serviceChargePercent?.toString() ?: "0"
            menuCatText = pipeListToMultiline(s?.menuCategories)
            expenseCatText = pipeListToMultiline(s?.expenseCategories)
            modules = parseModulesJson(s?.modulesJson)
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Global settings",
                subtitle = "Venue, categories & modules",
                accent = HeaderAccent.Tertiary,
                decorationResId = R.drawable.decor_plate_meal,
            )
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                    "Menu categories (POS / menu admin)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "One category per line — these appear as chips when you add menu items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = menuCatText,
                    onValueChange = { menuCatText = it },
                    label = { Text("Categories") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    minLines = 4,
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    "Expense categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "One per line — only these show when logging expenses (e.g. Food, Water, Tomatoes).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = expenseCatText,
                    onValueChange = { expenseCatText = it },
                    label = { Text("Categories") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    minLines = 4,
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
                    "Turn off what you do not use — hidden from Operations and the Kitchen tab.",
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
                    Text("Reservations", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.reservations,
                        onCheckedChange = { modules = modules.copy(reservations = it) },
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
                    Text("Staff & shifts", modifier = Modifier.weight(1f))
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
                    Text("Reports", modifier = Modifier.weight(1f))
                    Switch(
                        checked = modules.reports,
                        onCheckedChange = { modules = modules.copy(reports = it) },
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
                        val t = tax.toDoubleOrNull() ?: 0.0
                        val sc = svc.toDoubleOrNull() ?: 0.0
                        vm.saveSettings(
                            AppSettingsEntity(
                                venueName = venue.ifBlank { "My Restaurant" },
                                taxPercent = t,
                                serviceChargePercent = sc,
                                qrMenuToken = s?.qrMenuToken ?: "",
                                menuCategories = multilineToPipeList(menuCatText),
                                expenseCategories = multilineToPipeList(expenseCatText),
                                modulesJson = modulesToJson(modules),
                            ),
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text("Save settings")
                }
                Text(
                    "Leave category lists empty to use built-in defaults. Amounts are INR (₹).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}
