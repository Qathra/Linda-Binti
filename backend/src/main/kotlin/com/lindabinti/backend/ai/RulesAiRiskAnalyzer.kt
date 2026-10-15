package com.lindabinti.backend.ai

import com.lindabinti.backend.services.ReportRiskClassifier

/**
 * Always-available rule fallback. Used when the online AI is off, times out, or returns junk.
 */
class RulesAiRiskAnalyzer : AiRiskAnalyzer {
    override fun analyze(input: AiRiskInput): AiRiskResult {
        val classified = ReportRiskClassifier.classify(
            input.categoryId,
            input.redactedDescription.takeIf { it.isNotBlank() }
        )
        val level = AiRiskLevel.parse(classified.urgency) ?: AiRiskLevel.MEDIUM
        val relevance = relevanceFor(input.categoryId)
        return AiRiskResult(
            riskLevel = level,
            gbvFgmRelevance = relevance,
            incidentCategory = input.categoryLabel.ifBlank { input.categoryId },
            urgencyIndicators = if (classified.urgency.equals("CRITICAL", true) ||
                classified.urgency.equals("critical", true)
            ) {
                listOf("Imminent-language or high-risk GBV/FGM category")
            } else emptyList(),
            safetyConcerns = safetyFor(input.categoryId, level),
            recommendedPriority = level,
            explanation = classified.explanation,
            source = "rules",
            status = AiStatus.COMPLETE
        )
    }

    private fun relevanceFor(categoryId: String): String {
        val id = categoryId.lowercase()
        return when {
            id.startsWith("fgm") -> AiRelevance.FGM
            id == "forced_marriage" -> AiRelevance.BOTH
            else -> AiRelevance.GBV
        }
    }

    private fun safetyFor(categoryId: String, level: String): List<String> {
        val id = categoryId.lowercase()
        val out = mutableListOf<String>()
        if (id.startsWith("fgm") || id == "forced_marriage") {
            out += "Child-protection or coercion risk may be present"
        }
        if (level == AiRiskLevel.CRITICAL || level == AiRiskLevel.HIGH) {
            out += "Possible need for immediate safety support"
        }
        return out
    }
}
