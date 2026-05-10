package com.restaurant.management.ui.util

import java.net.URLEncoder

/** Deep link opened when guests scan the QR (same app must be installed). */
fun customerMenuDeepLink(qrMenuToken: String): String =
    "restaurantmgmt://customer-menu?t=" +
        URLEncoder.encode(qrMenuToken, Charsets.UTF_8.name())
