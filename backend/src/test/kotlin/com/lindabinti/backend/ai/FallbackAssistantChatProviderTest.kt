package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackAssistantChatProviderTest {

    @Test
    fun fallsBackToRulesOnRateLimit() {
        val primary = object : AssistantChatProvider {
            override val providerId = "cloud"
            override fun isConfigured() = true
            override fun chat(input: AssistantChatInput): AssistantChatResult {
                throw AiProviderException("rate_limited", "limit", AiStatus.UNAVAILABLE)
            }
        }
        val fallback = RulesAssistantChatProvider()
        val result = FallbackAssistantChatProvider(primary, fallback).chat(
            AssistantChatInput("My husband keeps threatening me", emptyList(), "en")
        )
        assertTrue(result.reply.isNotBlank())
        assertTrue(result.providerId.contains("fallback"))
    }
}
