package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiResultMerger
import com.lindabinti.backend.ai.AiStatus

/**
 * Human-readable AI source and status labels for the NGO dashboard.
 * Never implies LLM usage when only rule-based classification ran.
 */
object NgoAiDisplayLabels {

    const val SOURCE_RULE_BASED = "RULE-BASED AI"
    const val SOURCE_ONLINE = "ONLINE AI"
    const val SOURCE_HYBRID = "HYBRID AI"

    const val STATUS_WORKING = "WORKING"
    const val STATUS_FALLBACK = "FALLBACK"
    const val STATUS_FAILED = "FAILED"
    const val STATUS_NOT_CONFIGURED = "NOT CONFIGURED"

    fun sourceLabel(classifierSource: String): String = when (classifierSource.trim().lowercase()) {
        AiResultMerger.SOURCE_ONLINE, "online" -> SOURCE_ONLINE
        AiResultMerger.SOURCE_HYBRID, "hybrid" -> SOURCE_HYBRID
        else -> SOURCE_RULE_BASED
    }

    fun caseStatusLabel(
        aiStatus: String,
        classifierSource: String,
        onlineConfigured: Boolean
    ): String {
        val status = aiStatus.trim().lowercase()
        val source = classifierSource.trim().lowercase()
        return when {
            status == AiStatus.FAILED ||
                status == AiStatus.INVALID ||
                status == AiStatus.TIMEOUT -> STATUS_FAILED
            status == AiStatus.PENDING -> STATUS_WORKING
            status == AiStatus.COMPLETE && source == AiResultMerger.SOURCE_HYBRID -> STATUS_WORKING
            status == AiStatus.COMPLETE && source == AiResultMerger.SOURCE_ONLINE -> STATUS_WORKING
            status == AiStatus.COMPLETE && source == AiResultMerger.SOURCE_OFFLINE &&
                onlineConfigured -> STATUS_FALLBACK
            status == AiStatus.COMPLETE -> STATUS_WORKING
            status == AiStatus.UNAVAILABLE || status == AiStatus.MISSING ->
                if (onlineConfigured) STATUS_FALLBACK else STATUS_NOT_CONFIGURED
            else -> if (onlineConfigured) STATUS_FALLBACK else STATUS_WORKING
        }
    }

    fun engineStatusLabel(
        onlineConfigured: Boolean,
        pendingAi: Int,
        rulesFallback: Int,
        failed: Int
    ): String = when {
        !onlineConfigured -> STATUS_NOT_CONFIGURED
        failed > 0 && rulesFallback == 0 && pendingAi == 0 -> STATUS_FAILED
        rulesFallback > 0 || failed > 0 || pendingAi > 0 -> STATUS_FALLBACK
        else -> STATUS_WORKING
    }

    fun incidentType(categoryId: String, incidentCategory: String): String {
        val id = categoryId.trim().lowercase()
        val label = incidentCategory.trim().lowercase()
        return when {
            id.contains("pressure") || label.contains("pressure") -> "pressure"
            id.contains("threat") || id == "threats" || label.contains("threat") -> "threat"
            else -> "incident"
        }
    }

    fun buildWhyFlaggedExplanation(
        severity: String,
        scope: String,
        incidentType: String,
        indicators: List<String>,
        explanation: String
    ): String {
        val parts = mutableListOf<String>()
        parts += "Flagged as $scope · ${incidentType.replaceFirstChar { it.uppercase() }} · severity $severity"
        if (indicators.isNotEmpty()) {
            parts += "Detected indicators: ${indicators.joinToString("; ")}"
        } else if (explanation.isNotBlank()) {
            parts += explanation.take(240)
        }
        return parts.joinToString(". ")
    }
}
