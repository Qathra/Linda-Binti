package com.lindabinti.backend.ai

/**
 * Lightweight urgent-danger detection for assistant chat (rules, not an LLM).
 */
object AssistantRiskDetector {

    data class Assessment(
        val riskLevel: String,
        val relatedAction: String?,
        val urgent: Boolean,
        val reason: String
    )

    fun assess(message: String): Assessment {
        val n = normalize(message)
        if (n.isBlank()) return Assessment("NONE", null, false, "empty")

        val criticalHits = listOf(
            "i am in danger", "help me now", "someone is attacking", "being attacked",
            "will cut me", "about to undergo", "forcing my daughter", "forcing my sister",
            "scared to go home", "afraid to go home", "kill me", "weapon", "bleeding",
            "rape", "raped", "hatari", "nisaidie sasa", "karibia kufanyiwa", "ogopa kurudi"
        )
        if (criticalHits.any { n.contains(it) }) {
            return Assessment("CRITICAL", "emergency", true, "urgent_danger_phrase")
        }

        val highHits = listOf(
            "threaten", "threatening", "beat me", "beats me", "husband keeps",
            "partner hits", "fgm", "cut my", "violence", "tisho", "kupigwa", "mume"
        )
        if (highHits.any { n.contains(it) }) {
            val action = if (n.contains("fgm") || n.contains("cut")) "emergency" else "reporting"
            return Assessment("HIGH", action, false, "elevated_threat_phrase")
        }

        val mediumHits = listOf("unsafe", "afraid", "scared", "rights", "report gbv", "where can i")
        if (mediumHits.any { n.contains(it) }) {
            return Assessment("MEDIUM", null, false, "concern_phrase")
        }
        return Assessment("NONE", null, false, "none")
    }

    fun urgentSafetyReply(language: String): String {
        val lang = language.lowercase()
        return when {
            lang.startsWith("sw") ->
                "Usalama wako ni wa kwanza. Ikiwa uko hatarini sasa: fungua Msaada wa Dharura katika Linda Binti, " +
                    "piga 999 au 112, au 1195 (GBV). Unaweza pia kutuma SOS kutoka calculator. " +
                    "Hii ni mwongozo wa kiotomatiki — si daktari, mwanasheria, wala polisi."
            else ->
                "Your safety comes first. If you are in danger now: open Emergency Assistance in Linda Binti, " +
                    "call 999 or 112, or 1195 (GBV helpline). You can also send SOS from the calculator. " +
                    "This is automated guidance only — not a doctor, lawyer, or police officer."
        }
    }

    private fun normalize(text: String): String =
        text.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
