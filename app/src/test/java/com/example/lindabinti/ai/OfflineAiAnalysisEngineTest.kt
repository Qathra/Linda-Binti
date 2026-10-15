package com.example.lindabinti.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineAiAnalysisEngineTest {

    private val engine = OfflineAiAnalysisEngine()

    @Test
    fun detectsGbvPhysicalViolence() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "physical",
                categoryLabel = "Physical violence",
                description = "He hit me repeatedly at home"
            )
        )
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
        assertEquals(AiAnalysisCategory.GBV, result.category)
        assertEquals("OFFLINE AI", result.sourceLabel)
        assertTrue(result.severity.ordinal >= AiSeverity.ELEVATED.ordinal)
        assertTrue(result.explanation.contains("Offline AI Safety Analysis"))
    }

    @Test
    fun detectsFgmIncident() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "fgm_incident",
                categoryLabel = "FGM incident",
                description = "She was cut yesterday"
            )
        )
        assertEquals(AiAnalysisCategory.FGM, result.category)
        assertEquals(AiSeverity.HIGH, result.severity)
    }

    @Test
    fun detectsFgmThreatWithImminentLanguage() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "fgm_pressure",
                categoryLabel = "FGM pressure",
                description = "They will cut my sister tomorrow at the ceremony"
            )
        )
        assertEquals(AiAnalysisCategory.FGM_THREAT, result.category)
        assertTrue(result.indicators.any { it.contains("Imminent", ignoreCase = true) })
        assertTrue(result.severity.ordinal >= AiSeverity.HIGH.ordinal)
    }

    @Test
    fun detectsHighRiskWeaponLanguage() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "threats",
                categoryLabel = "Threats",
                description = "He has a knife and says he will hurt me tonight"
            )
        )
        assertTrue(result.indicators.any { it.contains("Weapon", ignoreCase = true) })
        assertTrue(result.indicators.any { it.contains("Immediate", ignoreCase = true) })
        assertEquals(AiSeverity.CRITICAL, result.severity)
    }

    @Test
    fun detectsCriticalChildAtRisk() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "fgm_pressure",
                categoryLabel = "FGM pressure",
                description = "My daughter will be cut tonight right now"
            )
        )
        assertEquals(AiAnalysisCategory.FGM_THREAT, result.category)
        assertTrue(result.indicators.any { it.contains("Child", ignoreCase = true) })
        assertEquals(AiSeverity.CRITICAL, result.severity)
    }

    @Test
    fun harmlessGeneralTextStaysLowerPriority() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "other",
                categoryLabel = "Other",
                description = "I want general information about community meetings"
            )
        )
        assertTrue(result.severity.ordinal <= AiSeverity.ELEVATED.ordinal)
        assertFalse(result.indicators.any { it.contains("Weapon") })
        assertTrue(result.recommendations.isNotEmpty())
    }

    @Test
    fun forcedMarriageMapsToRelatedCategory() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput(
                categoryId = "forced_marriage",
                categoryLabel = "Forced marriage",
                description = "I am being forced to marry next week"
            )
        )
        assertEquals(AiAnalysisCategory.RELATED, result.category)
    }

    @Test
    fun offlineResultUsesRuleClassifierMetadata() = runBlocking {
        val result = engine.analyze(
            AiAnalysisInput("harassment", "Harassment", "Repeated messages at work")
        )
        assertEquals(OfflineAiAnalysisEngine.PROVIDER_ID, result.providerId)
        assertEquals(OfflineAiAnalysisEngine.MODEL_ID, result.modelId)
        assertFalse(result.consentGranted)
    }
}
