package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiProviderException
import com.lindabinti.backend.ai.AiResultMerger
import com.lindabinti.backend.ai.AiStatus
import com.lindabinti.backend.ai.AssistantChatTurn
import com.lindabinti.backend.ai.OnlineReportAnalysisResult
import com.lindabinti.backend.api.AiAnalyzeReportResponse
import com.lindabinti.backend.api.AiCapabilitiesResponse
import com.lindabinti.backend.api.AssistantChatResponse

/**
 * Thin facade over [AiOrchestrationService] for survivor report analysis API.
 */
class AiAnalysisService(
    private val orchestration: AiOrchestrationService
) {
    fun isOnlineAvailable(): Boolean = orchestration.isSurvivorOnlineAvailable()

    fun capabilities(): AiCapabilitiesResponse = orchestration.capabilities()

    fun analyzeReportOnline(
        categoryId: String,
        categoryLabel: String,
        description: String,
        includeDescription: Boolean,
        cloudAiConsent: Boolean,
        actorUserId: String = "anonymous"
    ): AiAnalyzeReportResponse = orchestration.analyzeSurvivorReport(
        categoryId = categoryId,
        categoryLabel = categoryLabel,
        description = description,
        includeDescription = includeDescription,
        cloudAiConsent = cloudAiConsent,
        actorUserId = actorUserId
    )
}
