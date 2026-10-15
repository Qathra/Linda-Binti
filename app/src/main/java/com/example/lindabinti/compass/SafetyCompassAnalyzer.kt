package com.example.lindabinti.compass

import com.example.lindabinti.ai.AiAnalysisInput
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiAnalysisStorage
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.ai.PrivacyFilter
import com.example.lindabinti.data.content.RiskQuestion
import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.RiskAssessment

/**
 * Resolves truthful OFFLINE / ONLINE / HYBRID labels for Safety Compass.
 * Never returns ONLINE unless a pure online result exists (no offline merge).
 */
object CompassSourceLabel {
    const val OFFLINE = "OFFLINE AI"
    const val ONLINE = "ONLINE AI"
    const val HYBRID = "HYBRID AI"

    fun resolve(results: List<AiAnalysisResult>): String {
        if (results.isEmpty()) return OFFLINE
        val hasHybrid = results.any { it.source == AiAnalysisSource.HYBRID }
        val hasPureOnline = results.any { it.source == AiAnalysisSource.ONLINE }
        val hasOffline = results.any { it.source == AiAnalysisSource.OFFLINE }
        return when {
            hasHybrid || (hasPureOnline && hasOffline) -> HYBRID
            hasPureOnline -> ONLINE
            else -> OFFLINE
        }
    }
}

/**
 * Builds Safety Compass snapshots through the unified [AiOrchestrator].
 */
object SafetyCompassAnalyzer {

    suspend fun buildSnapshot(
        orchestrator: AiOrchestrator,
        language: String,
        latestRisk: RiskAssessment?,
        riskQuestions: List<RiskQuestion>,
        reports: List<Report>,
        completedEducationIds: Set<String>,
        trustedContactCount: Int
    ): SafetyCompassSnapshot {
        val insights = reports.take(8).map { report ->
            val analysis = resolveReportAnalysis(orchestrator, report)
            SafetyCompassReportInsight(
                categoryId = report.categoryId,
                categoryLabel = report.category,
                createdAt = report.createdAt,
                analysis = analysis
            )
        }
        val sourceLabel = CompassSourceLabel.resolve(insights.mapNotNull { it.analysis })
        return SafetyCompassBuilder.build(
            SafetyCompassBuildInput(
                language = language,
                latestRisk = latestRisk,
                riskQuestions = riskQuestions,
                recentReports = insights,
                completedEducationIds = completedEducationIds,
                trustedContactCount = trustedContactCount,
                aiSourceLabel = sourceLabel
            )
        )
    }

    internal suspend fun resolveReportAnalysis(
        orchestrator: AiOrchestrator,
        report: Report
    ): AiAnalysisResult {
        val cached = AiAnalysisStorage.fromJson(report.aiAnalysisJson)
        val shouldRefreshForOnline = cached != null &&
            cached.source == AiAnalysisSource.OFFLINE &&
            orchestrator.canUseOnlineAnalysis()
        if (cached != null && !shouldRefreshForOnline) {
            return cached
        }
        return orchestrator.analyze(compassInputFor(report))
    }

    internal fun compassInputFor(report: Report): AiAnalysisInput {
        val redacted = PrivacyFilter.redactForCloudAnalysis(report.body)
        return AiAnalysisInput(
            categoryId = report.categoryId,
            categoryLabel = report.category,
            description = redacted,
            includeDescription = redacted.isNotBlank()
        )
    }
}
