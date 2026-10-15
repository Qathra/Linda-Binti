package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AssistantChatTurn
import com.lindabinti.backend.api.AssistantChatResponse

class AssistantChatService(
    private val orchestration: AiOrchestrationService
) {
    fun isOnlineAvailable(): Boolean = orchestration.isAssistantOnlineAvailable()

    fun chat(
        message: String,
        history: List<AssistantChatTurn>,
        language: String,
        cloudAiConsent: Boolean,
        actorUserId: String = "anonymous"
    ): AssistantChatResponse = orchestration.assistantChat(
        message = message,
        history = history,
        language = language,
        cloudAiConsent = cloudAiConsent,
        actorUserId = actorUserId
    )
}
