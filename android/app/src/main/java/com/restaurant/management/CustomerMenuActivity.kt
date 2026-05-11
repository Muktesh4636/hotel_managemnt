package com.restaurant.management

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.RestaurantViewModelFactory
import com.restaurant.management.ui.screens.CustomerMenuScreen
import com.restaurant.management.ui.theme.RestaurantManagementTheme

/**
 * Opened when a guest scans the venue QR (`restaurantmgmt://customer-menu?t=…`).
 * Uses the signed-in staff account's venue database on this device.
 */
class CustomerMenuActivity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as RestaurantApplication
        if (!app.ensureRestaurantRepositoryForGuest()) {
            Toast.makeText(
                this,
                "Open the staff app and sign in once on this phone to use the guest menu.",
                Toast.LENGTH_LONG,
            ).show()
            finish()
            return
        }
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        val token = intent?.data?.getQueryParameter("t").orEmpty()

        setContent {
            val viewModel: RestaurantViewModel =
                viewModel(
                    factory =
                        RestaurantViewModelFactory(
                            app.requireRestaurantRepository(),
                        ),
                )
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
