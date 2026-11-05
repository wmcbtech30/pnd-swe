package com.paninda.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.launch
import com.paninda.app.domain.model.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    fun current(): NetworkStatus = status(manager.getNetworkCapabilities(manager.activeNetwork))
    val changes = callbackFlow {
        var active = manager.activeNetwork
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val changed = active != network
                active = network
                // Keep the initial validated snapshot; a newly switched network awaits validation.
                if (changed) trySend(NetworkStatus.OFFLINE)
                if (Build.VERSION.SDK_INT < 26) launch {
                    // API 24–25 do not guarantee the immediate capabilities callback.
                    if (active == network) trySend(status(manager.getNetworkCapabilities(network)))
                }
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (network == active) trySend(status(capabilities))
            }
            override fun onLost(network: Network) {
                if (network == active) { active = null; trySend(NetworkStatus.OFFLINE) }
            }
            override fun onUnavailable() { trySend(NetworkStatus.OFFLINE) }
        }
        manager.registerDefaultNetworkCallback(callback)
        trySend(current())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun status(capabilities: NetworkCapabilities?): NetworkStatus =
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) NetworkStatus.ONLINE
        else NetworkStatus.OFFLINE
}
