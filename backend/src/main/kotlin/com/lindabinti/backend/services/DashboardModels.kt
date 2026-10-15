package com.lindabinti.backend.services

data class NamedCount(val key: String, val count: Int)

data class DashboardOverview(
    val totalReports: Int,
    val totalGbvReports: Int,
    val totalFgmReports: Int,
    val critical: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val urgentCases: Int,
    val highPriorityCases: Int,
    val pendingAi: Int,
    val byStatus: List<NamedCount>,
    val attentionCount: Int,
    val serviceArea: String,
    val gpsShown: Boolean,
    val note: String
)

data class DashboardLocationPrivacy(
    val serviceArea: String,
    val gpsShown: Boolean,
    val subCountyBreakdownAvailable: Boolean,
    val approvedAreaType: String,
    val note: String
)

data class GeoAreaSummary(
    val areaCode: String,
    val areaLabel: String,
    val areaType: String,
    val gbvCount: Int,
    val fgmCount: Int,
    val totalCount: Int,
    val sosCount: Int = 0,
    /** HIGH | ELEVATED | LOWER — from case volume in this Kajiado sub-county. */
    val hotspotLevel: String = "LOWER",
    val hotspotReason: String = "",
    val suppressed: Boolean = false
)

data class GeoAreaWeeklyTrend(
    val weekLabel: String,
    val areaLabel: String,
    val gbvCount: Int,
    val fgmCount: Int,
    val totalCount: Int,
    val suppressed: Boolean = false
)

data class DashboardTrends(
    val gbvVsFgm: List<NamedCount>,
    val categories: List<NamedCount>,
    val weekly: List<NamedCount>,
    val monthly: List<NamedCount>,
    val geoAreas: List<GeoAreaSummary>,
    val geoWeekly: List<GeoAreaWeeklyTrend>,
    val location: DashboardLocationPrivacy,
    val educationCompletions: Int,
    val sosEvents: Int,
    val analyticsNote: String
)

data class AiEngineStatusDto(
    val mode: String,
    val label: String,
    val health: String,
    val onlineConfigured: Boolean,
    val degradedReason: String = "",
    /** WORKING | FALLBACK | FAILED | NOT CONFIGURED */
    val statusLabel: String = "",
    /** Active classification mode shown to NGO staff */
    val activeSourceLabel: String = ""
)

data class AiCommandSummary(
    val totalGbvCases: Int,
    val totalFgmCases: Int,
    val highPriorityCases: Int,
    val criticalCases: Int,
    val recentIncidents: Int,
    val aiFlaggedCases: Int,
    val pendingAi: Int,
    val openCriticalHigh: Int,
    val totalReports: Int
)

data class AiPriorityQueueItem(
    val referenceId: String,
    val serverReportId: String,
    val category: String,
    val categoryId: String,
    val scope: String,
    val severity: String,
    val urgency: String,
    val incidentType: String,
    val confidence: Double,
    val confidencePercent: Int,
    val indicators: List<String>,
    val explanation: String,
    val whyFlagged: String,
    val recommendedPriority: String,
    val createdAtEpochMs: Long,
    val caseStatus: String,
    val aiStatus: String,
    val aiStatusLabel: String,
    val classifierSource: String,
    val sourceLabel: String,
    val gbvFgmRelevance: String,
    val areaLabel: String,
    val areaCode: String = ""
)

data class AiTrendComparison(
    val currentPeriodDays: Int,
    val previousPeriodDays: Int,
    val currentTotal: Int,
    val previousTotal: Int,
    val totalChangePercent: Int?,
    val currentGbv: Int,
    val previousGbv: Int,
    val currentFgm: Int,
    val previousFgm: Int,
    val currentCritical: Int,
    val previousCritical: Int,
    val emergingCategories: List<NamedCount>,
    val highRiskAreas: List<NamedCount>,
    val note: String
)

data class IntelligenceInsight(
    val text: String,
    val kind: String
)

data class SeverityBucket(
    val key: String,
    val displayLabel: String,
    val count: Int
)

data class NgoIntelligenceDashboard(
    val dashboardTitle: String,
    val aiSafetySectionTitle: String,
    val periodDays: Int,
    val generatedAtEpochMs: Long,
    val aiEngine: AiEngineStatusDto,
    val commandSummary: AiCommandSummary,
    val priorityQueue: List<AiPriorityQueueItem>,
    val priorityCases: List<AiPriorityQueueItem>,
    val caseFlags: List<AiPriorityQueueItem>,
    val severityDistribution: List<NamedCount>,
    val severityDisplay: List<SeverityBucket>,
    val gbvVsFgm: List<NamedCount>,
    val trendInsights: List<IntelligenceInsight>,
    val interventionInsights: List<IntelligenceInsight>,
    val insightOfPeriod: IntelligenceInsight?,
    val trendComparison: AiTrendComparison,
    val trends: DashboardTrends,
    /** Per-area counts for the live map (threshold 1 — any reported case flags the sub-county). */
    val mapGeoAreas: List<GeoAreaSummary>,
    val geoInsufficient: Boolean,
    val privacyNote: String,
    val privacyIndicator: String,
    val aiDisclaimer: String,
    val minThreshold: Int
)