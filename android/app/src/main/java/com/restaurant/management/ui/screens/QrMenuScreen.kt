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
import com.restaurant.management.data.remote.ApiPrefs
import com.restaurant.management.ui.util.customerMenuDeepLink
import com.restaurant.management.ui.util.customerMenuWebUrl
import com.restaurant.management.ui.util.encodeQrBitmap
import com.restaurant.management.ui.util.saveQrBitmapToPictures

@Composable
fun QrMenuScreen(vm: RestaurantViewModel) {
    val settings by vm.settings.collectAsState()
    val token = settings?.qrMenuToken.orEmpty()
    val venue = settings?.venueName ?: "Restaurant"
    val context = LocalContext.current
    val prefs = remember { ApiPrefs(context) }

    val webLink =
        remember(token, prefs.baseUrl) {
            if (token.isNotBlank()) customerMenuWebUrl(prefs.baseUrl, token) else ""
        }
    val appLink =
        remember(token) {
            if (token.isNotBlank()) customerMenuDeepLink(token) else ""
        }
    val webBitmap =
        remember(webLink) {
            if (webLink.isNotBlank()) encodeQrBitmap(webLink) else null
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "Customer QR menu",
            subtitle = "Web QR for any phone — orders reach Kitchen after sync",
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
                "Each restaurant has its own token. The large QR opens your **website menu** in the guest’s browser (set the same server URL under Global settings → Log in to backend). They tap Place order — the ticket is stored on the server and appears in Kitchen after a short refresh or sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (token.isBlank()) {
                Text(
                    "Generating your QR link… open again in a moment.",
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
            Text(
                "“${venue}” — print this for tables",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Guest web menu (recommended)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    webBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code for web guest menu",
                            modifier =
                                Modifier
                                    .size(280.dp)
                                    .padding(8.dp),
                        )
                    }
                    Text(
                        webLink,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Button(
                onClick = {
                    webBitmap?.let {
                        saveQrBitmapToPictures(context, it, "$venue-web-menu-qr")
                    }
                },
                enabled = webBitmap != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save web QR to gallery")
            }
            OutlinedButton(
                onClick = {
                    val send =
                        android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, webLink)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "$venue — guest menu link")
                        }
                    context.startActivity(
                        android.content.Intent.createChooser(send, "Share guest menu link"),
                    )
                },
                enabled = webLink.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Share web guest link")
            }
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "App-only deep link (guest must have this app installed)",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        appLink,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "How it works",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "1. Guest scans the web QR → browser menu for this venue only.\n" +
                    "2. They add dishes and tap Place order.\n" +
                    "3. Kitchen / POS pull from the server (auto-refresh on web; Android Kitchen syncs in the background).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
