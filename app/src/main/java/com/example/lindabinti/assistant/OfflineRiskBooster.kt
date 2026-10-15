package com.example.lindabinti.assistant

/**
 * On-device urgent-danger boost for offline FAQ answers.
 */
object OfflineRiskBooster {

    fun boost(userText: String, base: AssistantMessage, language: String): AssistantMessage {
        val n = userText.trim().lowercase()
        val urgent = listOf(
            "i am in danger", "help me now", "someone is attacking", "scared to go home",
            "about to undergo", "forcing my daughter", "will cut", "nisaidie sasa", "hatari"
        ).any { n.contains(it) }
        if (!urgent) return base
        val banner = if (language.startsWith("sw", ignoreCase = true)) {
            "Usalama wako ni wa kwanza. Fungua Msaada wa Dharura, piga 999/112 au 1195. "
        } else {
            "Your safety comes first. Open Emergency Assistance, call 999/112 or 1195. "
        }
        val text = if (base.text.contains("999") || base.text.contains("Emergency")) {
            base.text
        } else {
            banner + base.text
        }
        return base.copy(
            text = text,
            relatedAction = "emergency",
            riskLevel = "CRITICAL"
        )
    }
}
