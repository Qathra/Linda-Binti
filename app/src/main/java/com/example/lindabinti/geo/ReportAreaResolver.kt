package com.example.lindabinti.geo

import com.example.lindabinti.sos.LocationProvider
import com.example.lindabinti.sos.SosLocationRules

/**
 * Best-effort area lookup for ordinary reports and SOS NGO flags.
 * Converts a one-time device location into a coarse Kajiado sub-county.
 */
class ReportAreaResolver(
    private val locationProvider: LocationProvider
) {
    fun hasLocationPermission(): Boolean = locationProvider.hasPermission()

    suspend fun resolveAreaOrNull(): ReportArea? {
        if (!locationProvider.hasPermission()) return null
        val loc = locationProvider.getForSos(timeoutMs = SosLocationRules.SOS_TIMEOUT_MS)
        if (loc.status == "unavailable") return null
        return KajiadoGeoAreas.fromCoordinates(loc.latitude, loc.longitude)
    }
}
