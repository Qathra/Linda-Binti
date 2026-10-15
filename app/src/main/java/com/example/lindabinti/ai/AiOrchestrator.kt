package com.example.lindabinti.ai

import com.example.lindabinti.assistant.AssistantEngine
import com.example.lindabinti.assistant.AssistantChatTurn
import com.example.lindabinti.assistant.AssistantMessage
import com.example.lindabinti.assistant.AssistantRemoteClient
import com.example.lindabinti.assistant.AssistantSource

enum class AiOrchestrationMode {
    OFFLINE,
    ONLINE,
    DEGRADED
}

/** Why the safety assistant is not using cloud AI (for UI hints). */
enum class AssistantOnlineBlocker {
    NONE,
    BACKEND_NOT_CONFIGURED,
    NO_NETWORK,
    NOT_SIGNED_IN,
    CLOUD_AI_OFF,
    SERVER_AI_UNAVAILABLE,
    ONLINE_REQUEST_FAILED
}

data class AssistantReplyResult(
    val message: AssistantMessage,
    val blocker: AssistantOnlineBlocker = AssistantOnlineBlocker.NONE
)

/**
 * Single Android entry point for survivor report analysis and safety assistant chat.
 * Offline engines always run for reports; online runs only with consent, session, network,
 * and a configured cloud provider. Assistant falls back to offline knowledge when cloud fails.
 */
class AiOrchestrator(
    private val offline: OfflineAiAnalysisEngine,
    private val online: OnlineAiAnalysisEngine,
    private val localAssistant: AssistantEngine,
    private val remoteAssistant: AssistantRemoteClient?,
    private val policy: AiRoutingPolicy,
    private val isOnlineAssistantConfigured: suspend () -> Boolean = { false }
) : AiAnalysisEngine {

    override suspend fun analyze(input: AiAnalysisInput): AiAnalysisResult {
        val offlineResult = offline.analyze(input)
        if (!shouldUseOnlineAnalysis()) {
            return offlineResult
        }
        val onlineResult = runCatching { online.analyze(input) }.getOrNull()
        return if (onlineResult != null) {
            AiAnalysisMerger.merge(offlineResult, onlineResult)
        } else {
            offlineResult
        }
    }

    suspend fun analyzeAndPersist(
        input: AiAnalysisInput,
        persist: suspend (AiAnalysisResult) -> Unit
    ): AiAnalysisResult {
        val result = analyze(input)
        persist(result)
        return result
    }

    suspend fun assistantReply(
        userText: String,
        history: List<AssistantChatTurn> = emptyList()
    ): AssistantReplyResult {
        val sessionBlocker = assistantSessionBlocker()
        if (sessionBlocker != AssistantOnlineBlocker.NONE) {
            return AssistantReplyResult(
                message = localAssistant.answer(userText, history),
                blocker = sessionBlocker
            )
        }
        val client = remoteAssistant ?: return AssistantReplyResult(
            message = localAssistant.answer(userText, history),
            blocker = AssistantOnlineBlocker.SERVER_AI_UNAVAILABLE
        )
        val remote = runCatching { client.answer(userText, history) }
            .getOrNull()
            ?.copy(source = AssistantSource.Remote)
        if (remote != null) {
            return AssistantReplyResult(message = remote, blocker = AssistantOnlineBlocker.NONE)
        }
        return AssistantReplyResult(
            message = localAssistant.answer(userText, history),
            blocker = AssistantOnlineBlocker.ONLINE_REQUEST_FAILED
        )
    }

    /** Session + consent readiness (chip label). Does not depend on a single chat result. */
    suspend fun assistantSessionBlocker(): AssistantOnlineBlocker {
        if (!policy.isBackendConfigured()) return AssistantOnlineBlocker.BACKEND_NOT_CONFIGURED
        if (!policy.isNetworkAvailable()) return AssistantOnlineBlocker.NO_NETWORK
        if (!policy.isUserSessionActive()) return AssistantOnlineBlocker.NOT_SIGNED_IN
        if (!policy.hasCloudAiConsent()) return AssistantOnlineBlocker.CLOUD_AI_OFF
        return AssistantOnlineBlocker.NONE
    }

    suspend fun assistantOnlineBlocker(): AssistantOnlineBlocker {
        val sessionBlocker = assistantSessionBlocker()
        if (sessionBlocker != AssistantOnlineBlocker.NONE) return sessionBlocker
        if (!runCatching { isOnlineAssistantConfigured() }.getOrDefault(false)) {
            return AssistantOnlineBlocker.SERVER_AI_UNAVAILABLE
        }
        return AssistantOnlineBlocker.NONE
    }

    /** Top-bar label: ONLINE when signed in with cloud consent (not per-message fallback). */
    suspend fun assistantStatusLabel(): String =
        if (assistantSessionBlocker() == AssistantOnlineBlocker.NONE) {
            AssistantSource.Remote.modeLabel
        } else {
            AssistantSource.Local.modeLabel
        }

    /** Label for UI chrome — ONLINE AI only when cloud assistant is actually reachable. */
    suspend fun assistantAvailabilityLabel(): String = assistantStatusLabel()

    suspend fun routingMode(): AiOrchestrationMode {
        if (!policy.isBackendConfigured()) return AiOrchestrationMode.OFFLINE
        if (!policy.hasCloudAiConsent()) return AiOrchestrationMode.OFFLINE
        if (!policy.isUserSessionActive()) return AiOrchestrationMode.OFFLINE
        if (!policy.isNetworkAvailable()) return AiOrchestrationMode.OFFLINE
        val reportOnline = runCatching { policy.isOnlineAiConfigured() }.getOrDefault(false)
        val assistantOnline = runCatching { isOnlineAssistantConfigured() }.getOrDefault(false)
        return when {
            reportOnline && assistantOnline -> AiOrchestrationMode.ONLINE
            reportOnline || assistantOnline -> AiOrchestrationMode.DEGRADED
            else -> AiOrchestrationMode.OFFLINE
        }
    }

    fun canUseCloudAi(): Boolean =
        policy.isBackendConfigured() &&
            policy.hasCloudAiConsent() &&
            policy.isUserSessionActive()

    /** True when report analysis may call the cloud provider (consent, session, network, configured). */
    suspend fun canUseOnlineAnalysis(): Boolean = shouldUseOnlineAnalysis()

    private suspend fun shouldUseOnlineAnalysis(): Boolean {
        if (!policy.isBackendConfigured()) return false
        if (!policy.isNetworkAvailable()) return false
        if (!policy.hasCloudAiConsent()) return false
        if (!policy.isUserSessionActive()) return false
        return runCatching { policy.isOnlineAiConfigured() }.getOrDefault(false)
    }

    private suspend fun shouldUseOnlineAssistant(): Boolean {
        if (remoteAssistant == null) return false
        if (!policy.isBackendConfigured()) return false
        if (!policy.isNetworkAvailable()) return false
        if (!policy.hasCloudAiConsent()) return false
        if (!policy.isUserSessionActive()) return false
        return runCatching { isOnlineAssistantConfigured() }.getOrDefault(false)
    }

    companion object {
        fun resetOnlineAssistantPause() {
            // No-op: kept for clear-chat compatibility.
        }

        /** @deprecated Use [com.example.lindabinti.compass.CompassSourceLabel.resolve] for compass UI. */
        fun resolveAnalysisSourceLabel(results: List<AiAnalysisResult>): String =
            com.example.lindabinti.compass.CompassSourceLabel.resolve(results)
    }
}
