package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantChatParserTest {

    @Test
    fun acceptsPlainTextWhenModelSkipsJson() {
        val parsed = AssistantChatParser.parse("Stigma is not your fault. Call 1195 when you can.")
        assertTrue(parsed.reply.contains("1195"))
        assertEquals("NONE", parsed.riskLevel)
    }

    @Test
    fun parsesValidAssistantJson() {
        val parsed = AssistantChatParser.parse(
            """
            {
              "reply":"If you are unsafe, call 1195 or use Emergency Assistance.",
              "relatedAction":"emergency",
              "riskLevel":"HIGH"
            }
            """.trimIndent()
        )
        assertTrue(parsed.reply.contains("1195"))
        assertEquals("emergency", parsed.relatedAction)
        assertEquals("HIGH", parsed.riskLevel)
    }

    @Test(expected = InvalidAiResultException::class)
    fun rejectsMissingReply() {
        AssistantChatParser.parse("""{"riskLevel":"LOW"}""")
    }

    @Test
    fun stripsDefaultSosDemonstrationCode() {
        val parsed = AssistantChatParser.parse(
            """{"reply":"Never use 1102 as your code. Call 999 instead.","riskLevel":"NONE"}"""
        )
        assertFalse(parsed.reply.contains("1102"))
    }
}
