package com.restaurant.management

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.restaurant.management.ui.AuthViewModel
import com.restaurant.management.ui.AuthViewModelFactory
import com.restaurant.management.ui.RestaurantRoot
import com.restaurant.management.ui.RestaurantViewModel
import com.restaurant.management.ui.RestaurantViewModelFactory
import com.restaurant.management.ui.screens.AuthScreens
import com.restaurant.management.ui.theme.RestaurantManagementTheme

class MainActivity : androidx.activity.ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            val app = application as RestaurantApplication
            val session by app.sessionUserId.collectAsStateWithLifecycle(
                initialValue = app.sessionUserId.value,
            )
            RestaurantManagementTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (session == null) {
                        val authVm: AuthViewModel =
                            viewModel(factory = AuthViewModelFactory(application))
                        AuthScreens(vm = authVm)
                    } else {
                        val vm: RestaurantViewModel =
                            viewModel(
                                key = session.toString(),
                                factory =
                                    RestaurantViewModelFactory(
                                        app.requireRestaurantRepository(),
                                    ),
                            )
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner, session) {
                            val observer =
                                LifecycleEventObserver { _, event ->
                                    if (event == Lifecycle.Event.ON_RESUME) {
                                        val app = application as RestaurantApplication
                                        if (app.networkMonitor.online.value) {
                                            vm.syncPullIfConnected(application)
                                        }
                                    }
                                }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }
                        RestaurantRoot(vm)
                    }
                }
            }
        }
    }
}
