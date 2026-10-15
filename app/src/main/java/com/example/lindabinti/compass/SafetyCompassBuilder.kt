package com.example.lindabinti.compass

import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.data.content.EducationCatalog
import com.example.lindabinti.data.content.RiskQuestion
import com.example.lindabinti.data.local.RiskAssessment

data class SafetyCompassReportInsight(
    val categoryId: String,
    val categoryLabel: String,
    val createdAt: Long,
    val analysis: AiAnalysisResult?
)

data class SafetyCompassBuildInput(
    val language: String,
    val latestRisk: RiskAssessment?,
    val riskQuestions: List<RiskQuestion>,
    val recentReports: List<SafetyCompassReportInsight>,
    val completedEducationIds: Set<String>,
    val trustedContactCount: Int,
    val aiSourceLabel: String
)

/**
 * Builds a local-only safety overview from information the survivor chose to provide.
 * Not a diagnosis — never claims certainty.
 */
object SafetyCompassBuilder {

    private const val RECENT_REPORT_WINDOW_MS = 90L * 24 * 60 * 60 * 1000

    fun build(input: SafetyCompassBuildInput): SafetyCompassSnapshot {
        val now = System.currentTimeMillis()
        val recent = input.recentReports.filter { now - it.createdAt <= RECENT_REPORT_WINDOW_MS }
        val analyses = recent.mapNotNull { it.analysis }

        var status = input.latestRisk?.band?.let { SafetyCompassStatus.fromRiskBand(it) }
            ?: SafetyCompassStatus.LOWER
        analyses.forEach { status = SafetyCompassStatus.max(status, it.severity.toCompassStatus()) }

        val riskIndicators = buildRiskIndicators(input.latestRisk, input.riskQuestions)
        val gbvConcerns = buildGbvFgmConcerns(recent, analyses)
        val safetyFactors = buildSafetyFactors(
            input.latestRisk,
            input.riskQuestions,
            input.completedEducationIds,
            input.trustedContactCount
        )
        val nextSteps = buildNextSteps(status, analyses, input.completedEducationIds.isEmpty())
        val educationItems = buildEducationItems(input.language, analyses, input.completedEducationIds)

        val hasData = input.latestRisk != null ||
            recent.isNotEmpty() ||
            input.completedEducationIds.isNotEmpty() ||
            input.trustedContactCount > 0

        return SafetyCompassSnapshot(
            status = status,
            statusConfidenceNote = confidenceNote(status, hasData, analyses.size),
            aiSourceLabel = input.aiSourceLabel,
            generatedAt = now,
            reassurance = "You are not alone.",
            disclaimer = "This guidance is based on the information you chose to provide. " +
                "It is a safety-oriented overview, not a medical, legal, or psychological diagnosis.",
            riskIndicators = riskIndicators.take(6),
            gbvFgmConcerns = gbvConcerns.take(6),
            safetyFactors = safetyFactors.take(6),
            nextSteps = nextSteps.take(6),
            educationItems = educationItems.take(4),
            trustedContactCount = input.trustedContactCount,
            showTrustedContacts = input.trustedContactCount > 0,
            showEmergency = true,
            showSupportServices = true,
            hasAnyData = hasData
        )
    }

    private fun confidenceNote(
        status: SafetyCompassStatus,
        hasData: Boolean,
        analysisCount: Int
    ): String = when {
        !hasData ->
            "We do not have enough local information yet. Complete a safety check or save a report to personalize this view."
        analysisCount == 0 && status == SafetyCompassStatus.LOWER ->
            "Based on your latest safety check. This is guidance, not certainty."
        status.ordinal >= SafetyCompassStatus.HIGH.ordinal ->
            "Patterns you shared may suggest elevated concern. Consider the steps below — you can change your situation."
        else ->
            "Based on local safety checks and reports you saved on this device."
    }

    private fun buildRiskIndicators(
        latestRisk: RiskAssessment?,
        questions: List<RiskQuestion>
    ): List<String> {
        if (latestRisk == null) return emptyList()
        val answers = parseAnswers(latestRisk.answersJson)
        return questions.mapNotNull { q ->
            if (answers[q.id] == true && q.weightIfYes > 0) {
                "Safety check: ${q.text}"
            } else null
        }
    }

    private fun buildGbvFgmConcerns(
        reports: List<SafetyCompassReportInsight>,
        analyses: List<AiAnalysisResult>
    ): List<String> {
        val out = linkedSetOf<String>()
        reports.forEach { r ->
            val label = r.categoryLabel.ifBlank { r.categoryId.replace('_', ' ') }
            out += "Report category noted: $label"
        }
        analyses.forEach { a ->
            when (a.category) {
                AiAnalysisCategory.FGM, AiAnalysisCategory.FGM_THREAT ->
                    out += "FGM-related concern may be present based on information you shared."
                AiAnalysisCategory.GBV ->
                    out += "GBV-related concern may be present based on information you shared."
                AiAnalysisCategory.RELATED ->
                    out += "Protection concern related to GBV/FGM may be present."
                AiAnalysisCategory.UNKNOWN -> Unit
            }
            a.indicators.forEach { out += it }
        }
        return out.toList()
    }

    private fun buildSafetyFactors(
        latestRisk: RiskAssessment?,
        questions: List<RiskQuestion>,
        completedEducation: Set<String>,
        contactCount: Int
    ): List<String> {
        val out = mutableListOf<String>()
        if (latestRisk != null) {
            val answers = parseAnswers(latestRisk.answersJson)
            val safeContactQ = questions.find { it.id == "q4" }
            if (safeContactQ != null && answers["q4"] == true) {
                out += "You indicated you have someone you can call safely."
            }
        }
        if (contactCount > 0) {
            out += "You saved $contactCount trusted contact${if (contactCount == 1) "" else "s"} on this device."
        }
        if (completedEducation.isNotEmpty()) {
            out += "You have started ${completedEducation.size} education lesson${if (completedEducation.size == 1) "" else "s"} — learning is a protective step."
        }
        if (out.isEmpty()) {
            out += "Your safety matters. Small steps — like saving a trusted contact — can help."
        }
        return out
    }

    private fun buildNextSteps(
        status: SafetyCompassStatus,
        analyses: List<AiAnalysisResult>,
        educationEmpty: Boolean
    ): List<String> {
        val out = linkedSetOf<String>()
        analyses.flatMap { it.recommendations }.forEach { out += it }
        when (status) {
            SafetyCompassStatus.CRITICAL, SafetyCompassStatus.HIGH -> {
                out += "If you feel unsafe right now, Emergency Assistance is available when you choose to use it."
                out += "Consider reaching someone you trust or a support hotline."
            }
            SafetyCompassStatus.ELEVATED -> {
                out += "Review your safety plan and trusted contacts."
                out += "Education lessons can help you prepare next steps at your own pace."
            }
            SafetyCompassStatus.LOWER -> {
                out += "Keep learning in the Education section — knowledge supports safety."
            }
        }
        if (educationEmpty) {
            out += "Explore one education lesson that feels relevant to you."
        }
        out += "You are not alone."
        return out.toList()
    }

    private fun buildEducationItems(
        language: String,
        analyses: List<AiAnalysisResult>,
        completed: Set<String>
    ): List<SafetyCompassEducationItem> {
        val topics = EducationCatalog.topics(language)
        val topicById = topics.associateBy { it.id }
        val recommendedIds = linkedSetOf<String>()
        analyses.flatMap { it.educationRecommendations }.forEach { raw ->
            mapEducationTopicId(raw)?.let { recommendedIds += it }
        }
        if (recommendedIds.isEmpty()) {
            recommendedIds += listOf("immediate_safety", "gbv", "seeking_help")
        }
        val incomplete = recommendedIds.filter { it !in completed }
        val fallback = topics.map { it.id }.filter { it !in completed }
        return (incomplete + fallback)
            .distinct()
            .mapNotNull { id ->
                topicById[id]?.let { SafetyCompassEducationItem(id, it.title) }
            }
    }

    fun mapEducationTopicId(raw: String): String? = when (raw.lowercase()) {
        "fgm" -> "fgm"
        "gbv", "gbv_basics" -> "gbv"
        "warning_signs" -> "warning_signs"
        "prevention", "child_protection" -> "prevention"
        "immediate_safety", "safety_planning" -> "immediate_safety"
        "reporting_evidence" -> "reporting_evidence"
        "seeking_help" -> "seeking_help"
        "dignity", "know_rights" -> "dignity"
        "forced_marriage" -> "prevention"
        else -> raw.takeIf { EducationCatalog.topicById("en", it) != null }
    }

    private fun parseAnswers(raw: String): Map<String, Boolean> =
        raw.split(";")
            .mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0] to (kv[1] == "true") else null
            }
            .toMap()
}
