package com.example.lindabinti.assistant

data class AssistantChatTurn(
    val role: String,
    val content: String
)

/**
 * Pluggable assistant contract so a secure AI backend can wrap the on-device engine.
 */
data class AssistantMessage(
    val text: String,
    val relatedAction: String? = null,
    val source: AssistantSource = AssistantSource.Local,
    val riskLevel: String? = null
)

enum class AssistantSource {
    Local,
    Remote;

    val modeLabel: String
        get() = when (this) {
            Local -> "OFFLINE AI"
            Remote -> "ONLINE AI"
        }
}

interface AssistantEngine {
    suspend fun answer(
        userText: String,
        history: List<AssistantChatTurn> = emptyList()
    ): AssistantMessage
}

interface AssistantRemoteClient {
    suspend fun answer(
        userText: String,
        history: List<AssistantChatTurn>
    ): AssistantMessage?
}
