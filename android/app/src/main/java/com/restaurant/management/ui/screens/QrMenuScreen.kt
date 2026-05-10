package com.restaurant.management.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.restaurant.management.R
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.theme.HeaderAccent
import com.restaurant.management.ui.theme.ScreenHeader
import com.restaurant.management.ui.util.customerMenuDeepLink
import com.restaurant.management.ui.util.encodeQrBitmap
import com.restaurant.management.ui.util.saveQrBitmapToPictures

@Composable
fun QrMenuScreen(vm: RestaurantViewModel) {
    val settings by vm.settings.collectAsState()
    val token = settings?.qrMenuToken.orEmpty()
    val venue = settings?.venueName ?: "Restaurant"
    val context = LocalContext.current

    val link =
        remember(token) {
            if (token.isNotBlank()) customerMenuDeepLink(token) else ""
        }
    val bitmap =
        remember(link) {
            if (link.isNotBlank()) encodeQrBitmap(link) else null
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "Customer QR menu",
            subtitle = "Guests scan → browse menu → order to kitchen",
            accent = HeaderAccent.Primary,
            decorationResId = R.drawable.ic_fork_knife,
        )
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Each venue has its own QR code (tied to “${venue}”). Print or save the image and place it on tables.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (token.isBlank()) {
                Text(
                    "Generating your QR link… open again in a moment.",
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    bitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code linking to guest menu",
                            modifier =
                                Modifier
                                    .size(280.dp)
                                    .padding(8.dp),
                        )
                    }
                    Text(
                        "Opens this app’s guest menu. Orders reach Kitchen here when guests use this same device & database (shared tablet works). Other phones need the app plus hosted sync later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        link,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    bitmap?.let {
                        saveQrBitmapToPictures(context, it, venue)
                    }
                },
                enabled = bitmap != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save QR image to gallery")
            }
            OutlinedButton(
                onClick = {
                    val send =
                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, link)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "$venue — menu QR link")
                        }
                    context.startActivity(
                        android.content.Intent.createChooser(send, "Share menu link"),
                    )
                },
                enabled = link.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Share link")
            }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "How it works",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "1. Guest scans QR with their phone.\n" +
                    "2. They pick dishes and tap Place order.\n" +
                    "3. The ticket appears on Kitchen right away.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
