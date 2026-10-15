package com.example.lindabinti.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRiskBoosterTest {

    @Test
    fun boostsDangerToCriticalEmergency() {
        val base = AssistantMessage(
            text = "Ask about FGM or GBV.",
            relatedAction = null,
            source = AssistantSource.Local,
            riskLevel = null
        )
        val out = OfflineRiskBooster.boost("I am in danger help me now", base, "en")
        assertEquals("CRITICAL", out.riskLevel)
        assertEquals("emergency", out.relatedAction)
        assertTrue(out.text.contains("999") || out.text.contains("Emergency"))
    }

    @Test
    fun leavesCalmFaqUnchanged() {
        val base = AssistantMessage(
            text = "FGM is illegal in Kenya.",
            relatedAction = "education",
            source = AssistantSource.Local,
            riskLevel = "LOW"
        )
        val out = OfflineRiskBooster.boost("Is FGM illegal?", base, "en")
        assertEquals("LOW", out.riskLevel)
        assertEquals("education", out.relatedAction)
        assertEquals(base.text, out.text)
    }
}
