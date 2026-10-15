package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiAnalysisLogger
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiHttpClient
import com.lindabinti.backend.ai.AiProviderException
import com.lindabinti.backend.ai.AiProviderFactory
import com.lindabinti.backend.ai.AiResultMerger
import com.lindabinti.backend.ai.AiRiskAnalyzer
import com.lindabinti.backend.ai.AiRiskInput
import com.lindabinti.backend.ai.AiRiskResult
import com.lindabinti.backend.ai.AiStatus
import com.lindabinti.backend.ai.AssistantChatInput
import com.lindabinti.backend.ai.AssistantChatProvider
import com.lindabinti.backend.ai.AssistantChatProviderFactory
import com.lindabinti.backend.ai.AssistantChatTurn
import com.lindabinti.backend.ai.AssistantKnowledgeBase
import com.lindabinti.backend.ai.AssistantRiskDetector
import com.lindabinti.backend.ai.AssistantSafetyFilter
import com.lindabinti.backend.ai.FallbackAiRiskAnalyzer
import com.lindabinti.backend.ai.HttpAiRiskAnalyzer
import com.lindabinti.backend.ai.JdkAiHttpClient
import com.lindabinti.backend.ai.OnlineAiProvider
import com.lindabinti.backend.ai.OnlineReportAnalysisResult
import com.lindabinti.backend.ai.PiiMinimizer
import com.lindabinti.backend.ai.RulesAiRiskAnalyzer
import com.lindabinti.backend.api.AiAnalyzeReportResponse
import com.lindabinti.backend.api.AiCapabilitiesResponse
import com.lindabinti.backend.api.AssistantChatResponse

enum class AiUseCase {
    SURVIVOR_ANALYZE,
    ASSISTANT_CHAT,
    NGO_FLAG,
    INTELLIGENCE
}

data class AiOrchestrationHealth(
    val health: String,
    val label: String,
    val onlineConfigured: Boolean,
    val degradedReason: String = ""
)

/**
 * Central AI orchestration for survivor analysis, assistant chat, NGO flagging, and health.
 * API keys remain backend-only. Never triggers SOS or authority contact.
 */
class AiOrchestrationService(
    private val onlineProvider: OnlineAiProvider,
    private val chatProvider: AssistantChatProvider,
    private val rulesAnalyzer: AiRiskAnalyzer,
    private val ngoOnlineAnalyzer: AiRiskAnalyzer?,
    private val aiConfig: AiConfig,
    private val audit: AuditService
) {
    fun isSurvivorOnlineAvailable(): Boolean = aiConfig.onlineEnabled && onlineProvider.isConfigured()

    fun isAssistantOnlineAvailable(): Boolean = aiConfig.onlineEnabled && chatProvider.isConfigured()

    fun capabilities(): AiCapabilitiesResponse = AiCapabilitiesResponse(
        onlineEnabled = isSurvivorOnlineAvailable(),
        statusLabel = aiConfig.onlineStatusLabel,
        configurationReason = aiConfig.configurationReason,
        providerId = if (isSurvivorOnlineAvailable()) onlineProvider.providerId else null,
        model = if (isSurvivorOnlineAvailable()) aiConfig.model else null,
        consentRequired = true,
        timeoutMs = aiConfig.timeoutMs,
        maxRetries = aiConfig.maxRetries
    )

    fun analyzeSurvivorReport(
        categoryId: String,
        categoryLabel: String,
        description: String,
        includeDescription: Boolean,
        cloudAiConsent: Boolean,
        actorUserId: String
    ): AiAnalyzeReportResponse {
        requireCloudConsent(cloudAiConsent)
        if (!isSurvivorOnlineAvailable()) {
            throw providerUnavailable()
        }
        val normalizedCategory = GbvFgmScope.requireValidCategory(categoryId)
        val input = PiiMinimizer.toInput(normalizedCategory, categoryLabel, description, includeDescription)
        AiAnalysisLogger.info(
            "orchestrate_survivor_analyze",
            "category=$normalizedCategory includeDescription=$includeDescription"
        )
        audit.log(actorUserId, "AI_SURVIVOR_ANALYZE", "useCase=${AiUseCase.SURVIVOR_ANALYZE}")
        val offline = rulesAnalyzer.analyze(input)
        val online = onlineProvider.analyzeReport(input)
        val merged = AiResultMerger.mergeSurvivorOnline(offline, online)
        return merged.toResponse()
    }

    fun assistantChat(
        message: String,
        history: List<AssistantChatTurn>,
        language: String,
        cloudAiConsent: Boolean,
        actorUserId: String
    ): AssistantChatResponse {
        requireCloudConsent(cloudAiConsent)
        if (!isAssistantOnlineAvailable()) {
            throw providerUnavailable()
        }
        val lang = language.ifBlank { "en" }.take(8)
        val rawMessage = AssistantSafetyFilter.sanitizeUserMessage(message)
        val sanitizedMessage = PiiMinimizer.minimizeDescription(rawMessage, maxChars = 500)
        if (sanitizedMessage.isBlank()) throw IllegalArgumentException("message_required")

        val risk = AssistantRiskDetector.assess(sanitizedMessage)
        val retrieved = AssistantKnowledgeBase.retrieve(sanitizedMessage, limit = 5)
        val contextBlock = AssistantKnowledgeBase.formatForPrompt(retrieved)

        // For clear imminent danger, return a deterministic safety reply (still audited).
        if (risk.urgent) {
            audit.log(
                actorUserId,
                "AI_ASSISTANT_CHAT",
                "useCase=${AiUseCase.ASSISTANT_CHAT};path=urgent_rules;risk=${risk.riskLevel}"
            )
            val reply = AssistantSafetyFilter.sanitizeReply(
                AssistantRiskDetector.urgentSafetyReply(lang)
            )
            return AssistantChatResponse(
                reply = reply,
                source = AiResultMerger.LABEL_ONLINE,
                relatedAction = "emergency",
                riskLevel = "CRITICAL",
                providerId = "rules-urgent",
                modelId = "risk-detector-v1",
                processingDurationMs = 0,
                aiStatus = AiStatus.COMPLETE
            )
        }

        val sanitizedHistory = history
            .takeLast(6)
            .mapNotNull { turn ->
                val role = turn.role.trim().lowercase()
                if (role !in setOf("user", "assistant")) return@mapNotNull null
                val content = PiiMinimizer.minimizeDescription(turn.content, maxChars = 300)
                if (content.isBlank()) return@mapNotNull null
                AssistantChatTurn(role = role, content = content)
            }
        audit.log(
            actorUserId,
            "AI_ASSISTANT_CHAT",
            "useCase=${AiUseCase.ASSISTANT_CHAT};rag=${retrieved.size};riskHint=${risk.riskLevel}"
        )
        val result = chatProvider.chat(
            AssistantChatInput(
                message = sanitizedMessage,
                history = sanitizedHistory,
                language = lang,
                retrievedContext = contextBlock,
                riskHint = risk.riskLevel
            )
        )
        val mergedRisk = AssistantSafetyFilter.mergeRisk(risk.riskLevel, result.riskLevel)
        val mergedAction = AssistantSafetyFilter.preferAction(
            risk.relatedAction ?: retrieved.firstOrNull()?.relatedAction,
            result.relatedAction
        )
        val safeReply = AssistantSafetyFilter.ensureCriticalGuidance(
            reply = AssistantSafetyFilter.sanitizeReply(result.reply),
            language = lang,
            urgent = mergedRisk == "CRITICAL"
        )
        return AssistantChatResponse(
            reply = safeReply,
            source = AiResultMerger.LABEL_ONLINE,
            relatedAction = mergedAction,
            riskLevel = mergedRisk,
            providerId = result.providerId,
            modelId = result.modelId,
            processingDurationMs = result.processingDurationMs,
            aiStatus = result.aiStatus
        )
    }

    fun analyzeNgoFlag(input: AiRiskInput, actorUserId: String): AiRiskResult {
        audit.log(actorUserId, "AI_NGO_FLAG", "useCase=${AiUseCase.NGO_FLAG};category=${input.categoryId}")
        val offline = rulesAnalyzer.analyze(input)
        val onlineAnalyzer = ngoOnlineAnalyzer
        if (onlineAnalyzer == null || !aiConfig.onlineEnabled) {
            return offline.copy(source = AiResultMerger.SOURCE_OFFLINE, status = AiStatus.COMPLETE)
        }
        val online = onlineAnalyzer.analyze(input)
        return AiResultMerger.mergeNgo(offline, online)
    }

    fun resolveEngineHealth(flags: List<ReportFlag>, pendingAi: Int): AiOrchestrationHealth {
        if (!aiConfig.onlineEnabled) {
            return AiOrchestrationHealth(
                health = "OFFLINE",
                label = "OFFLINE",
                onlineConfigured = false,
                degradedReason = "Online AI is not configured; ${AiResultMerger.LABEL_OFFLINE} is active."
            )
        }
        val rulesFallback = flags.count { it.classifierSource == AiResultMerger.SOURCE_OFFLINE }
        val failed = flags.count {
            it.aiStatus.equals(AiStatus.FAILED, ignoreCase = true) ||
                it.aiStatus.equals(AiStatus.UNAVAILABLE, ignoreCase = true) ||
                it.aiStatus.equals(AiStatus.TIMEOUT, ignoreCase = true)
        }
        val degraded = pendingAi > 0 || rulesFallback > 0 || failed > 0
        if (degraded) {
            val reason = when {
                pendingAi > 0 -> "$pendingAi case(s) awaiting AI analysis."
                failed > 0 -> "$failed case(s) used failed or unavailable AI paths."
                else -> "$rulesFallback case(s) classified with ${AiResultMerger.LABEL_OFFLINE}."
            }
            return AiOrchestrationHealth(
                health = "DEGRADED",
                label = "DEGRADED",
                onlineConfigured = true,
                degradedReason = reason
            )
        }
        return AiOrchestrationHealth(
            health = "ONLINE",
            label = "ONLINE",
            onlineConfigured = true
        )
    }

    private fun requireCloudConsent(cloudAiConsent: Boolean) {
        if (!cloudAiConsent) {
            throw AiProviderException("consent_required", "Cloud AI consent is required", AiStatus.UNAVAILABLE)
        }
    }

    private fun providerUnavailable(): AiProviderException = AiProviderException(
        if (aiConfig.enabledFlag == false) "ai_disabled" else "provider_unconfigured",
        "Online AI is not configured on the server",
        AiStatus.UNAVAILABLE
    )

    private fun OnlineReportAnalysisResult.toResponse() = AiAnalyzeReportResponse(
        analysisId = analysisId,
        source = source,
        category = category,
        severity = severity,
        confidence = confidence,
        indicators = indicators,
        explanation = explanation,
        recommendations = recommendations,
        educationRecommendations = educationRecommendations,
        timestamp = timestamp,
        providerId = providerId,
        modelId = modelId,
        processingDurationMs = processingDurationMs,
        aiStatus = aiStatus
    )

    companion object {
        fun create(
            aiConfig: AiConfig,
            audit: AuditService,
            http: AiHttpClient = JdkAiHttpClient()
        ): AiOrchestrationService {
            val online = AiProviderFactory.createOnlineProvider(aiConfig, http)
            val chat = AssistantChatProviderFactory.create(aiConfig, http)
            val ngoOnline = if (aiConfig.onlineEnabled) {
                FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(aiConfig, http))
            } else {
                null
            }
            return AiOrchestrationService(
                onlineProvider = online,
                chatProvider = chat,
                rulesAnalyzer = RulesAiRiskAnalyzer(),
                ngoOnlineAnalyzer = ngoOnline,
                aiConfig = aiConfig,
                audit = audit
            )
        }

        fun rulesOnly(audit: AuditService): AiOrchestrationService = create(
            aiConfig = AiConfig("", "", "gpt-4o-mini", 8000, 0, false),
            audit = audit
        )
    }
}
