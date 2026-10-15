package com.example.lindabinti.data.repository

import com.example.lindabinti.data.content.GbvReportCategories
import com.example.lindabinti.data.local.EducationDao
import com.example.lindabinti.data.local.EducationProgress
import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.ReportDao
import com.example.lindabinti.data.local.RiskAssessment
import com.example.lindabinti.data.local.RiskDao
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.sync.SyncStatus
import com.example.lindabinti.geo.ReportArea
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class SavedLocalReport(
    val id: Long,
    val syncStatus: String
)

class ReportRepository(
    private val dao: ReportDao,
    private val prefs: SecurePrefs,
    private val session: BackendSession
) {
    fun observeReports(): Flow<List<Report>> = dao.observeAll()

    /**
     * Always writes to Room first, then queues for the NGO dashboard.
     * Opening the app is consent. Exact GPS is not stored — only a Kajiado sub-county.
     */
    suspend fun addReport(
        categoryLabel: String,
        body: String,
        categoryId: String = "other",
        area: ReportArea? = null
    ): SavedLocalReport {
        val status = SyncStatus.PENDING
        val id = dao.insert(
            Report(
                category = categoryLabel,
                categoryId = GbvReportCategories.normalize(categoryId),
                body = body.trim(),
                latitude = null,
                longitude = null,
                areaCode = area?.code,
                areaLabel = area?.label,
                areaType = area?.type,
                syncStatus = status,
                clientUuid = UUID.randomUUID().toString(),
                includeBodyOnSync = prefs.syncReportBodies
            )
        )
        return SavedLocalReport(id = id, syncStatus = status)
    }

    /**
     * After SOS SMS, queue a CRITICAL NGO flag with coarse Kajiado sub-county when known.
     * Exact GPS is never stored — only approved area labels. Syncs automatically.
     */
    suspend fun queueSosAlertForNgo(area: ReportArea? = null): SavedLocalReport {
        val id = dao.insert(
            Report(
                category = "SOS alert",
                categoryId = "sos",
                body = "SOS activated right now. Immediate responder review required.",
                latitude = null,
                longitude = null,
                areaCode = area?.code,
                areaLabel = area?.label,
                areaType = area?.type,
                syncStatus = SyncStatus.PENDING,
                clientUuid = UUID.randomUUID().toString(),
                includeBodyOnSync = true
            )
        )
        return SavedLocalReport(id = id, syncStatus = SyncStatus.PENDING)
    }

    /** Promote leftover LOCAL rows so they still sync after this build. */
    suspend fun queueLocalReportsForSync(): Int {
        return dao.markAllLocalAsPending()
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): Report? = dao.getById(id)

    suspend fun getAll(): List<Report> = dao.getAll()
}

class RiskRepository(private val dao: RiskDao) {
    fun observeLatest(): Flow<RiskAssessment?> = dao.observeLatest()

    fun observeRecent(): Flow<List<RiskAssessment>> = dao.observeRecent()

    suspend fun save(answersJson: String, score: Int, band: String): Long {
        return dao.insert(
            RiskAssessment(
                answersJson = answersJson,
                score = score,
                band = band
            )
        )
    }
}

class EducationRepository(private val dao: EducationDao) {
    fun observeProgress(): Flow<List<EducationProgress>> = dao.observeAll()

    suspend fun markComplete(moduleId: String) {
        dao.upsert(EducationProgress(moduleId = moduleId))
    }
}
