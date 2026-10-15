package com.lindabinti.backend.ai

/**
 * Validates and merges offline (rules) and online AI structured results.
 * Never escalates to automatic SOS or authority contact.
 */
object AiResultMerger {

    const val SOURCE_OFFLINE = "rules"
    const val SOURCE_ONLINE = "ai"
    const val SOURCE_HYBRID = "hybrid"
    const val LABEL_OFFLINE = "RULE-BASED AI"
    const val LABEL_ONLINE = "ONLINE AI"
    const val LABEL_HYBRID = "HYBRID AI"

    fun mergeNgo(offline: AiRiskResult, online: AiRiskResult?): AiRiskResult {
        if (online == null || !isUsableOnline(online)) {
            return offline.copy(
                source = SOURCE_OFFLINE,
                status = when (online?.status) {
                    AiStatus.TIMEOUT, AiStatus.FAILED, AiStatus.INVALID, AiStatus.MISSING -> online.status
                    else -> AiStatus.COMPLETE
                }
            )
        }
        val mergedLevel = maxSeverity(offline.riskLevel, online.riskLevel)
        val mergedPriority = maxSeverity(offline.recommendedPriority, online.recommendedPriority)
        val indicators = (offline.urgencyIndicators + online.urgencyIndicators).distinct().take(12)
        val concerns = (offline.safetyConcerns + online.safetyConcerns).distinct().take(12)
        val explanation = buildString {
            append(online.explanation.trim())
            if (mergedLevel != online.riskLevel || mergedLevel != offline.riskLevel) {
                append(" Validated against $LABEL_OFFLINE baseline.")
            }
        }.trim()
        return online.copy(
            riskLevel = mergedLevel,
            recommendedPriority = mergedPriority,
            urgencyIndicators = indicators,
            safetyConcerns = concerns,
            explanation = explanation,
            source = SOURCE_HYBRID,
            status = AiStatus.COMPLETE
        )
    }

    private fun isUsableOnline(result: AiRiskResult): Boolean =
        result.status == AiStatus.COMPLETE && AiRiskLevel.isValid(result.riskLevel)

    private fun maxSeverity(a: String, b: String): String {
        val rankA = AiRiskLevel.sortRank(a)
        val rankB = AiRiskLevel.sortRank(b)
        return if (rankA <= rankB) {
            AiRiskLevel.parse(a) ?: AiRiskLevel.MEDIUM
        } else {
            AiRiskLevel.parse(b) ?: AiRiskLevel.MEDIUM
        }
    }

    fun mergeSurvivorOnline(
        offline: AiRiskResult,
        online: OnlineReportAnalysisResult
    ): OnlineReportAnalysisResult {
        val offlineMapped = mapOfflineSeverity(offline.riskLevel)
        val onlineMapped = online.severity.uppercase()
        val mergedSeverity = if (severityRank(offlineMapped) < severityRank(onlineMapped)) {
            offlineMapped
        } else {
            online.severity
        }
        val indicators = (offline.urgencyIndicators + online.indicators).distinct().take(12)
        var explanation = online.explanation
        if (mergedSeverity != online.severity) {
            explanation += " Validated against $LABEL_OFFLINE baseline."
        }
        return online.copy(
            source = LABEL_ONLINE,
            severity = mergedSeverity,
            indicators = indicators,
            explanation = explanation.trim()
        )
    }

    private fun mapOfflineSeverity(level: String): String = when (AiRiskLevel.parse(level)) {
        AiRiskLevel.CRITICAL -> "CRITICAL"
        AiRiskLevel.HIGH -> "HIGH"
        AiRiskLevel.LOW -> "LOWER"
        else -> "ELEVATED"
    }

    private fun severityRank(severity: String): Int = when (severity.uppercase()) {
        "CRITICAL" -> 0
        "HIGH" -> 1
        "ELEVATED", "MEDIUM" -> 2
        else -> 3
    }
}
