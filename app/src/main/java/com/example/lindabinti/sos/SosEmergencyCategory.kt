package com.example.lindabinti.sos

/**
 * Category for the SOS SMS. Secret calculator SOS uses [GENERAL]
 * so the disguise flow stays one-step. Never put PINs or report text here.
 */
enum class SosEmergencyCategory {
    GBV,
    FGM,
    FGM_THREAT,
    OTHER_GBV_FGM,
    GENERAL;

    companion object {
        fun fromWire(raw: String?): SosEmergencyCategory =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class SosLocationPeek {
    AVAILABLE,
    ATTEMPTING,
    UNAVAILABLE
}

object SosLocationRules {
    const val FRESH_MAX_AGE_MS = 90_000L
    const val CACHE_MAX_AGE_MS = 2 * 60 * 60_000L
    /** Older last-known still useful for SOS SMS (labelled cached). */
    const val EMERGENCY_STALE_MAX_AGE_MS = 24 * 60 * 60_000L
    /**
     * GNSS wait when Location is ON but no last-known yet.
     * Cold GPS on a real phone often needs more than a few seconds.
     */
    /** First SMS uses last-known GPS, or waits up to 2s so the maps link is in the alert. */
    const val SOS_FIRST_SMS_WAIT_MS = 2_000L
    const val SOS_TIMEOUT_MS = 35_000L
    /** Extra GNSS wait after the first SMS if that SMS had no fix. */
    const val SOS_FOLLOWUP_TIMEOUT_MS = 60_000L

    /** GNSS/GPS does not need mobile data or Wi-Fi. */
    fun locationRequiresInternet(): Boolean = false

    fun classifyAge(fixEpochMs: Long, nowEpochMs: Long): String? {
        val age = nowEpochMs - fixEpochMs
        if (age < 0L) return null
        if (age <= FRESH_MAX_AGE_MS) return "ok"
        if (age <= CACHE_MAX_AGE_MS) return "cached"
        return null
    }

    /** Same as [classifyAge], but keeps day-old (or unknown-time) fixes as cached for SOS. */
    fun classifyAgeForSos(fixEpochMs: Long, nowEpochMs: Long): String? {
        if (fixEpochMs <= 0L) return "cached"
        classifyAge(fixEpochMs, nowEpochMs)?.let { return it }
        val age = nowEpochMs - fixEpochMs
        if (age < 0L) return "cached"
        if (age <= EMERGENCY_STALE_MAX_AGE_MS) return "cached"
        // For SOS, any valid coordinates beat "unavailable".
        return "cached"
    }
}

/**
 * SOS SMS is local telephony. Backend/AI/internet must never gate the send.
 */
object SosDispatchPolicy {
    fun smsRequiresInternet(): Boolean = false
    fun smsRequiresBackend(): Boolean = false
    fun smsRequiresAi(): Boolean = false
    fun smsRequiresLocationFix(): Boolean = false
}
