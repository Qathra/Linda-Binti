package com.example.lindabinti.ai

/**
 * Routing policy inputs for hybrid AI selection.
 */
data class AiRoutingPolicy(
    val isBackendConfigured: () -> Boolean,
    val isNetworkAvailable: () -> Boolean,
    val hasCloudAiConsent: () -> Boolean,
    val isUserSessionActive: () -> Boolean,
    val isOnlineAiConfigured: suspend () -> Boolean = { false }
)

@Deprecated(
    message = "Use AiOrchestrator via AiOrchestrationFactory.createOrchestrator",
    replaceWith = ReplaceWith("AiOrchestrationFactory.createOrchestrator(context, prefs, session)")
)
class HybridAiRouter(
    private val offline: OfflineAiAnalysisEngine = OfflineAiAnalysisEngine(),
    private val online: OnlineAiAnalysisEngine,
    private val policy: AiRoutingPolicy
) : AiAnalysisEngine {

    override suspend fun analyze(input: AiAnalysisInput): AiAnalysisResult {
        if (shouldUseOnline()) {
            return runCatching { online.analyze(input) }
                .getOrElse { offline.analyze(input) }
        }
        return offline.analyze(input)
    }

    private suspend fun shouldUseOnline(): Boolean {
        if (!policy.isBackendConfigured()) return false
        if (!policy.isNetworkAvailable()) return false
        if (!policy.hasCloudAiConsent()) return false
        if (!policy.isUserSessionActive()) return false
        return runCatching { policy.isOnlineAiConfigured() }.getOrDefault(false)
    }
}
