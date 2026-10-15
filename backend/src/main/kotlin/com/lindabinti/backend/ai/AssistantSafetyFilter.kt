package com.lindabinti.backend.ai

/**
 * Input/output safety for assistant replies: no invented hotlines, no PIN/SOS leakage cues.
 */
object AssistantSafetyFilter {

    private val allowedHotlines = listOf("999", "112", "1195", "116")
    private val suspiciousPhone = Regex("""\b(?:0|\+?254)?7\d{8}\b""")
    private val pinCue = Regex("""(?i)\b(?:pin|sos code|access code)\s*(?:is|=|:)?\s*\d{3,6}\b""")

    fun sanitizeUserMessage(message: String): String =
        message
            .replace(pinCue, "[code removed]")
            .take(500)

    fun sanitizeReply(reply: String): String {
        var out = reply.trim()
        if (out.isBlank()) return out
        // Remove accidental PIN/SOS guesses, including the former default example code.
        out = out.replace(pinCue, "your secret code (set during setup)")
        out = out.replace(Regex("""\b1102\b"""), "[code removed]")
        // Soften unexpected mobile numbers that are not verified hotlines.
        out = suspiciousPhone.replace(out) { match ->
            val digits = match.value.filter { it.isDigit() }.takeLast(3)
            if (allowedHotlines.any { match.value.contains(it) }) match.value
            else "[unverified number removed — use 999, 112, 1195, or 116]"
        }
        return out
    }

    fun mergeRisk(a: String, b: String): String {
        val order = listOf("NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL")
        val ia = order.indexOf(a.uppercase()).coerceAtLeast(0)
        val ib = order.indexOf(b.uppercase()).coerceAtLeast(0)
        return order[maxOf(ia, ib)]
    }

    fun preferAction(primary: String?, secondary: String?): String? =
        when {
            primary == "emergency" || secondary == "emergency" -> "emergency"
            primary != null -> primary
            else -> secondary
        }

    fun ensureCriticalGuidance(reply: String, language: String, urgent: Boolean): String {
        if (!urgent) return reply
        val banner = AssistantRiskDetector.urgentSafetyReply(language)
        return if (reply.contains("999") || reply.contains("1195") || reply.contains("Emergency")) {
            reply
        } else {
            "$banner\n\n$reply"
        }
    }
}
