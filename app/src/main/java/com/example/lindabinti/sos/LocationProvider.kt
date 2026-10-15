package com.example.lindabinti.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class SosLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val status: String,
    val capturedAtMillis: Long? = null
) {
    /** True when SMS can include coordinates (never 0,0). */
    fun hasFix(): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return true
    }
}

/**
 * Emergency location only. No background tracking.
 * Listeners stay on while SOS is running so indoor/network fixes can arrive
 * after the first SMS, then go out as a location-update SMS.
 */
class LocationProvider(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionStarted = AtomicBoolean(false)
    private val sosActive = AtomicBoolean(false)
    private val liveFix = AtomicReference<SosLocation?>(null)
    private var gpsListener: LocationListener? = null
    private var fusedCallback: LocationCallback? = null

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Phone Location toggle (Quick Settings). Permission alone is not enough. */
    fun isSystemLocationEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (_: Exception) {
            true
        }
    }

    fun peekUiStatus(nowMs: Long = System.currentTimeMillis()): SosLocationPeek {
        if (!hasPermission()) return SosLocationPeek.UNAVAILABLE
        if (!isSystemLocationEnabled()) return SosLocationPeek.UNAVAILABLE
        if (bestAvailable(nowMs)?.hasFix() == true) return SosLocationPeek.AVAILABLE
        return SosLocationPeek.ATTEMPTING
    }

    /**
     * Start GPS/network listeners immediately (calculator SOS / Emergency / Home).
     * Safe to call more than once. Never sends SMS.
     */
    fun beginEmergencyFix() {
        harvestInstant()
        if (!hasPermission() || !isSystemLocationEnabled()) return
        if (!sessionStarted.compareAndSet(false, true)) return
        runOnMain { registerListeners() }
        pokeFusedLastLocation()
    }

    fun beginSosHold() {
        sosActive.set(true)
        beginEmergencyFix()
    }

    fun endSosHold() {
        sosActive.set(false)
        endEmergencyFix()
    }

    fun endIfIdle() {
        if (!sosActive.get()) endEmergencyFix()
    }

    fun endEmergencyFix() {
        if (!sessionStarted.compareAndSet(true, false)) return
        runOnMain { unregisterListeners() }
    }

    suspend fun warmCache(timeoutMs: Long = 8_000L) {
        if (!hasPermission() || !isSystemLocationEnabled()) return
        beginEmergencyFix()
        getForSos(timeoutMs = timeoutMs)
    }

    /**
     * Uses a live fix if listeners already got one, else last-known, else waits
     * for GPS/network. Does not block on Play Services Tasks.await.
     */
    suspend fun getForSos(timeoutMs: Long = SosLocationRules.SOS_TIMEOUT_MS): SosLocation {
        if (!hasPermission()) return unavailable()
        beginEmergencyFix()

        bestAvailable()?.let { return it }

        if (!isSystemLocationEnabled()) {
            return rememberedSos() ?: unavailable()
        }

        val wait = timeoutMs.coerceAtLeast(0L)
        if (wait <= 0L) return rememberedSos() ?: unavailable()

        val deadline = System.currentTimeMillis() + wait
        while (System.currentTimeMillis() < deadline) {
            bestAvailable()?.let { return it }
            delay(POLL_MS)
        }
        return bestAvailable() ?: rememberedSos() ?: unavailable()
    }

    private fun bestAvailable(nowMs: Long = System.currentTimeMillis()): SosLocation? {
        liveFix.get()?.takeIf { it.hasFix() }?.let { return it }
        pickForSos(lastKnownFromSystem(), nowMs)?.let { loc ->
            remember(loc)
            liveFix.compareAndSet(null, loc)
            return loc
        }
        return rememberedSos()
    }

    private fun harvestInstant() {
        val now = System.currentTimeMillis()
        pickForSos(lastKnownFromSystem(), now)?.let { loc ->
            remember(loc)
            liveFix.set(loc)
        }
        rememberedSos()?.let { cached ->
            if (liveFix.get()?.hasFix() != true) liveFix.set(cached)
        }
    }

    private fun onIncoming(location: Location) {
        if (!hasUsableCoords(location)) return
        val after = System.currentTimeMillis()
        val status = SosLocationRules.classifyAgeForSos(location.time, after) ?: "ok"
        val sos = toSos(location, status)
        liveFix.set(sos)
        remember(sos)
    }

    @SuppressLint("MissingPermission")
    private fun registerListeners() {
        if (!hasPermission() || !isSystemLocationEnabled()) return
        if (gpsListener != null) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onIncoming(location)
            }
        }
        gpsListener = listener
        for (provider in locationProviders()) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::onIncoming)
            }
        }
        fusedCallback = callback
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(0L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        try {
            val balanced = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(0L)
                .setWaitForAccurateLocation(false)
                .build()
            client.requestLocationUpdates(balanced, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
        pokeFusedLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun pokeFusedLastLocation() {
        if (!hasPermission()) return
        try {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) onIncoming(loc)
            }
        } catch (_: Exception) {
        }
        try {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { loc ->
                if (loc != null) onIncoming(loc)
            }
        } catch (_: Exception) {
        }
        try {
            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { loc ->
                if (loc != null) onIncoming(loc)
            }
        } catch (_: Exception) {
        }
    }

    private fun unregisterListeners() {
        gpsListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (_: Exception) {
            }
        }
        gpsListener = null
        fusedCallback?.let {
            try {
                client.removeLocationUpdates(it)
            } catch (_: Exception) {
            }
        }
        fusedCallback = null
    }

    private fun locationProviders(): List<String> {
        val providers = mutableListOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providers.add(0, LocationManager.FUSED_PROVIDER)
        }
        return providers
    }

    private fun hasUsableCoords(loc: Location): Boolean {
        if (!loc.latitude.isFinite() || !loc.longitude.isFinite()) return false
        if (loc.latitude == 0.0 && loc.longitude == 0.0) return false
        return true
    }

    private fun pickForSos(candidates: List<Location>, nowMs: Long): SosLocation? {
        val ranked = candidates.mapNotNull { loc ->
            if (!hasUsableCoords(loc)) return@mapNotNull null
            val status = SosLocationRules.classifyAgeForSos(loc.time, nowMs) ?: "cached"
            loc to status
        }.sortedByDescending { it.first.time }
        if (ranked.isEmpty()) return null
        val fresh = ranked.firstOrNull { it.second == "ok" }
        return if (fresh != null) toSos(fresh.first, "ok")
        else toSos(ranked.first().first, "cached")
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownFromSystem(): List<Location> {
        if (!hasPermission()) return emptyList()
        val out = mutableListOf<Location>()
        for (provider in locationProviders()) {
            try {
                locationManager.getLastKnownLocation(provider)?.let(out::add)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }
        return out
    }

    private fun toSos(location: Location, status: String): SosLocation =
        SosLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            status = status,
            capturedAtMillis = location.time.takeIf { it > 0L }
        )

    private fun remember(loc: SosLocation) {
        if (!loc.hasFix()) return
        context.getSharedPreferences(LAST_FIX_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAT, loc.latitude.toString())
            .putString(KEY_LNG, loc.longitude.toString())
            .putFloat(KEY_ACC, loc.accuracyMeters ?: -1f)
            .putLong(KEY_AT, loc.capturedAtMillis ?: System.currentTimeMillis())
            .apply()
    }

    private fun rememberedSos(): SosLocation? {
        val prefs = context.getSharedPreferences(LAST_FIX_PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
        if (!lat.isFinite() || !lng.isFinite()) return null
        if (lat == 0.0 && lng == 0.0) return null
        val acc = prefs.getFloat(KEY_ACC, -1f).takeIf { it >= 0f }
        val at = prefs.getLong(KEY_AT, 0L).takeIf { it > 0L }
        return SosLocation(lat, lng, acc, "cached", at)
    }

    private fun unavailable(): SosLocation =
        SosLocation(0.0, 0.0, null, "unavailable", null)

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    companion object {
        private const val LAST_FIX_PREFS = "linda_sos_last_fix"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_ACC = "acc"
        private const val KEY_AT = "at"
        private const val POLL_MS = 300L
    }
}
