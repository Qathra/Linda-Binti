package com.example.lindabinti.assistant

import com.example.lindabinti.LindaBintiApp
import com.example.lindabinti.data.content.AssistantKnowledge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAssistantEngine : AssistantEngine {
    override suspend fun answer(
        userText: String,
        history: List<AssistantChatTurn>
    ): AssistantMessage = withContext(Dispatchers.Default) {
        val language = runCatching {
            LindaBintiApp.instance.authRepository.getLanguage()
        }.getOrDefault("en")
        val reply = AssistantKnowledge.answer(userText, language)
        val message = AssistantMessage(
            text = reply.text,
            relatedAction = reply.relatedAction,
            source = AssistantSource.Local,
            riskLevel = reply.riskLevel
        )
        return@withContext OfflineRiskBooster.boost(userText, message, language)
    }
}

data class AssistantRoutingPolicy(
    val isBackendConfigured: () -> Boolean,
    val isNetworkAvailable: () -> Boolean,
    val hasCloudAiConsent: () -> Boolean,
    val isUserSessionActive: () -> Boolean,
    val isOnlineAssistantConfigured: suspend () -> Boolean = { false }
)

/**
 * Tries secure backend assistant when policy allows; always falls back to offline knowledge.
 */
@Deprecated(
    message = "Assistant routing is internal to AiOrchestrator",
    replaceWith = ReplaceWith("AiOrchestrator.assistantReply")
)
class HybridAssistantEngine(
    private val local: AssistantEngine = LocalAssistantEngine(),
    private val remote: AssistantRemoteClient?,
    private val policy: AssistantRoutingPolicy
) : AssistantEngine {

    suspend fun currentModeLabel(): String =
        if (shouldUseOnline()) AssistantSource.Remote.modeLabel else AssistantSource.Local.modeLabel

    override suspend fun answer(
        userText: String,
        history: List<AssistantChatTurn>
    ): AssistantMessage {
        if (shouldUseOnline()) {
            val client = remote ?: return local.answer(userText, history)
            return runCatching { client.answer(userText, history) }
                .getOrNull()
                ?.copy(source = AssistantSource.Remote)
                ?: local.answer(userText, history)
        }
        return local.answer(userText, history)
    }

    private suspend fun shouldUseOnline(): Boolean {
        if (remote == null) return false
        if (!policy.isBackendConfigured()) return false
        if (!policy.isNetworkAvailable()) return false
        if (!policy.hasCloudAiConsent()) return false
        if (!policy.isUserSessionActive()) return false
        return runCatching { policy.isOnlineAssistantConfigured() }.getOrDefault(false)
    }
}
