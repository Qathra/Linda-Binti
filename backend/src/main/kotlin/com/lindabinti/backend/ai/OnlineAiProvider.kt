package com.lindabinti.backend.ai

/**
 * Server-side online report analysis result aligned with the Android [AiAnalysisResult] contract.
 */
data class OnlineReportAnalysisResult(
    val analysisId: String,
    val source: String = "ONLINE",
    val category: String,
    val severity: String,
    val confidence: Double,
    val indicators: List<String>,
    val explanation: String,
    val recommendations: List<String>,
    val educationRecommendations: List<String>,
    val timestamp: Long,
    val providerId: String,
    val modelId: String,
    val processingDurationMs: Long,
    val aiStatus: String = AiStatus.COMPLETE
)

/**
 * Pluggable cloud AI provider for survivor report analysis.
 * API keys stay on the backend only — never in the Android app.
 */
interface OnlineAiProvider {
    val providerId: String
    fun isConfigured(): Boolean
    fun analyzeReport(input: AiRiskInput): OnlineReportAnalysisResult
}

class AiProviderException(
    val code: String,
    override val message: String,
    val aiStatus: String = AiStatus.UNAVAILABLE
) : RuntimeException(message)
