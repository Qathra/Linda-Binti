package com.lindabinti.backend.services

import com.lindabinti.backend.ai.AiRiskAnalyzer
import com.lindabinti.backend.ai.AiRiskInput
import com.lindabinti.backend.ai.AiRiskLevel
import com.lindabinti.backend.ai.AiRiskResult
import com.lindabinti.backend.ai.AiStatus
import com.lindabinti.backend.ai.PiiMinimizer
import com.lindabinti.backend.ai.RulesAiRiskAnalyzer
import com.lindabinti.backend.db.Database
import java.util.Calendar
import java.util.UUID

data class StoredReport(
    val serverId: String,
    val userId: String,
    val clientUuid: String,
    val categoryId: String,
    val categoryLabel: String,
    val body: String,
    val areaCode: String = "",
    val areaLabel: String = "",
    val areaType: String = "",
    val createdAt: Long,
    val receivedAt: Long,
    val urgency: String = AiRiskLevel.MEDIUM,
    val explanation: String = "",
    val classifierSource: String = "rules",
    val aiStatus: String = AiStatus.PENDING,
    val gbvFgmRelevance: String = "",
    val incidentCategory: String = "",
    val urgencyIndicators: String = "",
    val safetyConcerns: String = "",
    val recommendedPriority: String = "",
    val caseStatus: String = CaseStatus.NEW
)

/** NGO-visible flag: never includes body, user id, email, or phone. */
data class ReportFlag(
    val serverReportId: String,
    val categoryId: String,
    val categoryLabel: String,
    val createdAtEpochMs: Long,
    val weekLabel: String,
    val urgency: String,
    val explanation: String,
    val classifierSource: String,
    val disclaimer: String,
    val aiStatus: String = AiStatus.UNAVAILABLE,
    val gbvFgmRelevance: String = "",
    val incidentCategory: String = "",
    val urgencyIndicators: String = "",
    val safetyConcerns: String = "",
    val recommendedPriority: String = "",
    val caseStatus: String = CaseStatus.NEW,
    val referenceId: String = "",
    val areaLabel: String = "",
    val areaType: String = "",
    val areaCode: String = ""
)

class ReportService(
    private val db: Database,
    private val audit: AuditService,
    private val aiOrchestration: AiOrchestrationService
) {
    /**
     * Idempotent insert by (userId, clientUuid). Never exposes other users' reports.
     * Always persists first; AI failure never blocks the save.
     */
    fun upsertOwnedReport(
        userId: String,
        clientUuid: String,
        categoryId: String,
        categoryLabel: String,
        body: String,
        createdAt: Long,
        includeBody: Boolean,
        areaCode: String? = null,
        areaLabel: String? = null,
        areaType: String? = null
    ): StoredReport {
        require(clientUuid.isNotBlank()) { "clientUuid required" }
        val normalizedCategoryId = GbvFgmScope.requireValidCategory(categoryId)
        val approvedArea = KajiadoGeoAreas.canonicalOrNull(areaCode, areaLabel, areaType)
        findByClientUuid(userId, clientUuid)?.let { return it }

        val storedBody = if (includeBody) body.take(8000) else ""
        val placeholder = applyResult(
            RulesAiRiskAnalyzer().analyze(
                PiiMinimizer.toInput(normalizedCategoryId, categoryLabel, storedBody, includeBody)
            )
        )
        val report = StoredReport(
            serverId = UUID.randomUUID().toString(),
            userId = userId,
            clientUuid = clientUuid.trim(),
            categoryId = normalizedCategoryId.take(64),
            categoryLabel = categoryLabel.trim().take(128),
            body = storedBody,
            areaCode = approvedArea?.code.orEmpty(),
            areaLabel = approvedArea?.label.orEmpty(),
            areaType = approvedArea?.type.orEmpty(),
            createdAt = createdAt,
            receivedAt = System.currentTimeMillis(),
            urgency = placeholder.riskLevel,
            explanation = placeholder.explanation,
            classifierSource = placeholder.source,
            aiStatus = AiStatus.PENDING,
            gbvFgmRelevance = placeholder.gbvFgmRelevance,
            incidentCategory = placeholder.incidentCategory,
            urgencyIndicators = placeholder.indicatorsText(),
            safetyConcerns = placeholder.concernsText(),
            recommendedPriority = placeholder.recommendedPriority,
            caseStatus = CaseStatus.NEW
        )
        val persisted = insert(report)
        if (persisted.serverId != report.serverId) {
            return persisted
        }
        audit.log(
            userId,
            "REPORT_UPSERT",
            "clientUuid=$clientUuid;category=$categoryId;bodyStored=$includeBody;urgency=${persisted.urgency}"
        )
        return applyAiAndUpdate(persisted, includeBody)
    }

    fun retryAiAnalysis(actorUserId: String, serverReportId: String): ReportFlag {
        val stored = findByServerId(serverReportId)
            ?: throw IllegalArgumentException("report not found")
        val updated = applyAiAndUpdate(stored, includeBody = stored.body.isNotBlank())
        audit.log(actorUserId, "REPORT_AI_RETRY", "serverId=$serverReportId;status=${updated.aiStatus}")
        return toFlag(updated)
    }

    fun updateCaseStatus(actorUserId: String, serverReportId: String, rawStatus: String): ReportFlag {
        val status = CaseStatus.requireValid(rawStatus)
        val stored = findByServerId(serverReportId)
            ?: throw IllegalArgumentException("report not found")
        db.conn().prepareStatement(
            "UPDATE reports SET case_status = ? WHERE server_id = ?"
        ).use { ps ->
            ps.setString(1, status)
            ps.setString(2, serverReportId)
            ps.executeUpdate()
        }
        audit.log(actorUserId, "CASE_STATUS_UPDATE", "ref=${referenceId(serverReportId)};status=$status")
        return toFlag(stored.copy(caseStatus = status))
    }

    fun overviewForNgo(actorUserId: String): DashboardOverview {
        audit.log(actorUserId, "DASHBOARD_OVERVIEW_READ", "gps=hidden")
        val flags = loadFlags()
        val byLevel = flags.groupingBy { AiRiskLevel.parse(it.urgency) ?: AiRiskLevel.MEDIUM }.eachCount()
        val totalFgm = flags.count { GbvFgmScope.isFgm(it.categoryId) || it.gbvFgmRelevance == "FGM" }
        val totalGbv = flags.size - totalFgm
        val byStatus = CaseStatus.let { cs ->
            listOf(cs.NEW, cs.REVIEWING, cs.REFERRED, cs.RESPONDING, cs.RESOLVED).map { key ->
                NamedCount(key, flags.count { CaseStatus.parse(it.caseStatus) == key })
            }
        }
        val pendingAi = flags.count { it.aiStatus.equals(AiStatus.PENDING, ignoreCase = true) }
        val attention = flags.count { flag ->
            val level = AiRiskLevel.parse(flag.urgency)
            CaseStatus.isOpen(flag.caseStatus) &&
                (level == AiRiskLevel.CRITICAL || level == AiRiskLevel.HIGH)
        }
        return DashboardOverview(
            totalReports = flags.size,
            totalGbvReports = totalGbv,
            totalFgmReports = totalFgm,
            critical = byLevel[AiRiskLevel.CRITICAL] ?: 0,
            high = byLevel[AiRiskLevel.HIGH] ?: 0,
            medium = byLevel[AiRiskLevel.MEDIUM] ?: 0,
            low = byLevel[AiRiskLevel.LOW] ?: 0,
            urgentCases = byLevel[AiRiskLevel.CRITICAL] ?: 0,
            highPriorityCases = (byLevel[AiRiskLevel.CRITICAL] ?: 0) + (byLevel[AiRiskLevel.HIGH] ?: 0),
            pendingAi = pendingAi,
            byStatus = byStatus,
            attentionCount = attention,
            serviceArea = SERVICE_AREA,
            gpsShown = false,
            note = LOCATION_NOTE
        )
    }

    fun trendsForNgo(actorUserId: String, analytics: AnalyticsSummary): DashboardTrends {
        audit.log(actorUserId, "DASHBOARD_TRENDS_READ", "gps=hidden")
        val flags = loadFlags()
        val relevance = linkedMapOf("GBV" to 0, "FGM" to 0, "BOTH" to 0, "UNCLEAR" to 0)
        flags.forEach { flag ->
            val key = when (flag.gbvFgmRelevance.uppercase()) {
                "GBV" -> "GBV"
                "FGM" -> "FGM"
                "BOTH" -> "BOTH"
                else -> if (GbvFgmScope.isFgm(flag.categoryId)) "FGM" else "GBV"
            }
            relevance[key] = (relevance[key] ?: 0) + 1
        }
        val categories = flags.groupingBy { it.categoryLabel.ifBlank { it.categoryId } }
            .eachCount()
            .map { NamedCount(it.key, it.value) }
            .sortedByDescending { it.count }
        val weekly = flags.groupingBy { it.weekLabel }.eachCount()
            .toSortedMap()
            .map { NamedCount(it.key, it.value) }
        val monthly = flags.groupingBy { monthLabel(it.createdAtEpochMs) }.eachCount()
            .toSortedMap()
            .map { NamedCount(it.key, it.value) }
        val geoAreas = buildGeoAreas(flags, analytics.minThreshold)
        val geoWeekly = buildGeoWeekly(flags, analytics.minThreshold)
        return DashboardTrends(
            gbvVsFgm = relevance.map { NamedCount(it.key, it.value) },
            categories = categories,
            weekly = weekly,
            monthly = monthly,
            geoAreas = geoAreas,
            geoWeekly = geoWeekly,
            location = DashboardLocationPrivacy(
                serviceArea = SERVICE_AREA,
                gpsShown = false,
                subCountyBreakdownAvailable = true,
                approvedAreaType = ApprovedArea.TYPE_SUB_COUNTY,
                note = LOCATION_NOTE
            ),
            educationCompletions = analytics.educationCompletions,
            sosEvents = analytics.sosEvents,
            analyticsNote = analytics.note
        )
    }

    fun listForUser(userId: String): List<StoredReport> {
        db.conn().prepareStatement(
            """
            SELECT $REPORT_COLUMNS
            FROM reports WHERE user_id = ? ORDER BY created_at DESC
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, userId)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<StoredReport>()
                while (rs.next()) out += readReport(rs)
                return out
            }
        }
    }

    fun findByClientUuid(userId: String, clientUuid: String): StoredReport? {
        db.conn().prepareStatement(
            """
            SELECT $REPORT_COLUMNS
            FROM reports WHERE user_id = ? AND client_uuid = ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, userId)
            ps.setString(2, clientUuid)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return readReport(rs)
            }
        }
    }

    fun findByServerId(serverId: String): StoredReport? {
        db.conn().prepareStatement(
            """
            SELECT $REPORT_COLUMNS
            FROM reports WHERE server_id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, serverId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return readReport(rs)
            }
        }
    }

    /**
     * Authorized NGO view: category, time, urgency, explanation.
     * Never returns body, userId, email, or other identifiers.
     */
    fun listFlagsForNgo(actorUserId: String, limit: Int = 100): List<ReportFlag> {
        audit.log(actorUserId, "REPORT_FLAGS_READ", "limit=$limit")
        return loadFlags(limit)
    }

    fun countReports(): Int {
        db.conn().prepareStatement("SELECT COUNT(*) FROM reports").use { ps ->
            ps.executeQuery().use { rs ->
                return if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    private fun loadFlags(limit: Int = 100): List<ReportFlag> {
        db.conn().prepareStatement(
            """
            SELECT $REPORT_COLUMNS
            FROM reports
            ORDER BY created_at DESC
            LIMIT ?
            """.trimIndent()
        ).use { ps ->
            ps.setInt(1, limit)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<ReportFlag>()
                while (rs.next()) {
                    out += toFlag(readReport(rs))
                }
                return out.sortedBy { AiRiskLevel.sortRank(it.urgency) }
            }
        }
    }

    private fun applyAiAndUpdate(report: StoredReport, includeBody: Boolean): StoredReport {
        val input: AiRiskInput = PiiMinimizer.toInput(
            report.categoryId,
            report.categoryLabel,
            report.body,
            includeBody
        )
        val result = try {
            aiOrchestration.analyzeNgoFlag(input, report.userId)
        } catch (_: Exception) {
            RulesAiRiskAnalyzer().analyze(input).copy(status = AiStatus.FAILED)
        }
        val applied = applyResult(result)
        val updated = report.copy(
            urgency = applied.riskLevel,
            explanation = applied.explanation,
            classifierSource = applied.source,
            aiStatus = applied.status,
            gbvFgmRelevance = applied.gbvFgmRelevance,
            incidentCategory = applied.incidentCategory,
            urgencyIndicators = applied.indicatorsText(),
            safetyConcerns = applied.concernsText(),
            recommendedPriority = applied.recommendedPriority
        )
        updateAiFields(updated)
        return updated
    }

    private fun applyResult(result: AiRiskResult): AiRiskResult {
        val level = AiRiskLevel.parse(result.riskLevel) ?: AiRiskLevel.MEDIUM
        val priority = AiRiskLevel.parse(result.recommendedPriority) ?: level
        return result.copy(riskLevel = level, recommendedPriority = priority)
    }

    private fun insert(report: StoredReport): StoredReport {
        db.conn().prepareStatement(
            """
            INSERT INTO reports(
              server_id, user_id, client_uuid, category_id, category_label, body,
              area_code, area_label, area_type,
              created_at, received_at, urgency, explanation, classifier_source,
              ai_status, gbv_fgm_relevance, incident_category, urgency_indicators,
              safety_concerns, recommended_priority, case_status
            )
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, report.serverId)
            ps.setString(2, report.userId)
            ps.setString(3, report.clientUuid)
            ps.setString(4, report.categoryId)
            ps.setString(5, report.categoryLabel)
            ps.setString(6, report.body)
            ps.setString(7, report.areaCode)
            ps.setString(8, report.areaLabel)
            ps.setString(9, report.areaType)
            ps.setLong(10, report.createdAt)
            ps.setLong(11, report.receivedAt)
            ps.setString(12, report.urgency)
            ps.setString(13, report.explanation)
            ps.setString(14, report.classifierSource)
            ps.setString(15, report.aiStatus)
            ps.setString(16, report.gbvFgmRelevance)
            ps.setString(17, report.incidentCategory)
            ps.setString(18, report.urgencyIndicators)
            ps.setString(19, report.safetyConcerns)
            ps.setString(20, report.recommendedPriority)
            ps.setString(21, CaseStatus.parse(report.caseStatus) ?: CaseStatus.NEW)
            return try {
                ps.executeUpdate()
                report
            } catch (_: Exception) {
                findByClientUuid(report.userId, report.clientUuid)
                    ?: throw IllegalStateException("Duplicate report race unresolved")
            }
        }
    }

    private fun updateAiFields(report: StoredReport) {
        db.conn().prepareStatement(
            """
            UPDATE reports SET
              urgency = ?, explanation = ?, classifier_source = ?,
              ai_status = ?, gbv_fgm_relevance = ?, incident_category = ?,
              urgency_indicators = ?, safety_concerns = ?, recommended_priority = ?
            WHERE server_id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, report.urgency)
            ps.setString(2, report.explanation)
            ps.setString(3, report.classifierSource)
            ps.setString(4, report.aiStatus)
            ps.setString(5, report.gbvFgmRelevance)
            ps.setString(6, report.incidentCategory)
            ps.setString(7, report.urgencyIndicators)
            ps.setString(8, report.safetyConcerns)
            ps.setString(9, report.recommendedPriority)
            ps.setString(10, report.serverId)
            ps.executeUpdate()
        }
    }

    private fun toFlag(stored: StoredReport): ReportFlag {
        var urgency = AiRiskLevel.parse(stored.urgency) ?: AiRiskLevel.MEDIUM
        var explanation = stored.explanation
        var source = stored.classifierSource.ifBlank { "rules" }
        if (explanation.isBlank()) {
            val classified = ReportRiskClassifier.classify(stored.categoryId, null)
            urgency = AiRiskLevel.parse(classified.urgency) ?: urgency
            explanation = classified.explanation
            source = classified.source
        }
        return ReportFlag(
            serverReportId = stored.serverId,
            categoryId = stored.categoryId,
            categoryLabel = stored.categoryLabel,
            createdAtEpochMs = stored.createdAt,
            weekLabel = weekLabel(stored.createdAt),
            urgency = urgency,
            explanation = explanation,
            classifierSource = source,
            disclaimer = "Not a medical, legal, police, or counselling decision.",
            aiStatus = stored.aiStatus.ifBlank { AiStatus.UNAVAILABLE },
            gbvFgmRelevance = stored.gbvFgmRelevance,
            incidentCategory = stored.incidentCategory.ifBlank { stored.categoryLabel },
            urgencyIndicators = stored.urgencyIndicators,
            safetyConcerns = stored.safetyConcerns,
            recommendedPriority = stored.recommendedPriority.ifBlank { urgency },
            caseStatus = CaseStatus.parse(stored.caseStatus) ?: CaseStatus.NEW,
            referenceId = referenceId(stored.serverId),
            areaLabel = stored.areaLabel,
            areaType = stored.areaType,
            areaCode = stored.areaCode
        )
    }

    private fun readReport(rs: java.sql.ResultSet): StoredReport = StoredReport(
        serverId = rs.getString(1),
        userId = rs.getString(2),
        clientUuid = rs.getString(3),
        categoryId = rs.getString(4),
        categoryLabel = rs.getString(5),
        body = rs.getString(6),
        areaCode = rs.getString(7).orEmpty(),
        areaLabel = rs.getString(8).orEmpty(),
        areaType = rs.getString(9).orEmpty(),
        createdAt = rs.getLong(10),
        receivedAt = rs.getLong(11),
        urgency = rs.getString(12) ?: AiRiskLevel.MEDIUM,
        explanation = rs.getString(13) ?: "",
        classifierSource = rs.getString(14) ?: "rules",
        aiStatus = rs.getString(15) ?: AiStatus.PENDING,
        gbvFgmRelevance = rs.getString(16).orEmpty(),
        incidentCategory = rs.getString(17).orEmpty(),
        urgencyIndicators = rs.getString(18).orEmpty(),
        safetyConcerns = rs.getString(19).orEmpty(),
        recommendedPriority = rs.getString(20).orEmpty(),
        caseStatus = rs.getString(21) ?: CaseStatus.NEW
    )

    fun buildGeoAreasForFlags(flags: List<ReportFlag>, minThreshold: Int): List<GeoAreaSummary> =
        buildGeoAreas(flags, minThreshold)

    fun buildGeoWeeklyForFlags(flags: List<ReportFlag>, minThreshold: Int): List<GeoAreaWeeklyTrend> =
        buildGeoWeekly(flags, minThreshold)

    private fun buildGeoAreas(flags: List<ReportFlag>, minThreshold: Int): List<GeoAreaSummary> {
        data class AreaCounts(
            val code: String,
            val label: String,
            val type: String,
            val gbv: Int,
            val fgm: Int,
            val sos: Int
        )
        val grouped = flags
            .mapNotNull { flag ->
                val canonical = runCatching {
                    KajiadoGeoAreas.canonicalOrNull(
                        flag.areaCode.ifBlank { null },
                        flag.areaLabel.ifBlank { null },
                        flag.areaType.ifBlank { null }
                    )
                }.getOrNull() ?: return@mapNotNull null
                flag to canonical
            }
            .groupBy { it.second.code }
            .mapValues { (_, pairs) ->
                val canonical = pairs.first().second
                val reports = pairs.map { it.first }
                AreaCounts(
                    code = canonical.code,
                    label = canonical.label,
                    type = canonical.type,
                    gbv = reports.count { scopeFor(it) == "GBV" && !GbvFgmScope.isSos(it.categoryId) },
                    fgm = reports.count { scopeFor(it) == "FGM" },
                    sos = reports.count { GbvFgmScope.isSos(it.categoryId) }
                )
            }
        val visible = mutableListOf<GeoAreaSummary>()
        var suppressedTotal = 0
        grouped.values.sortedByDescending { it.gbv + it.fgm + it.sos }.forEach { counts ->
            val total = counts.gbv + counts.fgm + counts.sos
            if (total < minThreshold) {
                suppressedTotal += total
            } else {
                visible += GeoAreaSummary(
                    areaCode = counts.code,
                    areaLabel = counts.label,
                    areaType = counts.type,
                    gbvCount = counts.gbv,
                    fgmCount = counts.fgm,
                    totalCount = total,
                    sosCount = counts.sos
                )
            }
        }
        if (suppressedTotal > 0) {
            visible += GeoAreaSummary(
                areaCode = "suppressed",
                areaLabel = "Suppressed",
                areaType = ApprovedArea.TYPE_SUB_COUNTY,
                gbvCount = 0,
                fgmCount = 0,
                totalCount = suppressedTotal,
                sosCount = 0,
                suppressed = true
            )
        }
        return AreaHotspotAssigner.assign(visible)
    }

    private fun buildGeoWeekly(flags: List<ReportFlag>, minThreshold: Int): List<GeoAreaWeeklyTrend> {
        val rows = flags
            .filter { it.areaLabel.isNotBlank() }
            .groupBy { "${it.weekLabel}|${it.areaLabel}" }
            .values
            .map { reports ->
                val sample = reports.first()
                val gbv = reports.count { scopeFor(it) == "GBV" }
                val fgm = reports.count { scopeFor(it) == "FGM" }
                GeoAreaWeeklyTrend(
                    weekLabel = sample.weekLabel,
                    areaLabel = sample.areaLabel,
                    gbvCount = gbv,
                    fgmCount = fgm,
                    totalCount = gbv + fgm,
                    suppressed = gbv + fgm < minThreshold
                )
            }
            .sortedWith(compareByDescending<GeoAreaWeeklyTrend> { it.weekLabel }.thenByDescending { it.totalCount })
        return rows.take(12)
    }

    private fun scopeFor(flag: ReportFlag): String = when {
        flag.gbvFgmRelevance.equals("FGM", ignoreCase = true) -> "FGM"
        flag.gbvFgmRelevance.equals("BOTH", ignoreCase = true) -> "FGM"
        GbvFgmScope.isFgm(flag.categoryId) -> "FGM"
        else -> "GBV"
    }

    private fun weekLabel(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return "%d-W%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.WEEK_OF_YEAR))
    }

    private fun monthLabel(epochMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
        return "%d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    companion object {
        const val SERVICE_AREA = "Kajiado County"
        const val LOCATION_NOTE =
            "Exact GPS is not shown. Only coarse Kajiado sub-county areas may be stored for ordinary reports. Household or map-pin precision is never displayed."

        fun referenceId(serverId: String): String =
            "LB-" + serverId.replace("-", "").take(8).uppercase()

        const val REPORT_COLUMNS =
            "server_id, user_id, client_uuid, category_id, category_label, body, " +
                "area_code, area_label, area_type, " +
                "created_at, received_at, urgency, explanation, classifier_source, " +
                "ai_status, gbv_fgm_relevance, incident_category, urgency_indicators, " +
                "safety_concerns, recommended_priority, case_status"
    }
}
