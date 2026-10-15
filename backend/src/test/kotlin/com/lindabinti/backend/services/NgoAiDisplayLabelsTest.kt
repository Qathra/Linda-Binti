package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiResultMerger
import com.lindabinti.backend.ai.AiStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgoAiDisplayLabelsTest {

    @Test
    fun sourceLabelsMatchSpec() {
        assertEquals("RULE-BASED AI", NgoAiDisplayLabels.sourceLabel(AiResultMerger.SOURCE_OFFLINE))
        assertEquals("ONLINE AI", NgoAiDisplayLabels.sourceLabel(AiResultMerger.SOURCE_ONLINE))
        assertEquals("HYBRID AI", NgoAiDisplayLabels.sourceLabel(AiResultMerger.SOURCE_HYBRID))
    }

    @Test
    fun caseStatusShowsFallbackWhenRulesUsedWithOnlineConfigured() {
        val label = NgoAiDisplayLabels.caseStatusLabel(
            AiStatus.COMPLETE,
            AiResultMerger.SOURCE_OFFLINE,
            onlineConfigured = true
        )
        assertEquals("FALLBACK", label)
    }

    @Test
    fun caseStatusWorkingForHybridComplete() {
        val label = NgoAiDisplayLabels.caseStatusLabel(
            AiStatus.COMPLETE,
            AiResultMerger.SOURCE_HYBRID,
            onlineConfigured = true
        )
        assertEquals("WORKING", label)
    }

    @Test
    fun engineNotConfiguredWhenOnlineDisabled() {
        assertEquals(
            "NOT CONFIGURED",
            NgoAiDisplayLabels.engineStatusLabel(
                onlineConfigured = false,
                pendingAi = 0,
                rulesFallback = 5,
                failed = 0
            )
        )
    }

    @Test
    fun whyFlaggedUsesIndicatorsNotRawBody() {
        val text = NgoAiDisplayLabels.buildWhyFlaggedExplanation(
            severity = "HIGH",
            scope = "FGM",
            incidentType = "pressure",
            indicators = listOf("Imminent cutting language", "Family coercion"),
            explanation = "Should not appear if indicators present"
        )
        assertTrue(text.contains("Detected indicators"))
        assertFalse(text.contains("survivor narrative"))
    }

    @Test
    fun incidentTypeMapping() {
        assertEquals("pressure", NgoAiDisplayLabels.incidentType("fgm_pressure", ""))
        assertEquals("threat", NgoAiDisplayLabels.incidentType("threats", ""))
        assertEquals("incident", NgoAiDisplayLabels.incidentType("physical", "Physical violence"))
    }
}
