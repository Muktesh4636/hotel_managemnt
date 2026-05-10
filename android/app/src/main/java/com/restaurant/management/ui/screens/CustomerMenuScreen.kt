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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.restaurant.management.data.local.entity.MenuItemEntity
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.util.formatCents
import com.restaurant.management.ui.visual.MenuItemImageBadge

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
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val byCat = menu.groupBy { it.category }
                    byCat.forEach { (cat, dishes) ->
                        item(key = "hdr_$cat") {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
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
                }
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            "Total: ${formatCents(cartTotal)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    vm.placeGuestMenuOrder {
                                        Toast.makeText(
                                            context,
                                            "Order sent to the kitchen",
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
