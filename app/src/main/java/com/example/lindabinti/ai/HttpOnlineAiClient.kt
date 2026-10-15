package com.example.lindabinti.ai

import com.example.lindabinti.data.remote.AiAnalyzeReportRequestDto
import com.example.lindabinti.data.remote.AiAnalyzeReportResponseDto
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.remote.LindaBintiApi

class HttpOnlineAiClient(
    private val api: LindaBintiApi,
    private val session: BackendSession,
    private val hasCloudAiConsent: () -> Boolean
) : OnlineAiClient {

    override suspend fun isOnlineAiConfigured(): Boolean {
        return runCatching {
            val caps = api.aiCapabilities()
            caps.onlineEnabled || caps.statusLabel == "ONLINE AI: CONFIGURED"
        }.getOrElse {
            runCatching {
                val health = api.health()
                health["onlineAiStatus"] == "ONLINE AI: CONFIGURED" ||
                    health["ai"] == "configured"
            }.getOrDefault(false)
        }
    }

    override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult {
        val bearer = session.bearer()
            ?: throw AiAnalysisException("Cloud AI requires an authenticated USER session")
        if (!hasCloudAiConsent()) {
            throw AiAnalysisException("Cloud AI consent is required")
        }
        val safe = PrivacyFilter.prepareOnlineInput(input)
        PrivacyFilter.assertSafeForCloud(
            mapOf(
                "categoryId" to safe.categoryId,
                "categoryLabel" to safe.categoryLabel,
                "description" to safe.description
            )
        )
        val response = api.analyzeReport(
            bearer = bearer,
            body = AiAnalyzeReportRequestDto(
                categoryId = safe.categoryId,
                categoryLabel = safe.categoryLabel,
                description = safe.description,
                includeDescription = safe.includeDescription,
                cloudAiConsent = true
            )
        )
        if (!response.isSuccessful) {
            throw AiAnalysisException("Online AI unavailable (${response.code()})")
        }
        val body = response.body()
            ?: throw AiAnalysisException("Online AI returned an empty response")
        return body.toResult(consentGranted = true)
    }
}

private fun AiAnalyzeReportResponseDto.toResult(consentGranted: Boolean): AiAnalysisResult {
    val parsedCategory = runCatching {
        AiAnalysisCategory.valueOf(category.uppercase())
    }.getOrDefault(AiAnalysisCategory.UNKNOWN)
    val parsedSeverity = runCatching {
        AiSeverity.valueOf(severity.uppercase())
    }.getOrElse { AiSeverity.fromBackendLevel(severity) }
    val parsedSource = runCatching {
        AiAnalysisSource.valueOf(source.uppercase())
    }.getOrDefault(AiAnalysisSource.ONLINE)
    if (explanation.isBlank()) {
        throw AiAnalysisException("Online AI returned an empty explanation")
    }
    return AiAnalysisResult(
        analysisId = analysisId,
        source = parsedSource,
        category = parsedCategory,
        severity = parsedSeverity,
        confidence = confidence.coerceIn(0.0, 1.0),
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
}
