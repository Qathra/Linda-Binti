package com.example.lindabinti.compass

import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.data.local.RiskAssessment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCompassBuilderTest {

    @Test
    fun mergesRiskBandWithReportSeverity() {
        val risk = RiskAssessment(
            id = 1L,
            answersJson = "q1=true;q2=false;q3=false;q4=true;q5=false",
            score = 1,
            band = "lower",
            createdAt = System.currentTimeMillis()
        )
        val report = SafetyCompassReportInsight(
            categoryId = "fgm_pressure",
            categoryLabel = "FGM pressure",
            createdAt = System.currentTimeMillis(),
            analysis = sampleAnalysis(AiSeverity.HIGH, AiAnalysisCategory.FGM_THREAT)
        )
        val snapshot = SafetyCompassBuilder.build(
            SafetyCompassBuildInput(
                language = "en",
                latestRisk = risk,
                riskQuestions = LocalContent.riskQuestions("en"),
                recentReports = listOf(report),
                completedEducationIds = emptySet(),
                trustedContactCount = 0,
                aiSourceLabel = "OFFLINE AI"
            )
        )
        assertEquals(SafetyCompassStatus.HIGH, snapshot.status)
        assertTrue(snapshot.safetyFactors.any { it.contains("someone you can call", ignoreCase = true) })
    }

    @Test
    fun criticalRiskBandDominates() {
        val risk = RiskAssessment(
            id = 1L,
            answersJson = "q1=true;q2=true;q3=true;q4=false;q5=true",
            score = 10,
            band = "critical",
            createdAt = System.currentTimeMillis()
        )
        val snapshot = SafetyCompassBuilder.build(
            SafetyCompassBuildInput(
                language = "en",
                latestRisk = risk,
                riskQuestions = LocalContent.riskQuestions("en"),
                recentReports = emptyList(),
                completedEducationIds = setOf("gbv"),
                trustedContactCount = 2,
                aiSourceLabel = "OFFLINE AI"
            )
        )
        assertEquals(SafetyCompassStatus.CRITICAL, snapshot.status)
        assertTrue(snapshot.riskIndicators.isNotEmpty())
        assertTrue(snapshot.trustedContactCount == 2)
        assertTrue(snapshot.showTrustedContacts)
    }

    @Test
    fun emptyDataShowsGetStartedPath() {
        val snapshot = SafetyCompassBuilder.build(
            SafetyCompassBuildInput(
                language = "en",
                latestRisk = null,
                riskQuestions = LocalContent.riskQuestions("en"),
                recentReports = emptyList(),
                completedEducationIds = emptySet(),
                trustedContactCount = 0,
                aiSourceLabel = "OFFLINE AI"
            )
        )
        assertFalse(snapshot.hasAnyData)
        assertEquals(SafetyCompassStatus.LOWER, snapshot.status)
        assertTrue(snapshot.disclaimer.contains("not a", ignoreCase = true))
    }

    @Test
    fun educationMappingResolvesAliases() {
        assertEquals("gbv", SafetyCompassBuilder.mapEducationTopicId("gbv_basics"))
        assertEquals("immediate_safety", SafetyCompassBuilder.mapEducationTopicId("safety_planning"))
        assertEquals("fgm", SafetyCompassBuilder.mapEducationTopicId("fgm"))
    }

    @Test
    fun neverUsesCertaintyLanguageInConfidenceNote() {
        val snapshot = SafetyCompassBuilder.build(
            SafetyCompassBuildInput(
                language = "en",
                latestRisk = null,
                riskQuestions = LocalContent.riskQuestions("en"),
                recentReports = emptyList(),
                completedEducationIds = emptySet(),
                trustedContactCount = 0,
                aiSourceLabel = "OFFLINE AI"
            )
        )
        assertFalse(snapshot.statusConfidenceNote.contains("diagnosis", ignoreCase = true))
        assertFalse(snapshot.statusConfidenceNote.contains("certain", ignoreCase = true))
    }

    private fun sampleAnalysis(severity: AiSeverity, category: AiAnalysisCategory) =
        AiAnalysisResult(
            analysisId = "test",
            source = AiAnalysisSource.OFFLINE,
            category = category,
            severity = severity,
            confidence = 0.7,
            indicators = listOf("Imminent or planned FGM"),
            explanation = "Offline guidance",
            recommendations = listOf("Reach a safe place if possible."),
            educationRecommendations = listOf("fgm"),
            timestamp = System.currentTimeMillis()
        )
}
