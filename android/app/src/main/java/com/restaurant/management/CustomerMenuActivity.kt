package com.restaurant.management

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.RestaurantViewModelFactory
import com.restaurant.management.ui.screens.CustomerMenuScreen
import com.restaurant.management.ui.theme.RestaurantManagementTheme

/**
 * Opened when a guest scans the venue QR (`restaurantmgmt://customer-menu?t=…`).
 * Uses the same local database as the staff app on this device.
 */
class CustomerMenuActivity : androidx.activity.ComponentActivity() {

    private val viewModel: RestaurantViewModel by viewModels {
        RestaurantViewModelFactory((application as RestaurantApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val token = intent?.data?.getQueryParameter("t").orEmpty()

        setContent {
            RestaurantManagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CustomerMenuScreen(
                        qrToken = token,
                        vm = viewModel,
                        onClose = { finish() },
                    )
                }
            }
        }
    }
}
