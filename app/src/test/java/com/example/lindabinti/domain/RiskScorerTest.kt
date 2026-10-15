package com.example.lindabinti.domain

import com.example.lindabinti.data.content.RiskQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RiskScorerTest {

    private val questions = listOf(
        RiskQuestion("q1", "One", weightIfYes = 2),
        RiskQuestion("q2", "Two", weightIfYes = 2),
        RiskQuestion("q3", "Three", weightIfYes = 3),
        RiskQuestion("q4", "Protective", weightIfYes = -1),
        RiskQuestion("q5", "Home unsafe", weightIfYes = 3)
    )

    @Test
    fun incompleteReturnsNull() {
        assertNull(RiskScorer.evaluate(mapOf("q1" to true), questions))
    }

    @Test
    fun lowerBand() {
        val result = RiskScorer.evaluate(
            mapOf(
                "q1" to false, "q2" to false, "q3" to false,
                "q4" to true, "q5" to false
            ),
            questions
        )!!
        assertEquals(-1, result.score)
        assertEquals("lower", result.band)
    }

    @Test
    fun elevatedBand() {
        val result = RiskScorer.evaluate(
            mapOf(
                "q1" to true, "q2" to false, "q3" to false,
                "q4" to false, "q5" to false
            ),
            questions
        )!!
        assertEquals(2, result.score)
        assertEquals("elevated", result.band)
    }

    @Test
    fun fgmPressureAtLeastHigh() {
        val result = RiskScorer.evaluate(
            mapOf(
                "q1" to false, "q2" to false, "q3" to true,
                "q4" to false, "q5" to false
            ),
            questions
        )!!
        assertEquals(3, result.score)
        assertEquals("high", result.band)
    }

    @Test
    fun unsafeNowIsCritical() {
        val result = RiskScorer.evaluate(
            mapOf(
                "q1" to false, "q2" to false, "q3" to false,
                "q4" to false, "q5" to true
            ),
            questions
        )!!
        assertEquals(3, result.score)
        assertEquals("critical", result.band)
    }

    @Test
    fun criticalBand() {
        val result = RiskScorer.evaluate(
            mapOf(
                "q1" to true, "q2" to true, "q3" to true,
                "q4" to false, "q5" to true
            ),
            questions
        )!!
        assertEquals(10, result.score)
        assertEquals("critical", result.band)
    }
}
