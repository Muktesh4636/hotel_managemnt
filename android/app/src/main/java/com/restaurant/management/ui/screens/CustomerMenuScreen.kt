package com.restaurant.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.util.formatCents
import com.restaurant.management.ui.visual.MenuItemImageBadge
import kotlinx.coroutines.launch

@Composable
fun CustomerMenuScreen(
    qrToken: String,
    vm: RestaurantViewModel,
    onClose: () -> Unit,
) {
    val menu by vm.menu.collectAsState()
    val guestCart by vm.guestCart.collectAsState()
    val settings by vm.settings.collectAsState()
    var valid by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current

    LaunchedEffect(qrToken) {
        valid =
            if (qrToken.isBlank()) {
                false
            } else {
                vm.validateQrMenuToken(qrToken)
            }
    }

    val cartTotal =
        remember(guestCart, menu) {
            guestCart.entries.sumOf { (id, qty) ->
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
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        settings?.venueName ?: "Menu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "Guest order",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                    )
                }
                OutlinedButton(onClick = onClose) {
                    Text("Close")
                }
            }
        }

        when (valid) {
            null ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }

            false ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "This menu link isn’t valid for this restaurant.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.padding(16.dp))
                    Button(onClick = onClose) {
                        Text("Close")
                    }
                }

            true -> {
                var showGuestSummary by remember { mutableStateOf(false) }
                val guestMenu =
                    remember(menu) {
                        menu.filter { it.isAvailable }
                    }
                val categoryOrder =
                    remember(guestMenu) {
                        guestMenu.map { it.category.trim().ifEmpty { "Menu" } }.distinct()
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

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    Text(
                        "Swipe sideways for categories, or tap a chip.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        items(categoryOrder, key = { it }) { cat ->
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
                        beyondViewportPageCount = 0,
                    ) { page ->
                        val pageCategory = navCategories.getOrNull(page)
                        val menuSections =
                            remember(guestMenu, pageCategory) {
                                val sel = pageCategory
                                if (sel == null) {
                                    guestMenu
                                        .groupBy { it.category.trim().ifEmpty { "Menu" } }
                                        .mapNotNull { (cat, dishes) ->
                                            if (dishes.isEmpty()) null else cat to dishes
                                        }
                                } else {
                                    val dishes =
                                        guestMenu.filter {
                                            it.category.trim().ifEmpty { "Menu" } == sel
                                        }
                                    if (dishes.isEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(sel to dishes)
                                    }
                                }
                            }
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (guestMenu.isEmpty()) {
                                item {
                                    Text(
                                        "No dishes available right now.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 16.dp),
                                    )
                                }
                            } else {
                                menuSections.forEach { (categoryTitle, dishes) ->
                                    item(key = "hdr_${page}_$categoryTitle") {
                                        Text(
                                            text = categoryTitle,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                        )
                                    }
                                    items(dishes, key = { it.id }) { dish ->
                                        GuestMenuRow(
                                            item = dish,
                                            qty = guestCart[dish.id] ?: 0,
                                            onDelta = { d -> vm.addToGuestCart(dish.id, d) },
                                        )
                                    }
                                }
                                if (menuSections.isEmpty()) {
                                    item {
                                        Text(
                                            "No dishes in this category.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Column(Modifier.padding(16.dp)) {
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
                                    "Total: ${formatCents(cartTotal)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = { showGuestSummary = true },
                                    enabled = guestCart.values.any { it > 0 },
                                ) {
                                    Text("Order summary")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        vm.placeGuestMenuOrder { orderId ->
                                            val msg =
                                                if (orderId != null) {
                                                    "Order #$orderId sent to the kitchen"
                                                } else {
                                                    "Order sent to the kitchen"
                                                }
                                            Toast.makeText(
                                                context,
                                                msg,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    },
                                    enabled = guestCart.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Place order")
                                }
                                OutlinedButton(
                                    onClick = { vm.clearGuestCart() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }

                if (showGuestSummary) {
                    val scroll = rememberScrollState()
                    val priceById = remember(guestMenu) { guestMenu.associate { it.id to it.priceCents } }
                    val nameById = remember(guestMenu) { guestMenu.associate { it.id to it.name } }
                    AlertDialog(
                        onDismissRequest = { showGuestSummary = false },
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
                                guestCart.entries
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
                            TextButton(onClick = { showGuestSummary = false }) {
                                Text("Close")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GuestMenuRow(
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
