package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesAssistantChatProviderTest {

    private val provider = RulesAssistantChatProvider()

    @Test
    fun kajiadoFgmQuestionHasCountyFacts() {
        val result = provider.chat(
            AssistantChatInput(
                message = "Tell me about FGM in Kajiado",
                history = emptyList(),
                language = "en"
            )
        )
        assertTrue(result.reply.contains("Kajiado"))
        assertTrue(
            result.reply.contains("78") ||
                result.reply.contains("Maasai") ||
                result.reply.contains("2019") ||
                result.reply.contains("1195")
        )
        assertFalse(result.reply.contains("quick prompt", ignoreCase = true))
    }

    @Test
    fun fgmQuestionReturnsEducationAction() {
        val result = provider.chat(
            AssistantChatInput(
                message = "What is FGM law in Kenya?",
                history = emptyList(),
                language = "en"
            )
        )
        assertTrue(result.reply.contains("illegal", ignoreCase = true))
        assertEquals("education", result.relatedAction)
    }

    @Test
    fun dangerQuestionReturnsEmergencyAction() {
        val result = provider.chat(
            AssistantChatInput(
                message = "I am in danger right now",
                history = emptyList(),
                language = "en"
            )
        )
        assertEquals("emergency", result.relatedAction)
        assertEquals("CRITICAL", result.riskLevel)
    }

    @Test
    fun stigmaQuestionGetsSupportReply() {
        val result = provider.chat(
            AssistantChatInput(
                message = "I am stigmatized",
                history = emptyList(),
                language = "en"
            )
        )
        assertTrue(result.reply.contains("not your fault", ignoreCase = true) || result.reply.contains("1195"))
        assertEquals("support", result.relatedAction)
        assertFalse(result.reply.contains("quick prompt", ignoreCase = true))
    }
}
