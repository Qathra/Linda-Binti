package com.example.lindabinti.ai

/**
 * Calls the backend cloud AI provider through [OnlineAiClient].
 * Returns validated structured results only.
 */
class OnlineAiAnalysisEngine(
    private val client: OnlineAiClient
) : AiAnalysisEngine {

    override suspend fun analyze(input: AiAnalysisInput): AiAnalysisResult {
        val safe = PrivacyFilter.prepareOnlineInput(input)
        val result = client.analyzeReport(safe)
        validate(result)
        return result.copy(source = AiAnalysisSource.ONLINE)
    }

    private fun validate(result: AiAnalysisResult) {
        if (result.explanation.isBlank()) {
            throw AiAnalysisException("Invalid online AI response: missing explanation")
        }
        if (result.confidence !in 0.0..1.0) {
            throw AiAnalysisException("Invalid online AI response: confidence out of range")
        }
        if (result.source != AiAnalysisSource.ONLINE) {
            throw AiAnalysisException("Invalid online AI response: wrong source")
        }
    }
}
