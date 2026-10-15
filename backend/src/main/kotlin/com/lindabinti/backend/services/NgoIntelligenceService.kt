package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiRiskLevel
import com.lindabinti.backend.ai.AiStatus

/**
 * Privacy-preserving NGO intelligence layer over anonymized case flags.
 * Never exposes survivor identity, report bodies, or exact coordinates.
 */
class NgoIntelligenceService(
    private val reportService: ReportService,
    private val analyticsService: AnalyticsService,
    private val aiOrchestration: AiOrchestrationService,
    private val audit: AuditService
) {
    fun intelligenceForNgo(actorUserId: String, periodDays: Int = 30): NgoIntelligenceDashboard {
        val period = periodDays.coerceIn(7, 365)
        audit.log(actorUserId, "NGO_INTELLIGENCE_READ", "periodDays=$period")
        val allFlags = reportService.listFlagsForNgo(actorUserId, limit = 500)
        val cutoff = System.currentTimeMillis() - period.toLong() * 86_400_000L
        val flags = allFlags.filter { it.createdAtEpochMs >= cutoff }
        val analytics = analyticsService.summaryForNgo(actorUserId)
        val trends = reportService.trendsForNgo(actorUserId, analytics)

        val totalFgm = flags.count { scopeFor(it) == "FGM" }
        val totalGbv = flags.size - totalFgm
        val byLevel = flags.groupingBy { AiRiskLevel.parse(it.urgency) ?: AiRiskLevel.MEDIUM }.eachCount()
        val critical = byLevel[AiRiskLevel.CRITICAL] ?: 0
        val high = byLevel[AiRiskLevel.HIGH] ?: 0
        val recentCutoff = System.currentTimeMillis() - 7L * 86_400_000L
        val recent = flags.count { it.createdAtEpochMs >= recentCutoff }
        val aiFlagged = flags.count {
            it.aiStatus.equals(AiStatus.COMPLETE, ignoreCase = true) ||
                it.classifierSource == "ai"
        }
        val pendingAi = flags.count { it.aiStatus.equals(AiStatus.PENDING, ignoreCase = true) }
        val openCriticalHigh = flags.count { flag ->
            val level = AiRiskLevel.parse(flag.urgency)
            CaseStatus.isOpen(flag.caseStatus) &&
                (level == AiRiskLevel.CRITICAL || level == AiRiskLevel.HIGH)
        }

        val priorityQueue = flags
            .sortedWith(
                compareBy<ReportFlag> { AiRiskLevel.sortRank(it.urgency) }
                    .thenByDescending { it.createdAtEpochMs }
            )
            .take(25)
            .map { flag -> toPriorityItem(flag, aiOrchestration.isSurvivorOnlineAvailable()) }

        val caseFlags = flags
            .sortedWith(
                compareBy<ReportFlag> { AiRiskLevel.sortRank(it.urgency) }
                    .thenByDescending { it.createdAtEpochMs }
            )
            .map { flag -> toPriorityItem(flag, aiOrchestration.isSurvivorOnlineAvailable()) }

        val priorityCases = priorityQueue.filter {
            val level = AiRiskLevel.parse(it.severity)
            level == AiRiskLevel.CRITICAL || level == AiRiskLevel.HIGH
        }

        val severityDistribution = listOf(
            NamedCount("CRITICAL", critical),
            NamedCount("HIGH", high),
            NamedCount("MEDIUM", byLevel[AiRiskLevel.MEDIUM] ?: 0),
            NamedCount("LOW", byLevel[AiRiskLevel.LOW] ?: 0)
        )

        val severityDisplay = listOf(
            SeverityBucket("LOW", "Lower", byLevel[AiRiskLevel.LOW] ?: 0),
            SeverityBucket("MEDIUM", "Elevated", byLevel[AiRiskLevel.MEDIUM] ?: 0),
            SeverityBucket("HIGH", "High", high),
            SeverityBucket("CRITICAL", "Critical", critical)
        )

        val gbvVsFgm = listOf(
            NamedCount("GBV", totalGbv),
            NamedCount("FGM", totalFgm)
        )

        val minThreshold = analytics.minThreshold
        val trendInsights = buildTrendInsights(flags, totalGbv, totalFgm, minThreshold, period)
        val interventionInsights = buildInterventionInsights(flags, totalGbv, totalFgm, minThreshold, trends)
        val insightOfPeriod = buildInsightOfPeriod(
            flags, trendInsights, critical, high, totalGbv, totalFgm, minThreshold, period
        )

        val visibleGeo = trends.geoAreas.count { !it.suppressed && it.areaCode != "suppressed" }
        val mapGeoAreas = reportService.buildGeoAreasForFlags(flags, minThreshold = 1)
        val geoInsufficient = visibleGeo == 0

        val trendComparison = buildTrendComparison(allFlags, flags, cutoff, period, minThreshold)

        return NgoIntelligenceDashboard(
            dashboardTitle = DASHBOARD_TITLE,
            aiSafetySectionTitle = AI_SAFETY_SECTION_TITLE,
            periodDays = period,
            generatedAtEpochMs = System.currentTimeMillis(),
            aiEngine = aiEngineStatus(flags, pendingAi),
            commandSummary = AiCommandSummary(
                totalGbvCases = totalGbv,
                totalFgmCases = totalFgm,
                highPriorityCases = critical + high,
                criticalCases = critical,
                recentIncidents = recent,
                aiFlaggedCases = aiFlagged,
                pendingAi = pendingAi,
                openCriticalHigh = openCriticalHigh,
                totalReports = flags.size
            ),
            priorityQueue = priorityQueue,
            priorityCases = priorityCases,
            caseFlags = caseFlags,
            severityDistribution = severityDistribution,
            severityDisplay = severityDisplay,
            gbvVsFgm = gbvVsFgm,
            trendInsights = trendInsights,
            interventionInsights = interventionInsights,
            insightOfPeriod = insightOfPeriod,
            trendComparison = trendComparison,
            trends = filterTrendsToPeriod(trends, flags, minThreshold),
            mapGeoAreas = mapGeoAreas,
            geoInsufficient = geoInsufficient,
            privacyNote = ReportService.LOCATION_NOTE +
                " Survivor names, emails, phones, PINs, SOS codes, and report bodies are never shown.",
            privacyIndicator = PRIVACY_INDICATOR,
            aiDisclaimer = AI_DISCLAIMER,
            minThreshold = minThreshold
        )
    }

    private fun toPriorityItem(flag: ReportFlag, onlineConfigured: Boolean): AiPriorityQueueItem {
        val confidence = NgoFlagIntelligence.estimateConfidence(flag)
        val indicators = NgoFlagIntelligence.parseIndicators(flag)
        val severity = NgoFlagIntelligence.severityLabel(flag.urgency)
        val scope = scopeFor(flag)
        val incidentType = NgoAiDisplayLabels.incidentType(flag.categoryId, flag.incidentCategory)
        val sourceLabel = NgoAiDisplayLabels.sourceLabel(flag.classifierSource)
        val aiStatusLabel = NgoAiDisplayLabels.caseStatusLabel(
            flag.aiStatus,
            flag.classifierSource,
            onlineConfigured
        )
        return AiPriorityQueueItem(
            referenceId = flag.referenceId,
            serverReportId = flag.serverReportId,
            category = NgoFlagIntelligence.categoryDisplay(flag),
            categoryId = flag.categoryId,
            scope = scope,
            severity = severity,
            urgency = severity,
            incidentType = incidentType,
            confidence = confidence,
            confidencePercent = (confidence * 100).toInt(),
            indicators = indicators,
            explanation = flag.explanation,
            whyFlagged = NgoAiDisplayLabels.buildWhyFlaggedExplanation(
                severity, scope, incidentType, indicators, flag.explanation
            ),
            recommendedPriority = flag.recommendedPriority.ifBlank { severity },
            createdAtEpochMs = flag.createdAtEpochMs,
            caseStatus = flag.caseStatus,
            aiStatus = flag.aiStatus,
            aiStatusLabel = aiStatusLabel,
            classifierSource = flag.classifierSource,
            sourceLabel = sourceLabel,
            gbvFgmRelevance = flag.gbvFgmRelevance,
            areaLabel = flag.areaLabel.ifBlank { ReportService.SERVICE_AREA },
            areaCode = flag.areaCode
        )
    }

    private fun aiEngineStatus(flags: List<ReportFlag>, pendingAi: Int): AiEngineStatusDto {
        val health = aiOrchestration.resolveEngineHealth(flags, pendingAi)
        val rulesFallback = flags.count { it.classifierSource == com.lindabinti.backend.ai.AiResultMerger.SOURCE_OFFLINE }
        val failed = flags.count {
            it.aiStatus.equals(AiStatus.FAILED, ignoreCase = true) ||
                it.aiStatus.equals(AiStatus.TIMEOUT, ignoreCase = true) ||
                it.aiStatus.equals(AiStatus.INVALID, ignoreCase = true)
        }
        val statusLabel = NgoAiDisplayLabels.engineStatusLabel(
            health.onlineConfigured,
            pendingAi,
            rulesFallback,
            failed
        )
        val activeSourceLabel = when (statusLabel) {
            NgoAiDisplayLabels.STATUS_NOT_CONFIGURED -> NgoAiDisplayLabels.SOURCE_RULE_BASED
            NgoAiDisplayLabels.STATUS_WORKING -> {
                val hasHybrid = flags.any {
                    it.classifierSource == com.lindabinti.backend.ai.AiResultMerger.SOURCE_HYBRID
                }
                val hasOnline = flags.any {
                    it.classifierSource == com.lindabinti.backend.ai.AiResultMerger.SOURCE_ONLINE
                }
                when {
                    hasHybrid -> NgoAiDisplayLabels.SOURCE_HYBRID
                    hasOnline -> NgoAiDisplayLabels.SOURCE_ONLINE
                    else -> NgoAiDisplayLabels.SOURCE_RULE_BASED
                }
            }
            else -> NgoAiDisplayLabels.SOURCE_RULE_BASED
        }
        return AiEngineStatusDto(
            mode = health.health,
            label = statusLabel,
            health = health.health,
            onlineConfigured = health.onlineConfigured,
            degradedReason = health.degradedReason,
            statusLabel = statusLabel,
            activeSourceLabel = activeSourceLabel
        )
    }

    private fun buildTrendComparison(
        allFlags: List<ReportFlag>,
        currentFlags: List<ReportFlag>,
        currentCutoff: Long,
        periodDays: Int,
        minThreshold: Int
    ): AiTrendComparison {
        val periodMs = periodDays.toLong() * 86_400_000L
        val previousCutoff = currentCutoff - periodMs
        val previousFlags = allFlags.filter {
            it.createdAtEpochMs >= previousCutoff && it.createdAtEpochMs < currentCutoff
        }
        if (currentFlags.size < minThreshold && previousFlags.size < minThreshold) {
            return AiTrendComparison(
                currentPeriodDays = periodDays,
                previousPeriodDays = periodDays,
                currentTotal = currentFlags.size,
                previousTotal = previousFlags.size,
                totalChangePercent = null,
                currentGbv = currentFlags.count { scopeFor(it) == "GBV" },
                previousGbv = previousFlags.count { scopeFor(it) == "GBV" },
                currentFgm = currentFlags.count { scopeFor(it) == "FGM" },
                previousFgm = previousFlags.count { scopeFor(it) == "FGM" },
                currentCritical = 0,
                previousCritical = 0,
                emergingCategories = emptyList(),
                highRiskAreas = emptyList(),
                note = "Insufficient aggregated data for period comparison (minimum threshold: $minThreshold)."
            )
        }

        val currentGbv = currentFlags.count { scopeFor(it) == "GBV" }
        val currentFgm = currentFlags.count { scopeFor(it) == "FGM" }
        val previousGbv = previousFlags.count { scopeFor(it) == "GBV" }
        val previousFgm = previousFlags.count { scopeFor(it) == "FGM" }
        val currentCritical = currentFlags.count {
            AiRiskLevel.parse(it.urgency) == AiRiskLevel.CRITICAL
        }
        val previousCritical = previousFlags.count {
            AiRiskLevel.parse(it.urgency) == AiRiskLevel.CRITICAL
        }

        val totalChangePercent = if (previousFlags.isNotEmpty()) {
            ((currentFlags.size - previousFlags.size) * 100) / previousFlags.size
        } else {
            null
        }

        val currentCategories = currentFlags.groupingBy { it.categoryId }.eachCount()
        val previousCategories = previousFlags.groupingBy { it.categoryId }.eachCount()
        val emerging = currentCategories
            .filter { (id, count) ->
                count >= minThreshold && (previousCategories[id] ?: 0) < count
            }
            .map { NamedCount(it.key, it.value - (previousCategories[it.key] ?: 0)) }
            .sortedByDescending { it.count }
            .take(5)

        val highRiskAreas = reportService.buildGeoAreasForFlags(currentFlags, minThreshold = 1)
            .filter {
                !it.suppressed &&
                    it.areaCode != "suppressed" &&
                    it.hotspotLevel == AreaHotspotAssigner.HIGH
            }
            .sortedByDescending { it.totalCount }
            .take(5)
            .map { NamedCount(it.areaLabel, it.totalCount) }

        val note = "Observed period-over-period changes from anonymized case counts. " +
            "Not predictive forecasting."

        return AiTrendComparison(
            currentPeriodDays = periodDays,
            previousPeriodDays = periodDays,
            currentTotal = currentFlags.size,
            previousTotal = previousFlags.size,
            totalChangePercent = totalChangePercent,
            currentGbv = currentGbv,
            previousGbv = previousGbv,
            currentFgm = currentFgm,
            previousFgm = previousFgm,
            currentCritical = currentCritical,
            previousCritical = previousCritical,
            emergingCategories = emerging,
            highRiskAreas = highRiskAreas,
            note = note
        )
    }

    private fun buildInsightOfPeriod(
        flags: List<ReportFlag>,
        trendInsights: List<IntelligenceInsight>,
        critical: Int,
        high: Int,
        totalGbv: Int,
        totalFgm: Int,
        minThreshold: Int,
        periodDays: Int
    ): IntelligenceInsight? {
        if (trendInsights.isNotEmpty()) return trendInsights.first()
        if (flags.isEmpty()) return null
        if (flags.size < minThreshold) return null
        return when {
            critical > 0 -> IntelligenceInsight(
                "$critical critical case(s) recorded during the selected $periodDays-day period require authorized review.",
                "period_critical"
            )
            high > 0 -> IntelligenceInsight(
                "$high high-priority case(s) recorded during the selected $periodDays-day period.",
                "period_high"
            )
            totalFgm > totalGbv -> IntelligenceInsight(
                "FGM-related reports represent the largest share of scoped cases in this period.",
                "period_fgm_dominant"
            )
            totalGbv > 0 -> IntelligenceInsight(
                "$totalGbv GBV-scoped case(s) recorded during the selected $periodDays-day period.",
                "period_gbv_volume"
            )
            else -> null
        }
    }

    private fun buildTrendInsights(
        flags: List<ReportFlag>,
        totalGbv: Int,
        totalFgm: Int,
        minThreshold: Int,
        periodDays: Int
    ): List<IntelligenceInsight> {
        if (flags.size < minThreshold) return emptyList()
        val insights = mutableListOf<IntelligenceInsight>()
        if (totalFgm > 0) {
            insights += IntelligenceInsight(
                "FGM-related reports represent $totalFgm case(s) during the selected $periodDays-day period.",
                "fgm_volume"
            )
        }
        if (totalGbv > 0 && totalFgm > 0) {
            val dominant = if (totalGbv >= totalFgm) "GBV" else "FGM"
            insights += IntelligenceInsight(
                "$dominant represents the largest share of scoped reports in this period.",
                "scope_mix"
            )
        } else if (totalGbv >= minThreshold) {
            insights += IntelligenceInsight(
                "Domestic and GBV-related categories represent the largest proportion of GBV reports.",
                "gbv_dominant"
            )
        }
        val imminentFgm = flags.count { flag ->
            scopeFor(flag) == "FGM" &&
                (AiRiskLevel.parse(flag.urgency) == AiRiskLevel.CRITICAL ||
                    AiRiskLevel.parse(flag.urgency) == AiRiskLevel.HIGH)
        }
        if (imminentFgm >= minThreshold) {
            insights += IntelligenceInsight(
                "Several reports contain indicators associated with imminent FGM risk.",
                "fgm_risk"
            )
        }
        val threats = flags.count {
            it.categoryId == "threats" || it.categoryId == "domestic" ||
                it.incidentCategory.contains("threat", ignoreCase = true)
        }
        if (threats >= minThreshold) {
            insights += IntelligenceInsight(
                "Threat-related language appears in multiple reports during this period.",
                "threat_trend"
            )
        }
        return insights
    }

    private fun buildInterventionInsights(
        flags: List<ReportFlag>,
        totalGbv: Int,
        totalFgm: Int,
        minThreshold: Int,
        trends: DashboardTrends
    ): List<IntelligenceInsight> {
        if (flags.isEmpty()) return emptyList()
        val insights = mutableListOf<IntelligenceInsight>()
        if (totalFgm >= minThreshold) {
            insights += IntelligenceInsight(
                "FGM prevention education may be prioritized for the affected reporting period.",
                "education_fgm"
            )
        }
        val dvCases = flags.count { it.categoryId == "domestic" || it.categoryId == "physical" }
        if (dvCases >= minThreshold) {
            insights += IntelligenceInsight(
                "High-priority domestic violence cases may require timely follow-up.",
                "followup_dv"
            )
        }
        val threatCases = flags.count { it.categoryId == "threats" }
        if (threatCases >= minThreshold) {
            insights += IntelligenceInsight(
                "Reports include threat-related incidents during this period.",
                "threat_response"
            )
        }
        if (trends.sosEvents >= minThreshold) {
            insights += IntelligenceInsight(
                "Elevated SOS activity suggests reviewing trusted-contact readiness and emergency pathways.",
                "sos_planning"
            )
        }
        return insights
    }

    private fun filterTrendsToPeriod(
        trends: DashboardTrends,
        flags: List<ReportFlag>,
        minThreshold: Int
    ): DashboardTrends {
        val relevance = linkedMapOf("GBV" to 0, "FGM" to 0)
        flags.forEach { flag ->
            val key = scopeFor(flag)
            relevance[key] = (relevance[key] ?: 0) + 1
        }
        val categories = flags.groupingBy { it.categoryLabel.ifBlank { it.categoryId } }
            .eachCount()
            .map { NamedCount(it.key, it.value) }
            .sortedByDescending { it.count }
        val weekly = flags.groupingBy { it.weekLabel }.eachCount()
            .toSortedMap()
            .map { NamedCount(it.key, it.value) }
        return trends.copy(
            gbvVsFgm = relevance.map { NamedCount(it.key, it.value) },
            categories = categories,
            weekly = weekly,
            geoAreas = reportService.buildGeoAreasForFlags(flags, minThreshold),
            geoWeekly = reportService.buildGeoWeeklyForFlags(flags, minThreshold)
        )
    }

    private fun scopeFor(flag: ReportFlag): String = when {
        flag.gbvFgmRelevance.equals("FGM", ignoreCase = true) -> "FGM"
        flag.gbvFgmRelevance.equals("BOTH", ignoreCase = true) -> "FGM"
        GbvFgmScope.isFgm(flag.categoryId) -> "FGM"
        else -> "GBV"
    }

    companion object {
        const val DASHBOARD_TITLE = "LINDA BINTI — AI SAFETY & RESPONSE INTELLIGENCE"
        const val AI_SAFETY_SECTION_TITLE = "AI Safety Intelligence"
        const val PRIVACY_INDICATOR =
            "Privacy protected — minimum aggregation thresholds applied."
        const val AI_DISCLAIMER =
            "AI-generated insights support authorized stakeholders and do not replace professional judgment. " +
                "Rule-based classification is shown clearly when cloud LLM is unavailable."
    }
}
