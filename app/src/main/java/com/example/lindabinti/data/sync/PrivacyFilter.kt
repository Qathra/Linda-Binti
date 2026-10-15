package com.example.lindabinti.data.sync

/**
 * Strips identifiers before anonymized analytics leave the device.
 * Never includes Access PIN, SOS code, contacts, or free-text descriptions.
 */
object PrivacyFilter {

    private val phoneLike = Regex("""\+?\d[\d\s\-()]{7,}\d""")

    fun sanitizeCategoryOrBand(raw: String): String? {
        val v = raw.trim().lowercase().take(64)
        if (v.isBlank()) return null
        if (phoneLike.containsMatchIn(v)) return null
        if (v.length > 40) return null
        return v.replace(Regex("""[^a-z0-9_\- ]"""), "").ifBlank { null }
    }

    fun weekLabel(epochMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        val week = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        val year = cal.get(java.util.Calendar.YEAR)
        return "%d-W%02d".format(year, week)
    }

    /** Analytics payloads must never carry report bodies or names. */
    fun assertNoPii(payloadFields: Map<String, String>) {
        payloadFields.forEach { (k, v) ->
            require(k !in setOf("body", "name", "phone", "pin", "sos")) {
                "Forbidden analytics field: $k"
            }
            require(!phoneLike.containsMatchIn(v)) { "Phone-like value rejected" }
        }
    }
}