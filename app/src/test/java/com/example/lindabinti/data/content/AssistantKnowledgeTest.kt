package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantKnowledgeTest {

    @Test
    fun kajiadoFgmAnswerIsSpecific() {
        val reply = AssistantKnowledge.answer("FGM in Kajiado", "en")
        assertTrue(reply.text.contains("Kajiado"))
        assertTrue(reply.text.contains("78") || reply.text.contains("Maasai"))
    }

    @Test
    fun answersFgmQuestion() {
        val reply = AssistantKnowledge.answer("What is FGM law?", "en")
        assertTrue(reply.text.contains("illegal", ignoreCase = true))
    }

    @Test
    fun dangerRoutesToEmergency() {
        val reply = AssistantKnowledge.answer("I am in danger", "en")
        assertTrue(reply.relatedAction == "emergency")
        assertTrue(reply.text.contains("999") || reply.text.contains("Emergency"))
    }

    @Test
    fun sosExplanationStaysLocal() {
        val reply = AssistantKnowledge.answer("How does the secret SOS work?", "en")
        assertTrue(reply.text.contains("calculator", ignoreCase = true))
        assertTrue(reply.text.contains("Access Code", ignoreCase = true))
    }

    @Test
    fun sosAnswersNeverIncludeDefaultSosCode() {
        val en = AssistantKnowledge.answer("How does the secret SOS work?", "en")
        val sw = AssistantKnowledge.answer("SOS ya siri inafanya kazi vipi?", "sw")
        assertFalse(en.text.contains("1102"))
        assertFalse(sw.text.contains("1102"))
        AssistantKnowledge.faqs.forEach { faq ->
            assertFalse(faq.answerEn.contains("1102"))
            assertFalse(faq.answerSw.contains("1102"))
            assertFalse(faq.keywords.contains("1102"))
        }
    }

    @Test
    fun kiswahiliDangerUsesSwahiliAnswer() {
        val reply = AssistantKnowledge.answer("Niko hatarini", "sw")
        assertTrue(reply.relatedAction == "emergency")
        assertTrue(reply.text.contains("Dharura") || reply.text.contains("999"))
    }

    @Test
    fun kiswahiliNormalizeKeepsLetters() {
        val reply = AssistantKnowledge.answer("Sheria ya FGM ni nini?", "sw")
        assertTrue(reply.text.contains("kinyume") || reply.text.contains("2011"))
    }

    @Test
    fun fgmSisterImminentQuestionRoutesToEmergency() {
        val reply = AssistantKnowledge.answer("I think my sister is about to undergo FGM", "en")
        assertEquals("emergency", reply.relatedAction)
        assertTrue(reply.text.contains("116") || reply.text.contains("Emergency"))
        assertEquals("CRITICAL", reply.riskLevel)
    }

    @Test
    fun husbandThreatsRecognized() {
        val reply = AssistantKnowledge.answer("My husband keeps threatening me", "en")
        assertEquals("emergency", reply.relatedAction)
        assertTrue(reply.text.contains("1195") || reply.text.contains("999"))
    }

    @Test
    fun scaredToGoHomeIsCritical() {
        val reply = AssistantKnowledge.answer("I am scared to go home", "en")
        assertEquals("emergency", reply.relatedAction)
        assertEquals("CRITICAL", reply.riskLevel)
    }

    @Test
    fun emergencySystemExplainsWithoutSosCode() {
        val reply = AssistantKnowledge.answer("How does the emergency system work?", "en")
        assertTrue(reply.text.contains("999") || reply.text.contains("Emergency Assistance"))
        assertFalse(reply.text.contains("1102"))
    }

    @Test
    fun stigmaQuestionIsSupportive() {
        val reply = AssistantKnowledge.answer("I am stigmatized", "en")
        assertEquals("support", reply.relatedAction)
        assertTrue(reply.text.contains("1195"))
        assertFalse(reply.text.contains("quick prompt", ignoreCase = true))
    }

    @Test
    fun helpFriendQuestionMatchesEducationAction() {
        val reply = AssistantKnowledge.answer("How can I help a friend?", "en")
        assertEquals("education", reply.relatedAction)
    }
}
