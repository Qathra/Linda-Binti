package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiRiskLevel

/**
 * Rule-based GBV/FGM urgency for authorized NGO responders.
 * Does not diagnose. Does not quote private report text in the explanation.
 */
object ReportRiskClassifier {

    data class Classification(
        val urgency: String,
        val explanation: String,
        val source: String = "rules"
    )

    private val imminentTokens = listOf(
        "tonight", "right now", "bleeding", "weapon", "kill", "killing",
        "ceremony tomorrow", "cut her", "cutting her", "being attacked",
        "usiku huu", "sasa hivi", "damu", "silaha", "kuua", "keketa",
        "sherehe kesho", "ninashambuliwa"
    )

    fun classify(categoryId: String, body: String?): Classification {
        val cat = categoryId.trim().lowercase()
        var level = baseLevel(cat)
        val imminent = bodyHasImminentSignal(body)
        if (imminent) level = bump(level)

        val explanation = buildString {
            append(categoryExplanation(cat))
            if (imminent) {
                append(" Imminent-language signals raised priority. ")
            }
            append("This is a priority aid for authorized responders, not a medical, legal, police, or counselling diagnosis.")
        }
        return Classification(urgency = level, explanation = explanation)
    }

    private fun baseLevel(cat: String): String = when (cat) {
        "sos" -> AiRiskLevel.CRITICAL
        "sexual", "fgm", "fgm_incident", "fgm_pressure" -> AiRiskLevel.HIGH
        "physical", "domestic", "forced_marriage", "threats" -> AiRiskLevel.HIGH
        "harassment", "emotional", "other" -> AiRiskLevel.MEDIUM
        else -> AiRiskLevel.MEDIUM
    }

    private fun bump(level: String): String = when (level) {
        AiRiskLevel.LOW -> AiRiskLevel.MEDIUM
        AiRiskLevel.MEDIUM -> AiRiskLevel.HIGH
        AiRiskLevel.HIGH -> AiRiskLevel.CRITICAL
        AiRiskLevel.CRITICAL -> AiRiskLevel.CRITICAL
        else -> AiRiskLevel.HIGH
    }

    private fun bodyHasImminentSignal(body: String?): Boolean {
        val text = body?.lowercase() ?: return false
        if (text.isBlank()) return false
        return imminentTokens.any { text.contains(it) }
    }

    private fun categoryExplanation(cat: String): String = when (cat) {
        "sos" -> "Device SOS alert — treated as critical for immediate responder follow-up. "
        "sexual" -> "Sexual GBV reports are treated as high priority for survivor safety and referral. "
        "fgm", "fgm_incident", "fgm_pressure" -> "FGM-related reports are treated as high priority because of prevention and child-protection risk. "
        "forced_marriage" -> "Forced marriage linked to GBV/FGM prevention is treated as high priority. "
        "physical", "domestic" -> "Physical or domestic GBV can escalate quickly and is treated as high priority. "
        "threats" -> "Threats or intimidation may precede further harm and are treated as high priority. "
        "harassment" -> "Harassment is classified medium unless imminent-harm language is present. "
        "emotional" -> "Emotional or psychological abuse is classified medium unless imminent-harm language is present. "
        "other" -> "Other GBV/FGM-related incident — classified medium pending responder review. "
        else -> "GBV/FGM-related report classified for responder review. "
    }
}
