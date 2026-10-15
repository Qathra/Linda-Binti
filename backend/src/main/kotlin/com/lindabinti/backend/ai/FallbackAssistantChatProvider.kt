package com.lindabinti.backend.ai

/**
 * Tries the cloud LLM first; on transient provider errors uses [RulesAssistantChatProvider].
 */
class FallbackAssistantChatProvider(
    private val primary: AssistantChatProvider,
    private val fallback: AssistantChatProvider = RulesAssistantChatProvider()
) : AssistantChatProvider {

    override val providerId: String = primary.providerId

    override fun isConfigured(): Boolean = primary.isConfigured()

    override fun chat(input: AssistantChatInput): AssistantChatResult {
        return try {
            primary.chat(input)
        } catch (e: AiProviderException) {
            if (e.code !in TRANSIENT_CODES) throw e
            AiAnalysisLogger.failure(e.code, e.aiStatus, "assistant_fallback=${fallback.providerId}")
            fallback.chat(input).copy(
                providerId = "${fallback.providerId}+cloud-fallback",
                modelId = "rules-v1"
            )
        } catch (e: AiTransportException.Timeout) {
            AiAnalysisLogger.failure("timeout", AiStatus.TIMEOUT, "assistant_fallback=${fallback.providerId}")
            fallback.chat(input).copy(
                providerId = "${fallback.providerId}+cloud-fallback",
                modelId = "rules-v1",
                aiStatus = AiStatus.TIMEOUT
            )
        } catch (e: AiTransportException.Network) {
            AiAnalysisLogger.failure("provider_unavailable", AiStatus.UNAVAILABLE, "assistant_fallback=${fallback.providerId}")
            fallback.chat(input).copy(
                providerId = "${fallback.providerId}+cloud-fallback",
                modelId = "rules-v1",
                aiStatus = AiStatus.UNAVAILABLE
            )
        }
    }

    companion object {
        private val TRANSIENT_CODES = setOf("rate_limited", "timeout", "provider_unavailable")
    }
}
