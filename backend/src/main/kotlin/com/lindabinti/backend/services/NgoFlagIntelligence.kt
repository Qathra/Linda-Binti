package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiRiskLevel
import com.lindabinti.backend.ai.AiStatus

object NgoFlagIntelligence {

    fun estimateConfidence(flag: ReportFlag): Double {
        if (!flag.aiStatus.equals(AiStatus.COMPLETE, ignoreCase = true) &&
            flag.classifierSource != "ai"
        ) {
            return when (AiRiskLevel.parse(flag.urgency)) {
                AiRiskLevel.CRITICAL -> 0.72
                AiRiskLevel.HIGH -> 0.68
                AiRiskLevel.MEDIUM -> 0.55
                AiRiskLevel.LOW -> 0.45
                else -> 0.5
            }
        }
        var score = when (AiRiskLevel.parse(flag.urgency)) {
            AiRiskLevel.CRITICAL -> 0.94
            AiRiskLevel.HIGH -> 0.88
            AiRiskLevel.MEDIUM -> 0.76
            AiRiskLevel.LOW -> 0.62
            else -> 0.7
        }
        if (flag.classifierSource == "rules") score = (score * 0.88).coerceAtMost(0.82)
        if (flag.urgencyIndicators.isNotBlank()) score += 0.03
        if (flag.safetyConcerns.isNotBlank()) score += 0.02
        return score.coerceIn(0.35, 0.98)
    }

    fun parseIndicators(flag: ReportFlag): List<String> {
        val fromUrgency = flag.urgencyIndicators.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val fromSafety = flag.safetyConcerns.split(';', ',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return (fromUrgency + fromSafety).distinct().take(8)
    }

    fun severityLabel(urgency: String): String = AiRiskLevel.parse(urgency) ?: urgency.uppercase()

    fun categoryDisplay(flag: ReportFlag): String =
        flag.incidentCategory.ifBlank { flag.categoryLabel }.ifBlank { flag.categoryId }
}
