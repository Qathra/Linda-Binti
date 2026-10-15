package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantRagSafetyTest {

    @Test
    fun retrievesKajiadoFgmContext() {
        val hits = AssistantKnowledgeBase.retrieve("Tell me about FGM in Kajiado County")
        assertTrue(hits.any { it.id == "kajiado_fgm" || it.id == "kajiado_help" })
        val blob = hits.joinToString { it.body }
        assertTrue(blob.contains("Kajiado") || blob.contains("Maasai") || blob.contains("1195"))
    }

    @Test
    fun retrievesFgmLawSnippet() {
        val hits = AssistantKnowledgeBase.retrieve("Is FGM illegal for my sister?")
        assertTrue(hits.any { it.id == "fgm_law" })
        val prompt = AssistantKnowledgeBase.formatForPrompt(hits)
        assertTrue(prompt.contains("CURATED_KNOWLEDGE"))
        assertTrue(prompt.contains("116") || prompt.contains("1195"))
    }

    @Test
    fun detectsUrgentDanger() {
        val a = AssistantRiskDetector.assess("I am in danger right now help me")
        assertTrue(a.urgent)
        assertEquals("CRITICAL", a.riskLevel)
        assertEquals("emergency", a.relatedAction)
    }

    @Test
    fun safetyFilterRemovesPinGuess() {
        val cleaned = AssistantSafetyFilter.sanitizeReply("Your PIN is 1102 and call 0712345678")
        assertFalse(cleaned.contains("1102"))
        assertTrue(cleaned.contains("999") || cleaned.contains("unverified") || cleaned.contains("secret"))
    }

    @Test
    fun mergeRiskPrefersCritical() {
        assertEquals("CRITICAL", AssistantSafetyFilter.mergeRisk("LOW", "CRITICAL"))
        assertEquals("emergency", AssistantSafetyFilter.preferAction("reporting", "emergency"))
    }
}
