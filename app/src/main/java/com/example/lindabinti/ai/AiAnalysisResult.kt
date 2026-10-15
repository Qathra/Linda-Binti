package com.example.lindabinti.ai

data class AiAnalysisResult(
    val analysisId: String,
    val source: AiAnalysisSource,
    val category: AiAnalysisCategory,
    val severity: AiSeverity,
    val confidence: Double,
    val indicators: List<String>,
    val explanation: String,
    val recommendations: List<String>,
    val educationRecommendations: List<String>,
    val timestamp: Long,
    val providerId: String? = null,
    val modelId: String? = null,
    val processingDurationMs: Long? = null,
    val consentGranted: Boolean = false
) {
    val sourceLabel: String get() = source.displayLabel
}
