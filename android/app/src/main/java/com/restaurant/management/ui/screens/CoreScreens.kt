package com.restaurant.management.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurant.management.R
import com.restaurant.management.data.local.OrderWithLines
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.data.local.entity.isQrDirectKitchenOrder
import com.restaurant.management.model.OrderStatus
import com.restaurant.management.model.orderStatusLabel
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import com.restaurant.management.ui.visual.KitchenLineBadge
import com.restaurant.management.ui.visual.MenuItemImageBadge
import com.restaurant.management.ui.util.formatCents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CoreScreens {
    @Composable
    fun Dashboard(vm: RestaurantViewModel) {
        val dashCtx = LocalContext.current.applicationContext
        LaunchedEffect(Unit) {
            vm.syncPullIfConnected(dashCtx)
        }
        val stats by vm.dashboard.collectAsState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Dashboard",
                subtitle = "Today's snapshot",
                accent = HeaderAccent.Primary,
                decorationResId = R.drawable.decor_plate_meal,
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FoodHeroBanner()
                StatCard(title = "Today's net profit", value = formatCents(stats.todayNetProfitCents), flavor = 0)
                StatCard(title = "Active orders", value = stats.activeOrders.toString(), flavor = 1)
                Text(
                    "Net profit uses paid sales today, minus expenses logged today (Operations → Expenses), and one day's share of staff salaries (monthly total ÷ 30).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun FoodHeroBanner() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.decor_kitchen_pot),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "From kitchen to table",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Track orders, stock, and service in one warm, food-first workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }

    @Composable
    private fun StatCard(
        title: String,
        value: String,
        flavor: Int = 0,
    ) {
        val container: Color
        val onContainer: Color
        when (flavor % 3) {
            0 -> {
                container = MaterialTheme.colorScheme.primaryContainer
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer
            }
            1 -> {
                container = MaterialTheme.colorScheme.secondaryContainer
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer
            }
            else -> {
                container = MaterialTheme.colorScheme.tertiaryContainer
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = container),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = onContainer.copy(alpha = 0.88f))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainer,
                )
            }
        }
    }

    @Composable
    fun Pos(vm: RestaurantViewModel) {
        val posCtx = LocalContext.current.applicationContext
        LaunchedEffect(Unit) {
            vm.setSelectedTable(null)
            vm.syncPullIfConnected(posCtx)
        }
        val menu by vm.menu.collectAsState()
        val cart by vm.cart.collectAsState()
        val openOrders by vm.openOrders.collectAsState()

        val posMenu =
            remember(menu) {
                menu.filter { it.isAvailable }
            }

        val categoryOrder =
            remember(posMenu) {
                posMenu.map { it.category }.distinct()
            }

        val navCategories =
            remember(categoryOrder) {
                buildList {
                    add(null)
                    addAll(categoryOrder)
                }
            }

        val pagerState =
            rememberPagerState(
                pageCount = { navCategories.size.coerceAtLeast(1) },
            )
        val scope = rememberCoroutineScope()

        LaunchedEffect(navCategories.size) {
            if (navCategories.isNotEmpty() && pagerState.currentPage >= navCategories.size) {
                pagerState.scrollToPage((navCategories.size - 1).coerceAtLeast(0))
            }
        }

        val cartTotal =
            remember(cart, posMenu) {
                val priceById = posMenu.associate { it.id to it.priceCents }
                cart.entries.sumOf { (id, qty) ->
                    (priceById[id] ?: 0) * qty
                }
            }

        val posActiveOrders =
            remember(openOrders) {
                openOrders.filterNot { it.order.isQrDirectKitchenOrder() }
            }

        var showOrderSummary by remember { mutableStateOf(false) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Point of sale",
                subtitle = "Counter & takeaway",
                accent = HeaderAccent.Secondary,
                decorationResId = R.drawable.ic_fork_knife,
            )
            // Upper area: scroll through all menu items
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "Menu",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                Text(
                    "Tap a chip or drag the menu sideways — pages track your finger and ease into place. Vertical scroll still moves the list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            label = { Text("All") },
                        )
                    }
                    lazyRowItems(categoryOrder, key = { it }) { cat ->
                        val pageIndex = navCategories.indexOf(cat)
                        FilterChip(
                            selected = pageIndex >= 0 && pagerState.currentPage == pageIndex,
                            onClick = {
                                scope.launch {
                                    if (pageIndex >= 0) {
                                        if (pagerState.currentPage == pageIndex) {
                                            pagerState.animateScrollToPage(0)
                                        } else {
                                            pagerState.animateScrollToPage(pageIndex)
                                        }
                                    }
                                }
                            },
                            label = { Text(cat) },
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    /** Only lay out the visible category page so vertical fling stays smooth. */
                    beyondViewportPageCount = 0,
                ) { page ->
                    val pageCategory = navCategories.getOrNull(page)
                    val menuSections =
                        remember(posMenu, pageCategory) {
                            val sel = pageCategory
                            if (sel == null) {
                                posMenu
                                    .groupBy { it.category }
                                    .mapNotNull { (cat, dishes) ->
                                        if (dishes.isEmpty()) null else cat to dishes
                                    }
                            } else {
                                val dishes = posMenu.filter { it.category == sel }
                                if (dishes.isEmpty()) emptyList() else listOf(sel to dishes)
                            }
                        }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        menuSections.forEach { (categoryTitle, dishes) ->
                            item(key = "hdr_${page}_$categoryTitle") {
                                Text(
                                    text = categoryTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            lazyItems(dishes, key = { it.id }) { item ->
                                val qty = cart[item.id] ?: 0
                                MenuRow(item, qty) { delta ->
                                    vm.addToCart(item.id, delta)
                                }
                            }
                        }
                    }
                }
            }
            // Counter / table tickets only — QR guest tickets stay on Kitchen (same data, filtered here).
            if (posActiveOrders.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Active orders",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        lazyItems(posActiveOrders, key = { it.order.id }) { ow ->
                            OrderActionsCard(ow.order.id, ow.order.status, ow.order.totalCents, vm)
                        }
                    }
                }
            }
            // Bottom dock: cart + place order
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Cart total: ${formatCents(cartTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { showOrderSummary = true },
                            enabled = cart.values.any { it > 0 },
                        ) {
                            Text("Order summary")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                vm.placeOrder()
                            },
                            enabled = cart.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Place order")
                        }
                        OutlinedButton(onClick = { vm.clearCart() }, modifier = Modifier.weight(1f)) {
                            Text("Clear cart")
                        }
                    }
                }
            }
        }

        if (showOrderSummary) {
            val scroll = rememberScrollState()
            val priceById = remember(posMenu) { posMenu.associate { it.id to it.priceCents } }
            val nameById = remember(posMenu) { posMenu.associate { it.id to it.name } }
            AlertDialog(
                onDismissRequest = { showOrderSummary = false },
                title = {
                    Text(
                        "Order summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroll),
                    ) {
                        cart.entries
                            .filter { it.value > 0 }
                            .sortedBy { nameById[it.key] ?: "" }
                            .forEach { (id, qty) ->
                                val name = nameById[id] ?: "Item #$id"
                                val unit = priceById[id] ?: 0
                                val line = unit * qty
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        "${qty}× $name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        formatCents(line),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text(formatCents(cartTotal), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showOrderSummary = false }) {
                        Text("Close")
                    }
                },
            )
        }
    }

    @Composable
    private fun MenuRow(
        item: MenuItemEntity,
        qty: Int,
        onDelta: (Int) -> Unit,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MenuItemImageBadge(
                itemName = item.name,
                category = item.category,
                itemId = item.id,
                customPhotoPath = item.customPhotoPath,
                lightweightFrame = true,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Medium)
                Text(formatCents(item.priceCents), style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onDelta(-1) },
                    enabled = qty > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium)
                }
                Text("$qty", modifier = Modifier.padding(horizontal = 10.dp))
                OutlinedButton(
                    onClick = { onDelta(1) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    @Composable
    private fun OrderActionsCard(
        orderId: Long,
        status: String,
        totalCents: Int,
        vm: RestaurantViewModel,
    ) {
        val context = LocalContext.current
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(12.dp)) {
                Text("Order ID: $orderId · ${orderStatusLabel(status)} · ${formatCents(totalCents)}")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (status == OrderStatus.OPEN) {
                        Button(onClick = { vm.printOrderBill(context, orderId) }) {
                            Text("Print")
                        }
                    }
                    if (status == OrderStatus.READY) {
                        Button(onClick = { vm.markServed(orderId) }) {
                            Text("Served")
                        }
                    }
                    if (status != OrderStatus.PAID && status != OrderStatus.CANCELLED) {
                        Button(onClick = { vm.pay(orderId) }) {
                            Text("Pay")
                        }
                    }
                    if (status != OrderStatus.PAID) {
                        OutlinedButton(onClick = { vm.cancel(orderId) }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Kitchen(vm: RestaurantViewModel) {
        val orders by vm.openOrders.collectAsState()
        val menu by vm.menu.collectAsState()
        val tables by vm.tables.collectAsState()
        val menuMap = remember(menu) { menu.associateBy { it.id } }
        val kitchenCtx = LocalContext.current.applicationContext
        val toastCtx = LocalContext.current
        var kitchenActionMenu by remember { mutableStateOf<OrderWithLines?>(null) }
        var kitchenCancelConfirmId by remember { mutableStateOf<Long?>(null) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(12_000)
                vm.syncPullIfConnected(kitchenCtx)
            }
        }

        kitchenActionMenu?.let { ow ->
            AlertDialog(
                onDismissRequest = { kitchenActionMenu = null },
                title = { Text("Order #${ow.order.id}") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                vm.openReportOrder(ow.order.id)
                                kitchenActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("View line items")
                        }
                        TextButton(
                            onClick = {
                                vm.openOrderItemEditor(ow.order.id) { msg ->
                                    Toast.makeText(toastCtx, msg, Toast.LENGTH_LONG).show()
                                }
                                kitchenActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit order")
                        }
                        TextButton(
                            onClick = {
                                kitchenCancelConfirmId = ow.order.id
                                kitchenActionMenu = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel order")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { kitchenActionMenu = null }) {
                        Text("Close")
                    }
                },
            )
        }

        kitchenCancelConfirmId?.let { oid ->
            AlertDialog(
                onDismissRequest = { kitchenCancelConfirmId = null },
                title = { Text("Cancel this order?") },
                text = {
                    Text(
                        "Order #$oid will leave the kitchen queue and be marked cancelled.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.cancel(oid)
                            kitchenCancelConfirmId = null
                        },
                    ) {
                        Text("Cancel order")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { kitchenCancelConfirmId = null }) {
                        Text("Back")
                    }
                },
            )
        }

        Box(Modifier.fillMaxSize()) {
            val kitchenQueueOrders =
                remember(orders) {
                    orders.filter { ow ->
                        when (ow.order.status) {
                            OrderStatus.PAID,
                            OrderStatus.CANCELLED,
                            OrderStatus.SERVED,
                            -> false
                            else -> true
                        }
                    }
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
            ) {
                ScreenHeader(
                    title = "Kitchen",
                    subtitle = "Tap order title for summary · long-press for more actions · tap a line to bump · Close order or wait 12h to clear from this list",
                    accent = HeaderAccent.Tertiary,
                    decorationResId = R.drawable.decor_kitchen_pot,
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    lazyItems(
                        kitchenQueueOrders,
                        key = { it.order.id },
                    ) { ow ->
                        val tableLabel =
                            remember(ow.order.tableId, tables) {
                                ow.order.tableId?.let { tid ->
                                    tables.find { it.id == tid }?.label
                                } ?: "—"
                            }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Order ID: ${ow.order.id} · ${orderStatusLabel(ow.order.status)} · Table $tableLabel",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { vm.openReportOrder(ow.order.id) },
                                                onLongClick = { kitchenActionMenu = ow },
                                                onLongClickLabel = "Order actions",
                                            ),
                                )
                                ow.lines.forEach { line ->
                                    val name = menuMap[line.menuItemId]?.name ?: "Item #${line.menuItemId}"
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { vm.advanceLine(line.id) },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            KitchenLineBadge(
                                                itemName = name,
                                                category = menuMap[line.menuItemId]?.category,
                                                menuItemId = line.menuItemId,
                                                customPhotoPath = menuMap[line.menuItemId]?.customPhotoPath,
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "${line.quantity}× $name",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            )
                                        }
                                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                                            Text(
                                                labelKitchen(line.kitchenStatus),
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "Tap a line to advance: Queued → Cooking → Ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick = { vm.markServed(ow.order.id) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Close order")
                                    }
                                    Button(
                                        onClick = { vm.printOrderBill(toastCtx, ow.order.id) },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Print bill")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun labelKitchen(s: String): String =
        when (s) {
            com.restaurant.management.model.KitchenLineStatus.QUEUED -> "Queued"
            com.restaurant.management.model.KitchenLineStatus.COOKING -> "Cooking"
            com.restaurant.management.model.KitchenLineStatus.READY -> "Ready"
            else -> s
        }
}
