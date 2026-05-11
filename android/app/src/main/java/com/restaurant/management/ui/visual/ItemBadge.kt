package com.restaurant.management.ui.visual

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restaurant.management.R
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Matches bistro theme: terracotta, sage, honey, muted accents */
private val AccentRingColors =
    listOf(
        Color(0xFFC2543D),
        Color(0xFF3D6B58),
        Color(0xFFD4A574),
        Color(0xFF5B8FA8),
        Color(0xFFE8A598),
        Color(0xFF8B7355),
    )

private fun accentIndexForId(id: Long): Int {
    val n = AccentRingColors.size
    val m = ((id % n) + n) % n
    return m.toInt()
}

/** List-row thumbnails: small decoded bitmaps, reused while scrolling POS / menus. */
private const val MENU_THUMB_MAX_SIDE = 256

private val menuThumbImageCache =
    object : LruCache<String, ImageBitmap>(48) {}

private fun computeInSampleSize(
    opts: BitmapFactory.Options,
    maxSide: Int,
): Int {
    val height = opts.outHeight
    val width = opts.outWidth
    if (width <= 0 || height <= 0) return 1
    var inSampleSize = 1
    if (height > maxSide || width > maxSide) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize > maxSide || halfWidth / inSampleSize > maxSide) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}

private fun decodeBitmapSampledFromFile(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts =
        BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds, MENU_THUMB_MAX_SIDE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    return BitmapFactory.decodeFile(path, opts)
}

private fun decodeBitmapSampledFromNetworkStream(stream: InputStream): Bitmap? {
    val bytes = stream.use { it.readBytes() }
    if (bytes.isEmpty() || bytes.size > 4 * 1024 * 1024) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts =
        BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds, MENU_THUMB_MAX_SIDE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

private fun decodeMenuThumbBitmap(path: String): Bitmap? =
    when {
        path.startsWith("http://") || path.startsWith("https://") ->
            runCatching {
                val conn = URL(path).openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 20000
                conn.inputStream.use { decodeBitmapSampledFromNetworkStream(it) }
            }.getOrNull()
        else -> {
            val f = File(path)
            if (f.exists()) decodeBitmapSampledFromFile(path) else null
        }
    }

@Composable
private fun ResolvedMenuItemImage(
    itemName: String,
    category: String,
    customPhotoPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val fallbackRes =
        remember(itemName, category) {
            foodPhotoForMenuItem(itemName, category)
        }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(customPhotoPath) {
        val p = customPhotoPath?.trim().orEmpty()
        if (p.isEmpty()) {
            bitmap = null
            return@LaunchedEffect
        }
        val cached = menuThumbImageCache.get(p)
        if (cached != null) {
            bitmap = cached
            return@LaunchedEffect
        }
        val decoded =
            withContext(Dispatchers.IO) {
                decodeMenuThumbBitmap(p)?.asImageBitmap()?.also { img ->
                    menuThumbImageCache.put(p, img)
                }
            }
        bitmap = decoded
    }
    val b = bitmap
    if (b != null) {
        Image(
            bitmap = b,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Image(
            painter = painterResource(fallbackRes),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/** Square crop for lists/carousels (badge framing lives in the caller). */
@Composable
fun MenuItemPhotoThumbnail(
    itemName: String,
    category: String,
    customPhotoPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    ResolvedMenuItemImage(
        itemName = itemName,
        category = category,
        customPhotoPath = customPhotoPath,
        modifier = modifier,
        contentScale = contentScale,
    )
}

/**
 * One dish photo for reports carousel; [highlighted] draws a stronger ring when this page is centered.
 */
@Composable
fun ReportCarouselPhotoTile(
    menuItemId: Long,
    itemName: String,
    category: String,
    customPhotoPath: String?,
    quantity: Int,
    photoSize: Dp = 72.dp,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ring = AccentRingColors[accentIndexForId(menuItemId)]
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(photoSize)
                .border(
                    width = if (highlighted) 3.dp else 1.dp,
                    color =
                        if (highlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            ring.copy(alpha = 0.55f)
                        },
                    shape = RoundedCornerShape(14.dp),
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    ring.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                        ),
                )
                .padding(4.dp),
        ) {
            MenuItemPhotoThumbnail(
                itemName = itemName,
                category = category,
                customPhotoPath = customPhotoPath,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
            )
        }
        if (quantity > 1) {
            Text(
                "×$quantity",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun MenuItemImageBadge(
    itemName: String,
    category: String,
    itemId: Long,
    customPhotoPath: String? = null,
    modifier: Modifier = Modifier,
    /** Softer GPU work while flinging long POS lists (solid frame instead of gradient). */
    lightweightFrame: Boolean = false,
) {
    val ring = AccentRingColors[accentIndexForId(itemId)]
    Box(
        modifier =
            modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (lightweightFrame) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(14.dp),
                        ).border(
                            width = 1.dp,
                            color = ring.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                        )
                    } else {
                        Modifier.background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            ring.copy(alpha = 0.55f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        ),
                                ),
                            shape = RoundedCornerShape(14.dp),
                        )
                    },
                )
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        ResolvedMenuItemImage(
            itemName = itemName,
            category = category,
            customPhotoPath = customPhotoPath,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
        )
    }
}

@Composable
fun InventoryItemBadge(
    itemName: String,
    rowId: Long,
    modifier: Modifier = Modifier,
) {
    val resId = remember(itemName) { foodPhotoForInventoryItem(itemName) }
    val ring = AccentRingColors[accentIndexForId(rowId)]
    Box(
        modifier =
            modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    ring.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
                                ),
                        ),
                )
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

private val HubLeadDrawables =
    listOf(
        R.drawable.decor_plate_meal,
        R.drawable.ic_inventory_stock,
        R.drawable.decor_dining_table,
        R.drawable.decor_chef_hat,
        R.drawable.decor_kitchen_pot,
        R.drawable.ic_fork_knife,
    )

@Composable
fun HubModuleBadge(
    index: Int,
    modifier: Modifier = Modifier,
) {
    val resId = HubLeadDrawables[index % HubLeadDrawables.size]
    val ring = AccentRingColors[index % AccentRingColors.size]
    Box(
        modifier =
            modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    ring.copy(alpha = 0.52f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.38f),
                                ),
                        ),
                )
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(32.dp)
                        .padding(2.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun KitchenLineBadge(
    itemName: String,
    category: String?,
    menuItemId: Long,
    customPhotoPath: String? = null,
    modifier: Modifier = Modifier,
) {
    val ring = AccentRingColors[accentIndexForId(menuItemId)]
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    ring.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                ),
                        ),
                )
                .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        ResolvedMenuItemImage(
            itemName = itemName,
            category = category.orEmpty(),
            customPhotoPath = customPhotoPath,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(9.dp)),
        )
    }
}
