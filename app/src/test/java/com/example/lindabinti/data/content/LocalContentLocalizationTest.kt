package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalContentLocalizationTest {

    @Test
    fun kiswahiliHotlinesAndRiskMatchEnglishIds() {
        val enHotlines = LocalContent.hotlines("en")
        val swHotlines = LocalContent.hotlines("sw")
        assertEquals(enHotlines.map { it.number }, swHotlines.map { it.number })
        swHotlines.forEach { assertTrue(it.name.isNotBlank()) }

        val enRisk = LocalContent.riskQuestions("en")
        val swRisk = LocalContent.riskQuestions("sw")
        assertEquals(enRisk.map { it.id }, swRisk.map { it.id })
        assertEquals(enRisk.map { it.weightIfYes }, swRisk.map { it.weightIfYes })
        swRisk.forEach { assertTrue(it.text.isNotBlank()) }
    }

    @Test
    fun maaUsesMaasaiCatalog() {
        assertTrue(SurvivorSupportCatalog.coreMessage("maa").isNotBlank())
        assertNotEquals(
            SurvivorSupportCatalog.coreMessage("en"),
            SurvivorSupportCatalog.coreMessage("maa")
        )
        assertEquals(
            LocalContent.hotlines("en").map { it.number },
            LocalContent.hotlines("maa").map { it.number }
        )
        assertEquals(
            LocalContent.riskQuestions("en").map { it.id },
            LocalContent.riskQuestions("maa").map { it.id }
        )
        assertTrue(LocalContent.riskQuestions("maa").all { it.text.isNotBlank() })
    }
}
