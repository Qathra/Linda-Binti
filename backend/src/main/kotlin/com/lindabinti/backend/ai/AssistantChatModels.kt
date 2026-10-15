package com.lindabinti.backend.ai

data class AssistantChatTurn(
    val role: String,
    val content: String
)

data class AssistantChatInput(
    val message: String,
    val history: List<AssistantChatTurn>,
    val language: String,
    val retrievedContext: String = "",
    val riskHint: String = "NONE"
)

data class AssistantChatResult(
    val reply: String,
    val relatedAction: String?,
    val riskLevel: String,
    val providerId: String,
    val modelId: String,
    val processingDurationMs: Long,
    val aiStatus: String = AiStatus.COMPLETE
)

interface AssistantChatProvider {
    val providerId: String
    fun isConfigured(): Boolean
    fun chat(input: AssistantChatInput): AssistantChatResult
}
