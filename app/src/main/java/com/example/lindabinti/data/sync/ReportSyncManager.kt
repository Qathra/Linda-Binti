package com.example.lindabinti.data.sync

import com.example.lindabinti.data.local.ReportDao
import com.example.lindabinti.data.remote.BackendClientFactory
import com.example.lindabinti.data.remote.BackendHttpErrors
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.remote.ReportSubmitRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportSyncManager(
    private val reportDao: ReportDao,
    private val api: LindaBintiApi?,
    private val session: BackendSession,
    private val hasSyncConsent: () -> Boolean
) {
    data class SyncResult(
        val attempted: Int,
        val synced: Int,
        val failed: Int,
        val skipped: Int,
        val message: String,
        val code: String = CODE_OK
    )

    suspend fun syncPending(
        forceRetry: Boolean = false,
        allowSosWithoutConsent: Boolean = false
    ): SyncResult = withContext(Dispatchers.IO) {
        if (api == null) {
            return@withContext SyncResult(0, 0, 0, 0, "Backend URL not configured", CODE_NOT_CONFIGURED)
        }
        if (!session.isLoggedIn() || !session.isUserRole()) {
            return@withContext SyncResult(0, 0, 0, 0, "Sign in with a USER cloud account to sync", CODE_NEED_USER)
        }
        if (!hasSyncConsent() && !allowSosWithoutConsent) {
            return@withContext SyncResult(0, 0, 0, 0, "Report sync consent is off", CODE_CONSENT_OFF)
        }
        val bearer = session.bearer() ?: return@withContext SyncResult(0, 0, 0, 0, "Missing token", CODE_MISSING_TOKEN)

        val allMapped = reportDao.getAll().map {
            QueuedReport(
                localId = it.id,
                clientUuid = it.clientUuid,
                categoryId = it.categoryId.ifBlank { "other" },
                categoryLabel = it.category,
                body = it.body,
                createdAt = it.createdAt,
                areaCode = it.areaCode,
                areaLabel = it.areaLabel,
                areaType = it.areaType,
                syncStatus = it.syncStatus,
                syncAttempts = it.syncAttempts,
                includeBodyOnSync = it.includeBodyOnSync,
                lastSyncAttemptAt = it.lastSyncAttemptAt
            )
        }
        val queued = ReportSyncPlanner.selectForUpload(
            reports = allMapped,
            forceRetry = forceRetry
        ).let { selected ->
            if (!hasSyncConsent() && allowSosWithoutConsent) {
                selected.filter { it.categoryId == "sos" }
            } else {
                selected
            }
        }

        if (queued.isEmpty()) {
            return@withContext SyncResult(0, 0, 0, 0, "No pending or failed reports to sync", CODE_NOTHING_PENDING)
        }

        var synced = 0
        var failed = 0
        var skipped = 0
        val lastErrors = mutableListOf<String>()
        val already = reportDao.getAll()
            .filter { it.syncStatus == SyncStatus.SYNCED }
            .map { it.clientUuid }
            .toSet()

        for (item in queued) {
            if (item.clientUuid.isBlank()) {
                failed++
                lastErrors += "missing clientUuid"
                continue
            }
            if (ReportSyncPlanner.shouldSkipDuplicate(already, item.clientUuid)) {
                skipped++
                continue
            }
            try {
                val response = submitWithReconnectRetry(api, bearer, item)
                reportDao.updateSyncState(
                    id = item.localId,
                    status = ReportSyncPlanner.nextStatusOnSuccess(),
                    serverReportId = response.serverReportId,
                    attempts = item.syncAttempts + 1,
                    lastAttemptAt = System.currentTimeMillis()
                )
                synced++
            } catch (e: Exception) {
                // Keep FAILED so the same local row can be retried after a fix — never delete.
                reportDao.updateSyncState(
                    id = item.localId,
                    status = SyncStatus.FAILED,
                    serverReportId = null,
                    attempts = item.syncAttempts + 1,
                    lastAttemptAt = System.currentTimeMillis()
                )
                failed++
                lastErrors += BackendHttpErrors.toUserMessage(e)
            }
        }
        val detail = if (lastErrors.isNotEmpty()) " (${lastErrors.take(2).joinToString("; ")})" else ""
        SyncResult(
            attempted = queued.size,
            synced = synced,
            failed = failed,
            skipped = skipped,
            message = "Sync finished: $synced synced, $failed failed, $skipped skipped$detail",
            code = CODE_FINISHED
        )
    }

    /**
     * Authenticated report POST uses the same Retrofit client as health/login.
     * On TCP connect failures, evict idle sockets once and retry — stale pooled
     * connections / proxy races can fail POSTs after a successful GET health check.
     */
    private suspend fun submitWithReconnectRetry(
        api: LindaBintiApi,
        bearer: String,
        item: QueuedReport
    ) = try {
        submitOnce(api, bearer, item)
    } catch (first: Exception) {
        if (!BackendHttpErrors.isConnectFailure(first)) throw first
        BackendClientFactory.evictAllConnections()
        submitOnce(api, bearer, item)
    }

    private suspend fun submitOnce(
        api: LindaBintiApi,
        bearer: String,
        item: QueuedReport
    ) = api.submitReport(
        bearer = bearer,
        idempotencyKey = item.clientUuid,
        body = ReportSubmitRequestDto(
            clientUuid = item.clientUuid,
            categoryId = item.categoryId,
            categoryLabel = item.categoryLabel,
            body = if (item.includeBodyOnSync) item.body else "",
            createdAtEpochMs = item.createdAt,
            includeBody = item.includeBodyOnSync,
            areaCode = item.areaCode,
            areaLabel = item.areaLabel,
            areaType = item.areaType
        )
    )

    suspend fun markPendingIfEligible(reportId: Long) {
        if (!session.isLoggedIn() || !session.isUserRole()) return
        reportDao.updateSyncStatusOnly(reportId, SyncStatus.PENDING)
    }

    companion object {
        const val CODE_OK = "ok"
        const val CODE_NOT_CONFIGURED = "not_configured"
        const val CODE_CONSENT_OFF = "consent_off"
        const val CODE_NEED_USER = "need_user"
        const val CODE_MISSING_TOKEN = "missing_token"
        const val CODE_NOTHING_PENDING = "nothing_pending"
        const val CODE_FINISHED = "finished"
    }
}
