package com.example.lindabinti.ai

/**
 * Merges offline and online structured analysis without automatic emergency actions.
 */
object AiAnalysisMerger {

    fun merge(offline: AiAnalysisResult, online: AiAnalysisResult): AiAnalysisResult {
        val severity = maxSeverity(offline.severity, online.severity)
        val indicators = (offline.indicators + online.indicators).distinct().take(12)
        val recommendations = (offline.recommendations + online.recommendations).distinct().take(8)
        val education = (offline.educationRecommendations + online.educationRecommendations).distinct().take(6)
        val confidence = online.confidence.coerceIn(0.0, 1.0)
        val explanation = buildString {
            append(online.explanation.trim())
            if (severity != online.severity) {
                append(" Validated against ${AiAnalysisSource.OFFLINE.displayLabel} baseline.")
            }
        }.trim()
        return online.copy(
            source = AiAnalysisSource.HYBRID,
            category = if (online.category != AiAnalysisCategory.UNKNOWN) online.category else offline.category,
            severity = severity,
            confidence = confidence,
            indicators = indicators,
            explanation = explanation,
            recommendations = recommendations,
            educationRecommendations = education,
            timestamp = System.currentTimeMillis(),
            consentGranted = true
        )
    }

    private fun maxSeverity(a: AiSeverity, b: AiSeverity): AiSeverity =
        if (a.ordinal >= b.ordinal) a else b
}
