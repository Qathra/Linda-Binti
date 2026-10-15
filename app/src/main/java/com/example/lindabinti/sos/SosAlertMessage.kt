package com.example.lindabinti.sos

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a short trusted-contact SOS SMS (aim: one GSM part).
 * English on purpose. Never includes Access PIN, SOS code, or report text.
 */
object SosAlertMessage {

    fun build(
        location: SosLocation,
        triggeredAtMillis: Long,
        category: SosEmergencyCategory = SosEmergencyCategory.GENERAL
    ): String {
        val time = formatTime(triggeredAtMillis)
        return buildString {
            appendLine("LINDA BINTI EMERGENCY ALERT")
            appendLine("This person has activated an emergency alert and needs immediate assistance.")
            if (location.hasFix()) {
                appendLine(mapsUrl(location))
                append("GPS: ")
                append(formatCoord(location.latitude))
                append(",")
                appendLine(formatCoord(location.longitude))
                if (location.status == "cached") {
                    appendLine("Last known location:")
                    location.capturedAtMillis?.let {
                        append("Recorded: ")
                        appendLine(formatTime(it))
                    }
                }
            } else {
                appendLine("Location pending.")
            }
            if (category != SosEmergencyCategory.GENERAL) {
                appendLine(categoryLines(category))
            }
            append("Time: ")
            append(time)
        }.trim()
    }

    fun buildLocationUpdate(
        location: SosLocation,
        capturedAtMillis: Long
    ): String {
        val lat = formatCoord(location.latitude)
        val lng = formatCoord(location.longitude)
        return buildString {
            appendLine("LINDA BINTI LOCATION UPDATE")
            appendLine(mapsUrl(location))
            appendLine("Lat: $lat")
            appendLine("Lng: $lng")
            append("Time: ")
            append(formatTime(capturedAtMillis))
        }.trim()
    }

    private fun categoryLines(category: SosEmergencyCategory): String = when (category) {
        SosEmergencyCategory.GBV -> "Possible GBV emergency reported."
        SosEmergencyCategory.FGM -> "Possible FGM-related emergency reported."
        SosEmergencyCategory.FGM_THREAT -> "Possible FGM threat/emergency reported."
        SosEmergencyCategory.OTHER_GBV_FGM -> "Possible GBV/FGM-related emergency reported."
        SosEmergencyCategory.GENERAL ->
            "This person has activated an emergency alert and needs immediate assistance."
    }

    fun mapsUrl(location: SosLocation): String {
        val lat = formatCoord(location.latitude)
        val lng = formatCoord(location.longitude)
        return "https://maps.google.com/?q=$lat,$lng"
    }

    fun formatCoord(value: Double): String =
        String.format(Locale.US, "%.5f", value)

    fun formatTime(epochMs: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH).format(Date(epochMs))

    fun containsSecrets(message: String): Boolean {
        val lower = message.lowercase(Locale.ENGLISH)
        return lower.contains("access pin") ||
            lower.contains("access code") ||
            lower.contains("sos code") ||
            lower.contains("sos pin")
    }
}
