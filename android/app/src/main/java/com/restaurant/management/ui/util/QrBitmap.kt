package com.restaurant.management.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun encodeQrBitmap(
    content: String,
    sizePx: Int = 768,
): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix =
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val w = matrix.width
    val h = matrix.height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] =
                if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

fun saveQrBitmapToPictures(
    context: Context,
    bitmap: Bitmap,
    venueNameForFile: String,
): Boolean {
    val safe =
        venueNameForFile
            .replace(" ", "-")
            .filter { it.isLetterOrDigit() || it == '-' }
            .ifBlank { "menu-qr" }
    val resolver = context.contentResolver
    val displayName = "$safe-qr.png"
    val values =
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/RestaurantManagement",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
    val uri =
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
    return try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        Toast.makeText(
            context,
            "Saved to Pictures/RestaurantManagement",
            Toast.LENGTH_LONG,
        ).show()
        true
    } catch (_: Exception) {
        false
    }
}
