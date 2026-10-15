package com.example.lindabinti.domain

import com.example.lindabinti.data.content.RiskQuestion

data class RiskResult(val score: Int, val band: String)

/**
 * Local safety-awareness scoring — not a clinical or legal diagnosis.
 * Bands: lower | elevated | high | critical
 */
object RiskScorer {

    fun evaluate(answers: Map<String, Boolean>, questions: List<RiskQuestion>): RiskResult? {
        if (answers.size < questions.size) return null
        var score = 0
        questions.forEach { q ->
            if (answers[q.id] == true) score += q.weightIfYes
        }
        var band = when {
            score >= 6 -> "critical"
            score >= 4 -> "high"
            score >= 2 -> "elevated"
            else -> "lower"
        }
        // Imminent home danger → always Critical
        if (answers["q5"] == true) band = escalate(band, "critical")
        // FGM / forced marriage / leave school pressure → at least High
        if (answers["q3"] == true) band = escalate(band, "high")
        return RiskResult(score, band)
    }

    private fun escalate(current: String, floor: String): String {
        val order = listOf("lower", "elevated", "high", "critical")
        val c = order.indexOf(current).coerceAtLeast(0)
        val f = order.indexOf(floor).coerceAtLeast(0)
        return order[maxOf(c, f)]
    }
}
