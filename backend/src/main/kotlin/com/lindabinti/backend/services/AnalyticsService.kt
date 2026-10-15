package com.lindabinti.backend.services

import com.lindabinti.backend.db.Database

data class AnalyticsEventIn(
    val eventType: String,
    val categoryOrBand: String,
    val weekLabel: String,
    val count: Int
)

data class AggregateBucket(
    val key: String,
    val count: Int,
    val suppressed: Boolean = false
)

data class AnalyticsSummary(
    val reportsByCategory: List<AggregateBucket>,
    val riskByBand: List<AggregateBucket>,
    val educationCompletions: Int,
    val sosEvents: Int,
    val byWeek: List<AggregateBucket>,
    val minThreshold: Int,
    val note: String
)

class AnalyticsService(
    private val db: Database,
    private val audit: AuditService,
    private val minThreshold: Int = 3
) {
    fun ingest(userId: String, events: List<AnalyticsEventIn>) {
        require(events.size <= 100) { "Too many events" }
        db.conn().prepareStatement(
            """
            INSERT INTO analytics_events(user_id, event_type, category_or_band, week_label, count, received_at)
            VALUES (?,?,?,?,?,?)
            """.trimIndent()
        ).use { ps ->
            for (e in events) {
                val type = e.eventType.trim().lowercase()
                require(type in setOf("report", "risk", "education", "sos")) { "Invalid eventType" }
                require(e.count in 1..1000) { "Invalid count" }
                // Privacy filter: reject values that look like phone numbers or long free text
                require(e.categoryOrBand.length <= 64) { "categoryOrBand too long" }
                require(!e.categoryOrBand.any { it.isDigit() } || e.categoryOrBand.length < 8) {
                    "Suspicious categoryOrBand"
                }
                ps.setString(1, userId)
                ps.setString(2, type)
                ps.setString(3, e.categoryOrBand.trim())
                ps.setString(4, e.weekLabel.trim().take(16))
                ps.setInt(5, e.count)
                ps.setLong(6, System.currentTimeMillis())
                ps.addBatch()
            }
            ps.executeBatch()
        }
        audit.log(userId, "ANALYTICS_INGEST", "events=${events.size}")
    }

    fun summaryForNgo(actorUserId: String): AnalyticsSummary {
        val reportCats = aggregateReportCategoriesFromReports()
        val riskBands = aggregateRiskBandsFromReports()
        val education = sumType("education")
        val sos = sumType("sos")
        val weeks = aggregateReportWeeksFromReports()
        audit.log(actorUserId, "ANALYTICS_SUMMARY_READ", "threshold=$minThreshold")
        return AnalyticsSummary(
            reportsByCategory = reportCats.entries.map { AggregateBucket(it.key, it.value, suppressed = false) },
            riskByBand = riskBands.entries.map { AggregateBucket(it.key, it.value, suppressed = false) },
            educationCompletions = if (education < minThreshold) 0 else education,
            sosEvents = if (sos < minThreshold) 0 else sos,
            byWeek = applyThreshold(weeks),
            minThreshold = minThreshold,
            note = "Anonymized aggregates only. No names, phones, exact GPS, or report bodies. Low-volume weekly and event buckets remain suppressed."
        )
    }

    private fun aggregateReportCategoriesFromReports(): Map<String, Int> {
        db.conn().prepareStatement(
            "SELECT category_id, gbv_fgm_relevance FROM reports"
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val map = linkedMapOf("GBV" to 0, "FGM" to 0)
                while (rs.next()) {
                    val categoryId = rs.getString(1).orEmpty()
                    val relevance = rs.getString(2).orEmpty()
                    val key = when (relevance.uppercase()) {
                        "FGM" -> "FGM"
                        "BOTH" -> "FGM"
                        else -> if (GbvFgmScope.isFgm(categoryId)) "FGM" else "GBV"
                    }
                    map[key] = (map[key] ?: 0) + 1
                }
                return map.filterValues { it > 0 }
            }
        }
    }

    private fun aggregateRiskBandsFromReports(): Map<String, Int> {
        db.conn().prepareStatement(
            "SELECT urgency FROM reports"
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val map = linkedMapOf<String, Int>()
                while (rs.next()) {
                    val band = rs.getString(1).orEmpty().uppercase().ifBlank { "MEDIUM" }
                    map[band] = (map[band] ?: 0) + 1
                }
                return map
            }
        }
    }

    private fun aggregateReportWeeksFromReports(): Map<String, Int> {
        db.conn().prepareStatement(
            "SELECT created_at FROM reports"
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val map = linkedMapOf<String, Int>()
                while (rs.next()) {
                    val week = weekLabel(rs.getLong(1))
                    map[week] = (map[week] ?: 0) + 1
                }
                return map
            }
        }
    }

    private fun aggregate(eventType: String, groupByCategory: Boolean): Map<String, Int> {
        val sql = if (groupByCategory) {
            "SELECT category_or_band, SUM(count) FROM analytics_events WHERE event_type = ? GROUP BY category_or_band"
        } else {
            "SELECT week_label, SUM(count) FROM analytics_events WHERE event_type = ? GROUP BY week_label"
        }
        db.conn().prepareStatement(sql).use { ps ->
            ps.setString(1, eventType)
            ps.executeQuery().use { rs ->
                val map = linkedMapOf<String, Int>()
                while (rs.next()) map[rs.getString(1)] = rs.getInt(2)
                return map
            }
        }
    }

    private fun aggregateByWeek(): Map<String, Int> {
        db.conn().prepareStatement(
            "SELECT week_label, SUM(count) FROM analytics_events GROUP BY week_label"
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val map = linkedMapOf<String, Int>()
                while (rs.next()) map[rs.getString(1)] = rs.getInt(2)
                return map
            }
        }
    }

    private fun sumType(eventType: String): Int {
        db.conn().prepareStatement(
            "SELECT COALESCE(SUM(count),0) FROM analytics_events WHERE event_type = ?"
        ).use { ps ->
            ps.setString(1, eventType)
            ps.executeQuery().use { rs ->
                rs.next()
                return rs.getInt(1)
            }
        }
    }

    fun applyThreshold(raw: Map<String, Int>): List<AggregateBucket> {
        val out = mutableListOf<AggregateBucket>()
        var suppressedTotal = 0
        raw.forEach { (k, v) ->
            if (v < minThreshold) suppressedTotal += v
            else out += AggregateBucket(k, v, suppressed = false)
        }
        if (suppressedTotal > 0) {
            out += AggregateBucket("suppressed", suppressedTotal, suppressed = true)
        }
        return out
    }

    private fun weekLabel(epochMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
        val week = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        val year = cal.get(java.util.Calendar.YEAR)
        return "%d-W%02d".format(year, week)
    }
}