package com.lindabinti.backend.ai

/**
 * Maps OpenAI-compatible HTTP responses to safe, actionable errors.
 * Never includes API keys in messages.
 */
object HttpAiErrorMapper {

    fun ensureSuccess(statusCode: Int, body: String) {
        if (statusCode in 200..299) return
        AiAnalysisLogger.failure("http_error", AiStatus.UNAVAILABLE, "status=$statusCode")
        when (statusCode) {
            401, 403 -> throw AiProviderException(
                "invalid_api_key",
                "AI API key rejected by provider",
                AiStatus.UNAVAILABLE
            )
            429 -> throw AiProviderException(
                "rate_limited",
                "AI provider rate limit reached",
                AiStatus.UNAVAILABLE
            )
            in 500..599 -> throw AiTransportException.Network()
            else -> throw AiTransportException.Network()
        }
    }
}
