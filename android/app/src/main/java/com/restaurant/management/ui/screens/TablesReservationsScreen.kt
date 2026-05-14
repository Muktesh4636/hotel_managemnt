package com.restaurant.management.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurant.management.RestaurantApplication
import com.restaurant.management.data.local.entity.TableEntity
import com.restaurant.management.data.local.entity.TableReservationEntity
import com.restaurant.management.model.ReservationStatus
import com.restaurant.management.model.TableStatus
import com.restaurant.management.model.reservationStatusLabel
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM, HH:mm").withZone(ZoneId.systemDefault())

private fun nextTableStatus(current: String): String =
    when (current) {
        TableStatus.FREE -> TableStatus.OCCUPIED
        TableStatus.OCCUPIED -> TableStatus.NEEDS_CLEAN
        else -> TableStatus.FREE
    }

private fun tableStatusHuman(status: String): String =
    when (status) {
        TableStatus.FREE -> "Free"
        TableStatus.OCCUPIED -> "Occupied"
        TableStatus.NEEDS_CLEAN -> "Needs clean"
        else -> status
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TablesReservationsScreen(vm: RestaurantViewModel) {
    val app = LocalContext.current.applicationContext as RestaurantApplication
    val context = LocalContext.current
    val tables by vm.tables.collectAsState()
    val reservations by vm.tableReservations.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var guest by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var party by remember { mutableStateOf("2") }
    var startHours by remember { mutableStateOf("1") }
    var durHours by remember { mutableStateOf("3") }
    var notes by remember { mutableStateOf("") }
    var tablePick by remember { mutableStateOf<Long?>(null) }

    var showAddTable by remember { mutableStateOf(false) }
    var newTblLabel by remember { mutableStateOf("") }
    var newTblSec by remember { mutableStateOf("Main") }
    var delTableId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        vm.syncPullIfConnected(app)
    }

    fun tableLabel(id: Long?): String {
        if (id == null) return "Table not assigned yet"
        val t = tables.find { it.id == id } ?: return "#$id"
        return "${t.section} · ${t.label}"
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "Tables & reservations",
            subtitle = "Floor status and bookings — same data as the web CRM",
            accent = HeaderAccent.Secondary,
            decorationResId = null,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                "Floor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tap + to add a table. Labels sync to this app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                IconButton(onClick = { showAddTable = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add table")
                }
            }
            Spacer(Modifier.height(8.dp))
            val bySection = tables.groupBy { it.section.ifBlank { "Main" } }.toSortedMap()
            bySection.forEach { (sec, list) ->
                Text(
                    sec,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    list.sortedBy { it.label }.forEach { t ->
                        TableStatusChip(
                            table = t,
                            vm = vm,
                            onRequestDelete = { delTableId = t.id },
                        )
                    }
                }
            }
            if (tables.isEmpty()) {
                Text(
                    "No tables yet. Tap + above, or sync after adding tables on the web.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reservations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = { showAdd = true }) {
                    Text("New")
                }
            }
            Spacer(Modifier.height(8.dp))
            val sorted = reservations.sortedBy { it.startEpochMillis }
            if (sorted.isEmpty()) {
                Text("No reservations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                sorted.forEach { r ->
                    ReservationCard(
                        r = r,
                        tableLabel = { tableLabel(it) },
                        onStatus = { st -> vm.updateReservationStatus(r.id, st) },
                        onDelete = { vm.deleteReservation(r.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddTable) {
        AlertDialog(
            onDismissRequest = { showAddTable = false },
            title = { Text("Add table") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Use labels that match your floor. Each venue chooses how many tables to track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = newTblLabel,
                        onValueChange = { newTblLabel = it },
                        label = { Text("Table label") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newTblSec,
                        onValueChange = { newTblSec = it },
                        label = { Text("Section") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val lab = newTblLabel.trim()
                        if (lab.isEmpty()) {
                            Toast.makeText(context, "Enter a table label", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        vm.addVenueTable(lab, newTblSec) { ok ->
                            if (ok) {
                                showAddTable = false
                                newTblLabel = ""
                                newTblSec = "Main"
                                Toast.makeText(context, "Table added", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not add (check connection)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTable = false }) { Text("Cancel") }
            },
        )
    }

    val pendingDel = delTableId
    if (pendingDel != null) {
        AlertDialog(
            onDismissRequest = { delTableId = null },
            title = { Text("Remove table?") },
            text = {
                Text(
                    "Reservations linked to this table will show as “Any table”. You can add the table again anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingDel
                        delTableId = null
                        vm.deleteVenueTable(id)
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { delTableId = null }) { Text("Cancel") }
            },
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New reservation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = guest,
                        onValueChange = { guest = it },
                        label = { Text("Guest name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = party,
                        onValueChange = { party = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Number of guests") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = startHours,
                        onValueChange = { startHours = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Starts in (hours from now)") },
                        supportingText = { Text("When they are expected to arrive.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = durHours,
                        onValueChange = { durHours = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Approx. length (hours)") },
                        supportingText = {
                            Text(
                                "Rough guide for your calendar — not a strict checkout. Pick a generous time; you can edit the booking if they stay longer.",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Which table?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Optional. Choose now, or assign when guests arrive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    var menuOpen by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp),
                    ) {
                        Text(
                            if (tablePick == null) {
                                "Tap here to pick a table"
                            } else {
                                tableLabel(tablePick)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Not assigned — pick a table later",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                tablePick = null
                                menuOpen = false
                            },
                        )
                        tables.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${t.section} · ${t.label}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    tablePick = t.id
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val g = guest.trim()
                        if (g.isEmpty()) {
                            Toast.makeText(context, "Enter guest name", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val ps = party.toIntOrNull()?.coerceAtLeast(1) ?: 2
                        val sh = startHours.toIntOrNull()?.coerceAtLeast(0) ?: 1
                        val dh = durHours.toIntOrNull()?.coerceAtLeast(1) ?: 3
                        val start = System.currentTimeMillis() + sh * 3_600_000L
                        val end = start + dh * 3_600_000L
                        vm.createTableReservation(
                            tableId = tablePick,
                            guestName = g,
                            phone = phone.trim(),
                            partySize = ps,
                            startMillis = start,
                            endMillis = end,
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                            onDone = { ok ->
                                if (ok) {
                                    showAdd = false
                                    guest = ""
                                    phone = ""
                                    party = "2"
                                    startHours = "1"
                                    durHours = "3"
                                    notes = ""
                                    tablePick = null
                                } else {
                                    Toast.makeText(context, "Could not save (check connection)", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            },
        )
    }
}

private fun tableStatusCardColors(status: String): Pair<Color, Color> =
    when (status) {
        TableStatus.FREE -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
        TableStatus.OCCUPIED -> Pair(Color(0xFFC62828), Color(0xFFFFEBEE))
        else -> Pair(Color(0xFFEF6C00), Color(0xFFFFF3E0))
    }

/** Same footprint for every table so cycling status does not resize the grid. */
private val TableStatusCardWidth = 184.dp
private val TableStatusCardHeight = 214.dp
private val TableStatusCycleButtonHeight = 96.dp

@Composable
private fun TableStatusChip(
    table: TableEntity,
    vm: RestaurantViewModel,
    onRequestDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val (borderColor, bgColor) = tableStatusCardColors(table.status)
    Card(
        modifier = Modifier.size(TableStatusCardWidth, TableStatusCardHeight),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        colors =
            CardDefaults.cardColors(
                containerColor = bgColor,
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                table.label,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedButton(
                onClick = { vm.setTableStatus(table.id, nextTableStatus(table.status)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TableStatusCycleButtonHeight),
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        tableStatusHuman(table.status),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Tap for next · Free → Occupied → Needs clean",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            TextButton(
                onClick = onRequestDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReservationCard(
    r: TableReservationEntity,
    tableLabel: (Long?) -> String,
    onStatus: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(r.guestName, fontWeight = FontWeight.SemiBold)
            Text(
                "${r.partySize} guests · ${tableLabel(r.tableId)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${timeFmt.format(Instant.ofEpochMilli(r.startEpochMillis))} → ${timeFmt.format(Instant.ofEpochMilli(r.endEpochMillis))}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!r.notes.isNullOrBlank()) {
                Text(r.notes, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(reservationStatusLabel(r.status), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { open = true }) { Text("Change") }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        onDelete()
                    },
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                listOf(
                    ReservationStatus.PENDING,
                    ReservationStatus.CONFIRMED,
                    ReservationStatus.SEATED,
                    ReservationStatus.COMPLETED,
                    ReservationStatus.CANCELLED,
                    ReservationStatus.NO_SHOW,
                ).forEach { st ->
                    DropdownMenuItem(
                        text = { Text(reservationStatusLabel(st)) },
                        onClick = {
                            open = false
                            onStatus(st)
                        },
                    )
                }
            }
        }
    }
}
