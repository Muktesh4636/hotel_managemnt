package com.restaurant.management.ui.util

import java.net.URLEncoder

/** Deep link opened when guests scan the QR (same app must be installed). */
fun customerMenuDeepLink(qrMenuToken: String): String =
    "restaurantmgmt://customer-menu?t=" +
        URLEncoder.encode(qrMenuToken, Charsets.UTF_8.name())

/**
 * HTTPS page guests open in any browser (no app). [baseUrl] is the venue server root only,
 * e.g. `https://pimux.store` — same value as Global settings → Log in to backend.
 */
fun customerMenuWebUrl(
    baseUrl: String,
    qrMenuToken: String,
): String {
    val base = baseUrl.trim().trimEnd('/')
    val enc = URLEncoder.encode(qrMenuToken.trim(), Charsets.UTF_8.name())
    return "$base/menu/$enc/"
}
