package com.example.lindabinti.assistant

import com.example.lindabinti.ai.PrivacyFilter
import com.example.lindabinti.data.remote.AssistantChatRequestDto
import com.example.lindabinti.data.remote.AssistantChatTurnDto
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.LindaBintiApp

class HttpAssistantRemoteClient(
    private val api: LindaBintiApi,
    private val session: BackendSession,
    private val hasCloudAiConsent: () -> Boolean
) : AssistantRemoteClient {

    override suspend fun answer(
        userText: String,
        history: List<AssistantChatTurn>
    ): AssistantMessage? {
        return runCatching {
            answerInternal(userText, history)
        }.getOrNull()
    }

    private suspend fun answerInternal(
        userText: String,
        history: List<AssistantChatTurn>
    ): AssistantMessage? {
        val app = LindaBintiApp.instance
        app.backendAccountRepository.ensureValidSession(app.securePrefs)
        if (!hasCloudAiConsent()) return null
        return requestAnswer(userText, history, retryOnUnauthorized = true)
    }

    private suspend fun requestAnswer(
        userText: String,
        history: List<AssistantChatTurn>,
        retryOnUnauthorized: Boolean
    ): AssistantMessage? {
        val bearer = session.bearer() ?: return null
        val language = runCatching {
            LindaBintiApp.instance.authRepository.getLanguage()
        }.getOrDefault("en")
        val safeMessage = PrivacyFilter.redactForCloudAnalysis(userText, maxChars = 500)
        if (safeMessage.isBlank()) return null
        val safeHistory = history.takeLast(6).mapNotNull { turn ->
            val role = turn.role.trim().lowercase()
            if (role !in setOf("user", "assistant")) return@mapNotNull null
            val content = PrivacyFilter.redactForCloudAnalysis(turn.content, maxChars = 300)
            if (content.isBlank()) return@mapNotNull null
            AssistantChatTurnDto(role = role, content = content)
        }
        val response = api.assistantChat(
            bearer = bearer,
            body = AssistantChatRequestDto(
                message = safeMessage,
                history = safeHistory,
                language = language,
                cloudAiConsent = true
            )
        )
        if (response.code() == 401 && retryOnUnauthorized) {
            val app = LindaBintiApp.instance
            if (app.backendAccountRepository.ensureValidSession(app.securePrefs)) {
                return requestAnswer(userText, history, retryOnUnauthorized = false)
            }
            return null
        }
        if (!response.isSuccessful) return null
        val body = response.body() ?: return null
        if (body.reply.isBlank()) return null
        return AssistantMessage(
            text = body.reply,
            relatedAction = body.relatedAction,
            source = AssistantSource.Remote,
            riskLevel = body.riskLevel
        )
    }

    suspend fun isOnlineAssistantConfigured(): Boolean {
        return runCatching {
            val caps = api.aiCapabilities()
            caps.onlineEnabled || caps.statusLabel == "ONLINE AI: CONFIGURED"
        }.getOrElse {
            runCatching {
                api.health()["ai"] == "configured"
            }.getOrDefault(false)
        }
    }
}

class UnavailableAssistantRemoteClient : AssistantRemoteClient {
    override suspend fun answer(userText: String, history: List<AssistantChatTurn>): AssistantMessage? = null
}
