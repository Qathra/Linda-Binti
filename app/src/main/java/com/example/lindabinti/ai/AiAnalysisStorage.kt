package com.example.lindabinti.ai

import com.google.gson.Gson

object AiAnalysisStorage {
    private val gson = Gson()

    fun toJson(result: AiAnalysisResult): String = gson.toJson(StoredAiAnalysis.from(result))

    fun fromJson(raw: String?): AiAnalysisResult? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val stored = gson.fromJson(raw, StoredAiAnalysis::class.java)
            stored?.toResult()
        }.getOrNull()
    }

    private data class StoredAiAnalysis(
        val analysisId: String,
        val source: String,
        val category: String,
        val severity: String,
        val confidence: Double,
        val indicators: List<String>,
        val explanation: String,
        val recommendations: List<String>,
        val educationRecommendations: List<String>,
        val timestamp: Long,
        val providerId: String?,
        val modelId: String?,
        val processingDurationMs: Long?,
        val consentGranted: Boolean
    ) {
        fun toResult(): AiAnalysisResult = AiAnalysisResult(
            analysisId = analysisId,
            source = AiAnalysisSource.valueOf(source),
            category = AiAnalysisCategory.valueOf(category),
            severity = AiSeverity.valueOf(severity),
            confidence = confidence,
            indicators = indicators,
            explanation = explanation,
            recommendations = recommendations,
            educationRecommendations = educationRecommendations,
            timestamp = timestamp,
            providerId = providerId,
            modelId = modelId,
            processingDurationMs = processingDurationMs,
            consentGranted = consentGranted
        )

        companion object {
            fun from(result: AiAnalysisResult) = StoredAiAnalysis(
                analysisId = result.analysisId,
                source = result.source.name,
                category = result.category.name,
                severity = result.severity.name,
                confidence = result.confidence,
                indicators = result.indicators,
                explanation = result.explanation,
                recommendations = result.recommendations,
                educationRecommendations = result.educationRecommendations,
                timestamp = result.timestamp,
                providerId = result.providerId,
                modelId = result.modelId,
                processingDurationMs = result.processingDurationMs,
                consentGranted = result.consentGranted
            )
        }
    }
}
