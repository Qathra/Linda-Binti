package com.example.lindabinti.ai

interface AiAnalysisEngine {
    suspend fun analyze(input: AiAnalysisInput): AiAnalysisResult
}

class AiAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
