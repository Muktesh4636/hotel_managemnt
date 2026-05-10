package com.restaurant.management.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.model.OrderStatus
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import com.restaurant.management.ui.visual.KitchenLineBadge
import com.restaurant.management.ui.visual.MenuItemImageBadge
import com.restaurant.management.ui.util.formatCents

object CoreScreens {
    @Composable
    fun Dashboard(vm: RestaurantViewModel) {
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
        LaunchedEffect(Unit) {
            vm.setSelectedTable(null)
        }
        val menu by vm.menu.collectAsState()
        val cart by vm.cart.collectAsState()
        val openOrders by vm.openOrders.collectAsState()
        var selectedCategory by remember { mutableStateOf<String?>(null) }

        val categoryOrder =
            remember(menu) {
                menu.map { it.category }.distinct()
            }

        val cartTotal =
            remember(cart, menu) {
                cart.entries.sumOf { (id, qty) ->
                    val price = menu.find { it.id == id }?.priceCents ?: 0
                    price * qty
                }
            }

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
                    "Tap a category to show only those items, or All.",
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
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                        )
                    }
                    lazyRowItems(categoryOrder, key = { it }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory =
                                    if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat) },
                        )
                    }
                }
                val menuSections =
                    remember(menu, selectedCategory) {
                        val sel = selectedCategory
                        if (sel == null) {
                            menu
                                .groupBy { it.category }
                                .mapNotNull { (cat, dishes) ->
                                    if (dishes.isEmpty()) null else cat to dishes
                                }
                        } else {
                            val dishes = menu.filter { it.category == sel }
                            if (dishes.isEmpty()) emptyList() else listOf(sel to dishes)
                        }
                    }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    menuSections.forEach { (categoryTitle, dishes) ->
                        item(key = "hdr_$categoryTitle") {
                            Text(
                                text = categoryTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        lazyItems(dishes, key = { it.id }) { item ->
                            MenuRow(item, cart[item.id] ?: 0) { delta ->
                                vm.addToCart(item.id, delta)
                            }
                        }
                    }
                }
            }
            // Recent tickets (above checkout — stays visible when scrolling menu ends)
            if (openOrders.isNotEmpty()) {
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
                        lazyItems(openOrders, key = { it.order.id }) { ow ->
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
                    Text(
                        "Cart total: ${formatCents(cartTotal)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
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
    }

    @Composable
    private fun MenuRow(
        item: MenuItemEntity,
        qty: Int,
        onDelta: (Int) -> Unit,
    ) {
        if (item.isAvailable) {
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
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    Text(formatCents(item.priceCents), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onDelta(-1) }, enabled = qty > 0) { Text("−") }
                    Text("$qty", modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { onDelta(1) }) { Text("+") }
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
                Text("Order ID: $orderId · ${labelOrderStatus(status)} · ${formatCents(totalCents)}")
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
                            Text("Void")
                        }
                    }
                }
            }
        }
    }

    private fun labelOrderStatus(s: String): String =
        when (s) {
            OrderStatus.OPEN -> "Open"
            OrderStatus.IN_KITCHEN -> "In kitchen"
            OrderStatus.READY -> "Ready"
            OrderStatus.SERVED -> "Served"
            OrderStatus.PAID -> "Paid"
            OrderStatus.CANCELLED -> "Void"
            else -> s
        }

    @Composable
    fun Kitchen(vm: RestaurantViewModel) {
        val orders by vm.openOrders.collectAsState()
        val menu by vm.menu.collectAsState()
        val menuMap = remember(menu) { menu.associateBy { it.id } }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(
                title = "Kitchen",
                subtitle = "Tap a line to bump status",
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
                    orders.filter {
                        it.order.status != OrderStatus.PAID && it.order.status != OrderStatus.CANCELLED
                    },
                    key = { it.order.id },
                ) { ow ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Order ID: ${ow.order.id} · ${labelOrderStatus(ow.order.status)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
