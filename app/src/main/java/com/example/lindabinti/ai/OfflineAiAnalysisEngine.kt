package com.example.lindabinti.ai

import com.example.lindabinti.data.content.GbvReportCategories
import java.util.UUID

/**
 * Structured, explainable on-device GBV/FGM safety classifier.
 * This is a rule-based safety engine — not a neural network or large language model.
 */
class OfflineAiAnalysisEngine : AiAnalysisEngine {

    override suspend fun analyze(input: AiAnalysisInput): AiAnalysisResult {
        val start = System.currentTimeMillis()
        val categoryId = GbvReportCategories.normalize(input.categoryId)
        val text = if (input.includeDescription) input.description.trim() else ""
        val lower = text.lowercase()

        val matchedIndicators = detectIndicators(lower)
        val category = resolveCategory(categoryId, lower, matchedIndicators)
        val baseSeverity = baseSeverityFor(categoryId)
        val severity = bumpSeverity(baseSeverity, matchedIndicators)
        val confidence = computeConfidence(matchedIndicators, categoryId, text.isNotBlank())
        val explanation = buildExplanation(category, categoryId, matchedIndicators, severity)
        val recommendations = buildRecommendations(category, severity, matchedIndicators)
        val education = buildEducationRecommendations(category)

        return AiAnalysisResult(
            analysisId = UUID.randomUUID().toString(),
            source = AiAnalysisSource.OFFLINE,
            category = category,
            severity = severity,
            confidence = confidence,
            indicators = matchedIndicators,
            explanation = explanation,
            recommendations = recommendations,
            educationRecommendations = education,
            timestamp = System.currentTimeMillis(),
            providerId = PROVIDER_ID,
            modelId = MODEL_ID,
            processingDurationMs = System.currentTimeMillis() - start,
            consentGranted = false
        )
    }

    private fun detectIndicators(lower: String): List<String> {
        if (lower.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        if (matchesAny(lower, IMMEDIATE_DANGER)) out += "Immediate danger language"
        if (matchesAny(lower, PLANNED_VIOLENCE)) out += "Planned violence mentioned"
        if (matchesAny(lower, WEAPON)) out += "Weapon reference"
        if (matchesAny(lower, SEVERE_INJURY)) out += "Severe injury or bleeding mentioned"
        if (matchesAny(lower, CHILD_RISK)) out += "Child or minor potentially at risk"
        if (matchesAny(lower, IMMINENT_FGM)) out += "Imminent or planned FGM"
        if (matchesAny(lower, CONFINEMENT)) out += "Confinement or inability to leave"
        if (matchesAny(lower, REPEATED_THREATS)) out += "Repeated or ongoing threats"
        return out
    }

    private fun resolveCategory(
        categoryId: String,
        lower: String,
        indicators: List<String>
    ): AiAnalysisCategory {
        val id = categoryId.lowercase()
        val fgmThreat = id == "fgm_pressure" ||
            (id.startsWith("fgm") && indicators.any { it.contains("FGM", ignoreCase = true) }) ||
            matchesAny(lower, IMMINENT_FGM)
        if (fgmThreat) return AiAnalysisCategory.FGM_THREAT
        return when {
            id.startsWith("fgm") -> AiAnalysisCategory.FGM
            id == "forced_marriage" -> AiAnalysisCategory.RELATED
            id in GBV_IDS -> AiAnalysisCategory.GBV
            GbvReportCategories.isGbvFgmScope(id) -> when {
                id.startsWith("fgm") -> AiAnalysisCategory.FGM
                id == "forced_marriage" -> AiAnalysisCategory.RELATED
                else -> AiAnalysisCategory.GBV
            }
            else -> AiAnalysisCategory.UNKNOWN
        }
    }

    private fun baseSeverityFor(categoryId: String): AiSeverity = when (categoryId.lowercase()) {
        "sexual", "fgm", "fgm_incident", "fgm_pressure" -> AiSeverity.HIGH
        "physical", "domestic", "forced_marriage", "threats" -> AiSeverity.HIGH
        "harassment", "emotional", "other" -> AiSeverity.ELEVATED
        else -> AiSeverity.ELEVATED
    }

    private fun bumpSeverity(base: AiSeverity, indicators: List<String>): AiSeverity {
        if (indicators.isEmpty()) return base
        val criticalSignals = indicators.any {
            it.contains("Immediate") || it.contains("Weapon") ||
                it.contains("Imminent") || it.contains("Severe injury")
        }
        val highSignals = indicators.size >= 2
        return when {
            criticalSignals -> AiSeverity.CRITICAL
            base == AiSeverity.HIGH && highSignals -> AiSeverity.CRITICAL
            base == AiSeverity.ELEVATED && (criticalSignals || highSignals) -> AiSeverity.HIGH
            highSignals && base.ordinal < AiSeverity.HIGH.ordinal -> AiSeverity.HIGH
            else -> base
        }
    }

    private fun computeConfidence(indicators: List<String>, categoryId: String, hasText: Boolean): Double {
        var score = if (GbvReportCategories.isGbvFgmScope(categoryId)) 0.55 else 0.4
        if (hasText) score += 0.1
        score += indicators.size * 0.08
        return score.coerceIn(0.35, 0.95)
    }

    private fun buildExplanation(
        category: AiAnalysisCategory,
        categoryId: String,
        indicators: List<String>,
        severity: AiSeverity
    ): String = buildString {
        append("Offline AI Safety Analysis: ")
        append(categoryLabel(category, categoryId))
        append(". Priority ")
        append(severity.name.lowercase())
        append(". ")
        if (indicators.isNotEmpty()) {
            append("Indicators: ")
            append(indicators.joinToString("; "))
            append(". ")
        } else {
            append("Classification is based on the selected incident category. ")
        }
        append(
            "This is a safety-oriented priority aid, not a medical, legal, police, or counselling diagnosis."
        )
    }

    private fun buildRecommendations(
        category: AiAnalysisCategory,
        severity: AiSeverity,
        indicators: List<String>
    ): List<String> {
        val out = mutableListOf<String>()
        if (severity == AiSeverity.CRITICAL || indicators.any { it.contains("Immediate") }) {
            out += "If you are in immediate danger, use Emergency Assistance or call 999/112."
        }
        when (category) {
            AiAnalysisCategory.FGM, AiAnalysisCategory.FGM_THREAT -> {
                out += "Consider seeking immediate protection for the person at risk."
                out += "Contact a trusted GBV/FGM support service in Kajiado."
            }
            AiAnalysisCategory.GBV -> {
                out += "Reach a safe place if possible and contact a trusted support service."
            }
            AiAnalysisCategory.RELATED -> {
                out += "Forced marriage linked to GBV/FGM may need urgent referral to protection services."
            }
            AiAnalysisCategory.UNKNOWN -> {
                out += "Review the Education section for GBV/FGM safety information."
            }
        }
        if (out.isEmpty()) {
            out += "Use Education and Support sections for guidance. You are not alone."
        }
        return out.distinct()
    }

    private fun buildEducationRecommendations(category: AiAnalysisCategory): List<String> =
        when (category) {
            AiAnalysisCategory.FGM, AiAnalysisCategory.FGM_THREAT -> listOf("fgm", "child_protection", "know_rights")
            AiAnalysisCategory.GBV -> listOf("gbv_basics", "safety_planning", "know_rights")
            AiAnalysisCategory.RELATED -> listOf("forced_marriage", "know_rights")
            AiAnalysisCategory.UNKNOWN -> listOf("gbv_basics")
        }

    private fun categoryLabel(category: AiAnalysisCategory, categoryId: String): String = when (category) {
        AiAnalysisCategory.GBV -> categoryDisplay(categoryId)
        AiAnalysisCategory.FGM -> "FGM-related incident"
        AiAnalysisCategory.FGM_THREAT -> "FGM threat"
        AiAnalysisCategory.RELATED -> "GBV/FGM-related protection concern"
        AiAnalysisCategory.UNKNOWN -> "Unclassified GBV/FGM-related concern"
    }

    private fun categoryDisplay(categoryId: String): String = when (categoryId.lowercase()) {
        "physical" -> "Physical violence"
        "sexual" -> "Sexual violence"
        "emotional" -> "Emotional or psychological abuse"
        "domestic" -> "Domestic violence"
        "threats" -> "Threats or intimidation"
        "harassment" -> "Harassment"
        else -> "GBV-related incident"
    }

    private fun matchesAny(text: String, tokens: List<String>): Boolean =
        tokens.any { text.contains(it) }

    companion object {
        const val PROVIDER_ID = "linda-offline-safety-engine"
        const val MODEL_ID = "rule-classifier-v1"

        private val GBV_IDS = setOf(
            "physical", "sexual", "emotional", "domestic", "harassment", "threats"
        )

        private val IMMEDIATE_DANGER = listOf(
            "right now", "tonight", "today", "immediately", "help me now",
            "sasa hivi", "usiku huu", "leo", "nisaidie sasa"
        )
        private val PLANNED_VIOLENCE = listOf(
            "will hit", "going to hurt", "will beat", "plan to attack", "atapiga", "atanipiga"
        )
        private val WEAPON = listOf("knife", "gun", "weapon", "machete", "silaha", "kisu", "bunduki")
        private val SEVERE_INJURY = listOf(
            "bleeding", "blood", "broken", "hospital", "unconscious", "damu", "hospitali"
        )
        private val CHILD_RISK = listOf(
            "child", "daughter", "sister", "minor", "girl", "mtoto", "binti", "msichana"
        )
        private val IMMINENT_FGM = listOf(
            "cut her", "cutting her", "cut my sister", "cut my daughter", "will cut",
            "ceremony tomorrow", "fgm tomorrow", "will be cut",
            "keketa", "sherehe kesho", "atakata", "kumkeketa"
        )
        private val CONFINEMENT = listOf(
            "locked", "trapped", "cannot leave", "held inside", "imefungwa", "siwezi kutoka"
        )
        private val REPEATED_THREATS = listOf(
            "again", "keeps threatening", "every day", "tena", "kila siku", "anatishia"
        )
    }
}
