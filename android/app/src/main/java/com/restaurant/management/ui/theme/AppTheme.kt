package com.restaurant.management.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm bistro: terracotta + sage + honey on cream — pairs with food/kitchen imagery
private val Terracotta = Color(0xFFC2543D)
private val Sage = Color(0xFF3D6B58)
private val Honey = Color(0xFFC9933D)
private val Cream = Color(0xFFFFFBF7)
private val Ink = Color(0xFF2C2420)
private val Rose = Color(0xFFB71C1C)

private val RestaurantLightScheme =
    lightColorScheme(
        primary = Terracotta,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE8E0),
        onPrimaryContainer = Color(0xFF5C1F14),
        secondary = Sage,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD4EBE3),
        onSecondaryContainer = Color(0xFF1A3D32),
        tertiary = Honey,
        onTertiary = Color(0xFF2C2410),
        tertiaryContainer = Color(0xFFFFF3D6),
        onTertiaryContainer = Color(0xFF5C4010),
        background = Cream,
        onBackground = Ink,
        surface = Color(0xFFFFFFFF),
        onSurface = Ink,
        surfaceVariant = Color(0xFFF0E8E2),
        onSurfaceVariant = Color(0xFF5C534D),
        outline = Color(0xFFB8A99F),
        outlineVariant = Color(0xFFE0D5CD),
        error = Rose,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD7),
        onErrorContainer = Color(0xFF5C0012),
    )

@Composable
fun RestaurantManagementTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RestaurantLightScheme,
        content = content,
    )
}
