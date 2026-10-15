package com.lindabinti.backend

import com.lindabinti.backend.ai.AiRiskLevel
import com.lindabinti.backend.ai.AiStatus
import com.lindabinti.backend.services.NgoFlagIntelligence
import com.lindabinti.backend.services.ReportFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NgoFlagIntelligenceTest {

    @Test
    fun estimateConfidenceHigherForCriticalAiComplete() {
        val flag = sampleFlag(urgency = AiRiskLevel.CRITICAL, aiStatus = AiStatus.COMPLETE, source = "ai")
        val confidence = NgoFlagIntelligence.estimateConfidence(flag)
        assertTrue(confidence >= 0.9)
    }

    @Test
    fun estimateConfidenceLowerForRulesFallback() {
        val flag = sampleFlag(urgency = AiRiskLevel.HIGH, aiStatus = AiStatus.COMPLETE, source = "rules")
        val confidence = NgoFlagIntelligence.estimateConfidence(flag)
        assertTrue(confidence < 0.85)
    }

    @Test
    fun parseIndicatorsSplitsAndDedupes() {
        val flag = sampleFlag(
            urgencyIndicators = "imminent cut; family pressure",
            safetyConcerns = "family pressure, isolation"
        )
        val indicators = NgoFlagIntelligence.parseIndicators(flag)
        assertEquals(3, indicators.size)
        assertTrue(indicators.contains("imminent cut"))
    }

    private fun sampleFlag(
        urgency: String = AiRiskLevel.MEDIUM,
        aiStatus: String = AiStatus.COMPLETE,
        source: String = "ai",
        urgencyIndicators: String = "",
        safetyConcerns: String = ""
    ) = ReportFlag(
        serverReportId = "id-1",
        categoryId = "fgm_pressure",
        categoryLabel = "FGM threat",
        createdAtEpochMs = 1710000000000L,
        weekLabel = "2024-W10",
        urgency = urgency,
        explanation = "Test explanation",
        classifierSource = source,
        disclaimer = "Aid only",
        aiStatus = aiStatus,
        gbvFgmRelevance = "FGM",
        incidentCategory = "FGM threat",
        urgencyIndicators = urgencyIndicators,
        safetyConcerns = safetyConcerns,
        referenceId = "LB-TEST1234"
    )
}
