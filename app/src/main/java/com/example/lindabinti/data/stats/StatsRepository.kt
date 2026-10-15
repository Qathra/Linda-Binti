package com.example.lindabinti.data.stats

import com.example.lindabinti.data.local.ReportDao
import com.example.lindabinti.data.local.RiskDao
import com.example.lindabinti.data.local.SosDao
import java.util.Calendar

/**
 * Builds anonymized aggregate counts only — no names, phones, GPS, or free text.
 * This student build never uploads; opt-in only prepares local buckets for a future API.
 */
data class AnonymizedBucket(
    val eventType: String, // sos | report | risk
    val categoryOrBand: String,
    val weekLabel: String,
    val count: Int
)

data class NgoDashboardSnapshot(
    val generatedAtMillis: Long,
    val fromDevice: Boolean,
    /** True when figures are hard-coded demo samples, not this phone's Room data. */
    val isDemoSample: Boolean,
    val sosTotal: Int,
    val reportsTotal: Int,
    val riskCritical: Int = 0,
    val riskHigh: Int,
    val riskElevated: Int,
    val riskLower: Int,
    val reportByCategory: Map<String, Int>,
    val weeklySos: Map<String, Int>,
    val note: String
)

class StatsRepository(
    private val sosDao: SosDao,
    private val reportDao: ReportDao,
    private val riskDao: RiskDao
) {

    suspend fun buildSnapshot(includeDemoIfEmpty: Boolean = true): NgoDashboardSnapshot {
        val sosEvents = sosDao.getAllEvents()
        val reports = reportDao.getAll()
        val risks = riskDao.getAll()

        val hasData = sosEvents.isNotEmpty() || reports.isNotEmpty() || risks.isNotEmpty()
        if (!hasData && includeDemoIfEmpty) {
            return demoSnapshot()
        }

        val reportByCategory = reports.groupingBy { it.category }.eachCount()
        val weeklySos = sosEvents.groupingBy { weekLabel(it.triggeredAt) }.eachCount()
        val riskBands = risks.groupingBy { it.band }.eachCount()

        return NgoDashboardSnapshot(
            generatedAtMillis = System.currentTimeMillis(),
            fromDevice = hasData,
            isDemoSample = false,
            sosTotal = sosEvents.size,
            reportsTotal = reports.size,
            riskCritical = riskBands["critical"] ?: 0,
            riskHigh = riskBands["high"] ?: 0,
            riskElevated = riskBands["elevated"] ?: 0,
            riskLower = riskBands["lower"] ?: 0,
            reportByCategory = reportByCategory,
            weeklySos = weeklySos,
            note = if (hasData) {
                "THIS DEVICE — aggregates from local Room data only. No names, phones, or exact GPS."
            } else {
                "THIS DEVICE — no local events yet."
            }
        )
    }

    /** Explicit fictional demo for viva/examiner preview — never mixed with device counts. */
    fun buildDemoSnapshot(): NgoDashboardSnapshot = demoSnapshot()

    /**
     * Local-only preparation of anonymized buckets.
     * Returns empty when the user has not opted in, and never sends network traffic.
     */
    suspend fun prepareAnonymizedBuckets(shareEnabled: Boolean): List<AnonymizedBucket> {
        if (!shareEnabled) return emptyList()
        return buildLocalBuckets()
    }

    /** @deprecated Prefer [prepareAnonymizedBuckets] which respects the opt-in flag. */
    suspend fun buildUploadBuckets(): List<AnonymizedBucket> = buildLocalBuckets()

    private suspend fun buildLocalBuckets(): List<AnonymizedBucket> {
        val snap = buildSnapshot(includeDemoIfEmpty = false)
        if (snap.isDemoSample || !snap.fromDevice) return emptyList()
        val buckets = mutableListOf<AnonymizedBucket>()
        snap.weeklySos.forEach { (week, count) ->
            buckets += AnonymizedBucket("sos", "all", week, count)
        }
        snap.reportByCategory.forEach { (cat, count) ->
            buckets += AnonymizedBucket("report", cat, weekLabel(System.currentTimeMillis()), count)
        }
        if (snap.riskCritical > 0) {
            buckets += AnonymizedBucket(
                "risk",
                "critical",
                weekLabel(System.currentTimeMillis()),
                snap.riskCritical
            )
        }
        if (snap.riskHigh > 0) {
            buckets += AnonymizedBucket("risk", "high", weekLabel(System.currentTimeMillis()), snap.riskHigh)
        }
        if (snap.riskElevated > 0) {
            buckets += AnonymizedBucket(
                "risk",
                "elevated",
                weekLabel(System.currentTimeMillis()),
                snap.riskElevated
            )
        }
        if (snap.riskLower > 0) {
            buckets += AnonymizedBucket(
                "risk",
                "lower",
                weekLabel(System.currentTimeMillis()),
                snap.riskLower
            )
        }
        return buckets
    }

    private fun weekLabel(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        return "%d-W%02d".format(year, week)
    }

    private fun demoSnapshot(): NgoDashboardSnapshot {
        return NgoDashboardSnapshot(
            generatedAtMillis = System.currentTimeMillis(),
            fromDevice = false,
            isDemoSample = true,
            sosTotal = 12,
            reportsTotal = 9,
            riskCritical = 1,
            riskHigh = 3,
            riskElevated = 4,
            riskLower = 5,
            reportByCategory = mapOf(
                "GBV" to 4,
                "FGM threat" to 3,
                "Child marriage" to 1,
                "Other" to 1
            ),
            weeklySos = mapOf(
                "2026-W31" to 2,
                "2026-W32" to 5,
                "2026-W33" to 5
            ),
            note = "DEMO SAMPLE — fictional stakeholder preview. Not live community or this-device data."
        )
    }
}
