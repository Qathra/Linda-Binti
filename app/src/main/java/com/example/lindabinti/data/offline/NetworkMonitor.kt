package com.example.lindabinti.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.example.lindabinti.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Best-effort connectivity for optional cloud features.
 * SOS SMS does not use this — SMS uses the mobile network, not this probe.
 */
class NetworkMonitor(private val context: Context) {

    private val _isOnline = MutableStateFlow(probeOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun isInternetLikely(): Boolean = _isOnline.value || probeOnline().also { _isOnline.value = it }

    /** Register for live Online/Offline updates (safe to call more than once). */
    fun start() {
        if (callback != null) {
            _isOnline.value = probeOnline()
            return
        }
        val cm = connectivityManager() ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = probeOnline()
            }

            override fun onLost(network: Network) {
                _isOnline.value = probeOnline()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _isOnline.value = capsSuggestOnline(caps) || probeOnline()
            }
        }
        callback = cb
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(cb)
            } else {
                cm.registerNetworkCallback(request, cb)
            }
        }
        _isOnline.value = probeOnline()
    }

    fun stop() {
        val cm = connectivityManager() ?: return
        val cb = callback ?: return
        runCatching { cm.unregisterNetworkCallback(cb) }
        callback = null
    }

    private fun connectivityManager(): ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private fun probeOnline(): Boolean {
        val cm = connectivityManager() ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return capsSuggestOnline(caps)
    }

    private fun capsSuggestOnline(caps: NetworkCapabilities): Boolean {
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return true
        // Debug LAN backend: phone only needs Wi-Fi to reach the laptop (no public internet).
        if (BuildConfig.DEBUG && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return true
        return false
    }
}
