package com.example.lindabinti.data.sync

import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.ReportDao
import com.example.lindabinti.data.remote.AnalyticsIngestRequestDto
import com.example.lindabinti.data.remote.AnalyticsSummaryDto
import com.example.lindabinti.data.remote.AuthResponseDto
import com.example.lindabinti.data.remote.EducationContentDto
import com.example.lindabinti.data.remote.EducationContentListDto
import com.example.lindabinti.data.remote.FakeBackendSession
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.remote.LoginRequestDto
import com.example.lindabinti.data.remote.LogoutRequestDto
import com.example.lindabinti.data.remote.RegisterRequestDto
import com.example.lindabinti.data.remote.ReportFlagDto
import com.example.lindabinti.data.remote.ReportSubmitRequestDto
import com.example.lindabinti.data.remote.ReportSubmitResponseDto
import com.example.lindabinti.data.remote.SupportServicesCatalogDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ReportSyncManagerTest {

    private class MemDao : ReportDao {
        val rows = mutableListOf<Report>()
        override fun observeAll(): Flow<List<Report>> = flowOf(rows)
        override suspend fun getAll(): List<Report> = rows.toList()
        override suspend fun getById(id: Long): Report? = rows.firstOrNull { it.id == id }
        override suspend fun getPendingOrFailed(): List<Report> =
            rows.filter { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
        override suspend fun insert(report: Report): Long {
            val id = (rows.maxOfOrNull { it.id } ?: 0L) + 1L
            rows += report.copy(id = id)
            return id
        }
        override suspend fun deleteById(id: Long) { rows.removeAll { it.id == id } }
        override suspend fun updateSyncState(
            id: Long,
            status: String,
            serverReportId: String?,
            attempts: Int,
            lastAttemptAt: Long
        ) {
            val i = rows.indexOfFirst { it.id == id }
            if (i >= 0) {
                val r = rows[i]
                rows[i] = r.copy(
                    syncStatus = status,
                    serverReportId = serverReportId ?: r.serverReportId,
                    syncAttempts = attempts,
                    lastSyncAttemptAt = lastAttemptAt
                )
            }
        }
        override suspend fun updateSyncStatusOnly(id: Long, status: String) {
            updateSyncState(id, status, null, rows.firstOrNull { it.id == id }?.syncAttempts ?: 0, System.currentTimeMillis())
        }
        override suspend fun markAllLocalAsPending(): Int {
            var n = 0
            rows.replaceAll {
                if (it.syncStatus == SyncStatus.LOCAL) {
                    n++
                    it.copy(syncStatus = SyncStatus.PENDING)
                } else it
            }
            return n
        }
        override suspend fun countSyncedByClientUuid(clientUuid: String): Int =
            rows.count { it.clientUuid == clientUuid && it.syncStatus == SyncStatus.SYNCED }
        override suspend fun updateAiAnalysis(id: Long, json: String, analyzedAt: Long, source: String) = Unit
    }

    private class SyncApi(
        var failOnce: Boolean = false,
        var calls: Int = 0
    ) : LindaBintiApi {
        var lastBody: ReportSubmitRequestDto? = null
        override suspend fun register(body: RegisterRequestDto) = error("n/a")
        override suspend fun login(body: LoginRequestDto) = error("n/a")
        override suspend fun deviceSession(body: com.example.lindabinti.data.remote.DeviceSessionRequestDto) = error("n/a")
        override suspend fun refresh(body: com.example.lindabinti.data.remote.RefreshRequestDto) = error("n/a")
        override suspend fun logout(bearer: String, body: LogoutRequestDto): Response<Unit> = Response.success(Unit)
        override suspend fun submitReport(
            bearer: String,
            idempotencyKey: String,
            body: ReportSubmitRequestDto
        ): ReportSubmitResponseDto {
            calls++
            lastBody = body
            if (failOnce) {
                failOnce = false
                throw RuntimeException("network down")
            }
            return ReportSubmitResponseDto("srv-$idempotencyKey", idempotencyKey, "stored")
        }
        override suspend fun ingestAnalytics(bearer: String, body: AnalyticsIngestRequestDto) =
            Response.success(Unit)
        override suspend fun analyticsSummary(bearer: String) = AnalyticsSummaryDto()
        override suspend fun reportFlags(bearer: String): List<ReportFlagDto> = emptyList()
        override suspend fun retryReportFlagAnalysis(bearer: String, serverReportId: String): ReportFlagDto =
            error("n/a")
        override suspend fun health() = mapOf("status" to "ok")
        override suspend fun aiCapabilities() =
            com.example.lindabinti.data.remote.AiCapabilitiesResponseDto(onlineEnabled = false)
        override suspend fun analyzeReport(
            bearer: String,
            body: com.example.lindabinti.data.remote.AiAnalyzeReportRequestDto
        ) = error("n/a")
        override suspend fun assistantChat(
            bearer: String,
            body: com.example.lindabinti.data.remote.AssistantChatRequestDto
        ) = error("n/a")
        override suspend fun supportServices() = SupportServicesCatalogDto()
        override suspend fun educationContent(language: String?, category: String?, type: String?) =
            EducationContentListDto()
        override suspend fun educationContentById(id: String) = EducationContentDto(id = id)
    }

    private fun userSession() = FakeBackendSession().also {
        it.saveSession(
            AuthResponseDto("t", "r", "USER", "u", "e@e.com", System.currentTimeMillis() + 99999)
        )
    }

    @Test
    fun consentOffSkipsPrivateReports() = runBlocking {
        val dao = MemDao()
        dao.insert(Report(category = "P", body = "b", syncStatus = SyncStatus.PENDING, clientUuid = "c1"))
        val mgr = ReportSyncManager(dao, SyncApi(), userSession(), hasSyncConsent = { false })
        val result = mgr.syncPending()
        assertEquals(0, result.synced)
        assertEquals(ReportSyncManager.CODE_CONSENT_OFF, result.code)
    }

    @Test
    fun syncsPendingWhenConsentAndUserSession() = runBlocking {
        val dao = MemDao()
        dao.insert(
            Report(
                category = "Physical",
                categoryId = "physical",
                body = "note",
                syncStatus = SyncStatus.PENDING,
                clientUuid = "uuid-1"
            )
        )
        val api = SyncApi()
        val mgr = ReportSyncManager(dao, api, userSession(), hasSyncConsent = { true })
        val result = mgr.syncPending()
        assertEquals(1, result.synced)
        assertEquals(SyncStatus.SYNCED, dao.getAll().first().syncStatus)
        assertEquals(1, api.calls)
        assertEquals(null, api.lastBody?.areaCode)
    }

    @Test
    fun failedSyncRemainsRetryable() = runBlocking {
        val dao = MemDao()
        dao.insert(
            Report(
                category = "Physical",
                categoryId = "physical",
                body = "note",
                syncStatus = SyncStatus.PENDING,
                clientUuid = "uuid-2"
            )
        )
        val api = SyncApi(failOnce = true)
        val mgr = ReportSyncManager(dao, api, userSession(), hasSyncConsent = { true })
        val fail = mgr.syncPending()
        assertEquals(1, fail.failed)
        assertEquals(SyncStatus.FAILED, dao.getAll().first().syncStatus)
        // Manual / forced retry skips backoff so the same row remains recoverable.
        val retry = mgr.syncPending(forceRetry = true)
        assertEquals(1, retry.synced)
        assertEquals(SyncStatus.SYNCED, dao.getAll().first().syncStatus)
    }

    @Test
    fun ngoRoleCannotSyncAsUser() = runBlocking {
        val dao = MemDao()
        dao.insert(Report(category = "P", body = "b", syncStatus = SyncStatus.PENDING, clientUuid = "c"))
        val session = FakeBackendSession().also {
            it.saveSession(
                AuthResponseDto("t", "r", "NGO_STAFF", "u", "ngo@e.com", System.currentTimeMillis() + 99999)
            )
        }
        val mgr = ReportSyncManager(dao, SyncApi(), session, hasSyncConsent = { true })
        val result = mgr.syncPending()
        assertEquals(0, result.synced)
        assertEquals(ReportSyncManager.CODE_NEED_USER, result.code)
        assertTrue(result.message.contains("USER", ignoreCase = true))
    }

    @Test
    fun syncIncludesApproximateAreaOnly() = runBlocking {
        val dao = MemDao()
        dao.insert(
            Report(
                category = "Physical",
                categoryId = "physical",
                body = "note",
                areaCode = "kajiado_north",
                areaLabel = "Kajiado North",
                areaType = "SUB_COUNTY",
                syncStatus = SyncStatus.PENDING,
                clientUuid = "uuid-area"
            )
        )
        val api = SyncApi()
        val mgr = ReportSyncManager(dao, api, userSession(), hasSyncConsent = { true })
        val result = mgr.syncPending()
        assertEquals(1, result.synced)
        assertEquals("kajiado_north", api.lastBody?.areaCode)
        assertEquals("Kajiado North", api.lastBody?.areaLabel)
        assertEquals("SUB_COUNTY", api.lastBody?.areaType)
    }
}
