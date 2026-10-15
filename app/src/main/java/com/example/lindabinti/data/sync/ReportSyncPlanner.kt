package com.example.lindabinti.data.sync

/**
 * Pure planner for offline queue / retry / duplicate prevention (unit-testable).
 */
object ReportSyncPlanner {

    data class PlanItem(
        val localId: Long,
        val clientUuid: String,
        val categoryId: String,
        val categoryLabel: String,
        val body: String,
        val createdAt: Long,
        val includeBody: Boolean
    )

    fun selectForUpload(
        reports: List<QueuedReport>,
        maxBatch: Int = 20,
        nowMs: Long = System.currentTimeMillis(),
        forceRetry: Boolean = false
    ): List<QueuedReport> =
        reports
            .filter { SyncStatus.isQueueable(it.syncStatus) }
            .filter { forceRetry || isReadyForRetry(it, nowMs) }
            .sortedBy { it.createdAt }
            .take(maxBatch)

    fun isReadyForRetry(report: QueuedReport, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (report.syncStatus == SyncStatus.PENDING && report.syncAttempts <= 0) return true
        if (report.syncStatus == SyncStatus.PENDING && report.lastSyncAttemptAt == null) return true
        val last = report.lastSyncAttemptAt ?: return true
        return nowMs - last >= backoffMs(report.syncAttempts)
    }

    /** Exponential backoff: 5s → 15s → 45s → … capped at 15 minutes. */
    fun backoffMs(attempts: Int): Long {
        val n = attempts.coerceIn(0, 8)
        val delay = BASE_BACKOFF_MS * (1L shl n.coerceAtMost(5))
        return delay.coerceAtMost(MAX_BACKOFF_MS)
    }

    fun nextStatusOnSuccess(): String = SyncStatus.SYNCED

    fun nextStatusOnFailure(attempts: Int, maxAttemptsBeforeStayFailed: Int = 8): String =
        if (attempts >= maxAttemptsBeforeStayFailed) SyncStatus.FAILED else SyncStatus.FAILED

    fun shouldSkipDuplicate(alreadySyncedClientUuids: Set<String>, clientUuid: String): Boolean =
        clientUuid in alreadySyncedClientUuids

    private const val BASE_BACKOFF_MS = 5_000L
    private const val MAX_BACKOFF_MS = 15 * 60_000L
}

data class QueuedReport(
    val localId: Long,
    val clientUuid: String,
    val categoryId: String,
    val categoryLabel: String,
    val body: String,
    val createdAt: Long,
    val areaCode: String?,
    val areaLabel: String?,
    val areaType: String?,
    val syncStatus: String,
    val syncAttempts: Int,
    val includeBodyOnSync: Boolean,
    val lastSyncAttemptAt: Long? = null
)
