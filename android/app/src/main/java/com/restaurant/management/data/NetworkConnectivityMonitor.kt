package com.restaurant.management.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the device has validated internet (not just Wi‑Fi association).
 * Used to gate venue UI so menu, orders, and sync are only used when online.
 */
class NetworkConnectivityMonitor(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _online = MutableStateFlow(computeOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                emit()
            }

            override fun onLost(network: Network) {
                emit()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                emit()
            }
        }

    init {
        cm.registerDefaultNetworkCallback(callback)
        emit()
    }

    private fun emit() {
        _online.value = computeOnline()
    }

    private fun computeOnline(): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
