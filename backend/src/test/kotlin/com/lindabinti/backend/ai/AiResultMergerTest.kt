package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResultMergerTest {

    private fun offlineResult(level: String = AiRiskLevel.CRITICAL) = AiRiskResult(
        riskLevel = level,
        gbvFgmRelevance = "FGM",
        incidentCategory = "fgm_pressure",
        urgencyIndicators = listOf("Imminent FGM"),
        safetyConcerns = listOf("Cutting planned"),
        recommendedPriority = level,
        explanation = "Offline rules detected imminent threat.",
        source = AiResultMerger.SOURCE_OFFLINE,
        status = AiStatus.COMPLETE
    )

    private fun onlineResult(level: String = AiRiskLevel.HIGH) = AiRiskResult(
        riskLevel = level,
        gbvFgmRelevance = "FGM",
        incidentCategory = "fgm_pressure",
        urgencyIndicators = listOf("Family pressure"),
        safetyConcerns = listOf("Isolation"),
        recommendedPriority = level,
        explanation = "Online model assessment.",
        source = AiResultMerger.SOURCE_ONLINE,
        status = AiStatus.COMPLETE
    )

    @Test
    fun mergeNgoPrefersHigherSeverityWithOnlineExplanation() {
        val merged = AiResultMerger.mergeNgo(offlineResult(), onlineResult())
        assertEquals(AiRiskLevel.CRITICAL, merged.riskLevel)
        assertEquals(AiResultMerger.SOURCE_HYBRID, merged.source)
        assertTrue(merged.explanation.contains("Online model"))
        assertTrue(merged.explanation.contains("RULE-BASED AI"))
    }

    @Test
    fun mergeNgoUsesOfflineWhenOnlineFails() {
        val failed = onlineResult().copy(status = AiStatus.FAILED)
        val merged = AiResultMerger.mergeNgo(offlineResult(), failed)
        assertEquals(AiResultMerger.SOURCE_OFFLINE, merged.source)
        assertEquals(AiStatus.FAILED, merged.status)
    }

    @Test
    fun mergeSurvivorOnlineKeepsHigherOfflineSeverity() {
        val offline = offlineResult(AiRiskLevel.CRITICAL)
        val online = OnlineReportAnalysisResult(
            analysisId = "on-1",
            source = "ONLINE AI",
            category = "FGM",
            severity = "HIGH",
            confidence = 0.9,
            indicators = listOf("Pressure"),
            explanation = "Cloud analysis.",
            recommendations = listOf("Reach safety"),
            educationRecommendations = listOf("fgm"),
            timestamp = 1000L,
            providerId = "openai",
            modelId = "gpt-4",
            processingDurationMs = 50L
        )
        val merged = AiResultMerger.mergeSurvivorOnline(offline, online)
        assertEquals("CRITICAL", merged.severity)
        assertEquals("ONLINE AI", merged.source)
        assertTrue(merged.explanation.contains("RULE-BASED AI"))
    }
}
