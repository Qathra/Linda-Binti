package com.example.lindabinti.ai

/**
 * Optional remote client for cloud AI analysis. Implementations must never log report bodies.
 */
interface OnlineAiClient {
    suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult
    suspend fun isOnlineAiConfigured(): Boolean
}

class UnavailableOnlineAiClient : OnlineAiClient {
    override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult {
        throw AiAnalysisException("Online AI is not configured")
    }

    override suspend fun isOnlineAiConfigured(): Boolean = false
}
