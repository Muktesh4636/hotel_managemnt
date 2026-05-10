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
import com.restaurant.management.ui.RestaurantRoot
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.RestaurantViewModelFactory
import com.restaurant.management.ui.theme.RestaurantManagementTheme

class MainActivity : androidx.activity.ComponentActivity() {

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
        setContent {
            RestaurantManagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RestaurantRoot(viewModel)
                }
            }
        }
    }
}
