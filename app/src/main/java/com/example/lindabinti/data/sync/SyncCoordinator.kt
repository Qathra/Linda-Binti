package com.example.lindabinti.data.sync

import com.example.lindabinti.data.education.EducationContentRepository
import com.example.lindabinti.data.offline.NetworkMonitor
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.repository.SupportServicesRepository
import com.example.lindabinti.data.stats.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pulls server content and pushes queued local records when connectivity returns.
 * Neutral: does not surface report contents in notifications.
 */
class SyncCoordinator(
    private val network: NetworkMonitor,
    private val reportSyncManager: ReportSyncManager,
    private val supportServicesRepository: SupportServicesRepository,
    private val educationContentRepository: EducationContentRepository,
    private val analyticsUploadManager: AnalyticsUploadManager,
    private val statsRepository: StatsRepository,
    private val prefs: SecurePrefs,
    private val session: BackendSession,
    private val scope: CoroutineScope,
    private val ensureSession: suspend () -> Boolean = { session.isLoggedIn() }
) {
    data class CycleResult(
        val online: Boolean,
        val reports: ReportSyncManager.SyncResult?,
        val supportPulled: Boolean,
        val educationPulled: Boolean,
        val analyticsUploaded: Boolean,
        val message: String
    )

    private val mutex = Mutex()
    private val _lastResult = MutableStateFlow<CycleResult?>(null)
    val lastResult: StateFlow<CycleResult?> = _lastResult.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private var started = false
    private var debounceJob: Job? = null
    private var lastCycleAt = 0L

    fun start() {
        if (started) return
        started = true
        network.start()
        scope.launch {
            runCycle(force = true, reason = "startup")
        }
        scope.launch {
            network.isOnline.collect { online ->
                if (!online) return@collect
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(RECONNECT_DEBOUNCE_MS)
                    runCycle(force = false, reason = "reconnect")
                }
            }
        }
    }

    suspend fun syncNow(): CycleResult = runCycle(force = true, reason = "manual")

    private suspend fun runCycle(force: Boolean, reason: String): CycleResult = mutex.withLock {
        val now = System.currentTimeMillis()
        if (!force && now - lastCycleAt < MIN_CYCLE_INTERVAL_MS) {
            return@withLock _lastResult.value ?: CycleResult(
                online = network.isInternetLikely(),
                reports = null,
                supportPulled = false,
                educationPulled = false,
                analyticsUploaded = false,
                message = "Sync skipped (too soon)"
            )
        }
        _syncing.value = true
        try {
            val online = network.isInternetLikely()
            if (!online) {
                val offline = CycleResult(
                    online = false,
                    reports = null,
                    supportPulled = false,
                    educationPulled = false,
                    analyticsUploaded = false,
                    message = "Offline — local data kept on device"
                )
                _lastResult.value = offline
                return@withLock offline
            }

            var supportOk = false
            runCatching {
                supportServicesRepository.refreshFromBackend()
                supportOk = true
            }

            var educationOk = false
            runCatching {
                educationOk = educationContentRepository.refreshFromBackend()
            }

            if (supportOk || educationOk) {
                prefs.lastContentSyncAt = System.currentTimeMillis()
            }

            runCatching { ensureSession() }

            val reportResult = runCatching {
                reportSyncManager.syncPending(forceRetry = force || reason == "manual")
            }.getOrNull()

            var analyticsOk = false
            if (prefs.shareAnonymousStats && session.isLoggedIn()) {
                runCatching {
                    val buckets = statsRepository.prepareAnonymizedBuckets(true)
                    analyticsUploadManager.uploadBuckets(buckets)
                    analyticsOk = true
                }
            }

            lastCycleAt = System.currentTimeMillis()
            prefs.lastSyncCycleAt = lastCycleAt
            val result = CycleResult(
                online = true,
                reports = reportResult,
                supportPulled = supportOk,
                educationPulled = educationOk,
                analyticsUploaded = analyticsOk,
                message = buildMessage(reason, reportResult, supportOk, educationOk)
            )
            _lastResult.value = result
            result
        } finally {
            _syncing.value = false
        }
    }

    private fun buildMessage(
        reason: String,
        reports: ReportSyncManager.SyncResult?,
        supportOk: Boolean,
        educationOk: Boolean
    ): String {
        val parts = mutableListOf("Sync ($reason)")
        if (supportOk) parts += "support directory updated"
        if (educationOk) parts += "education catalog updated"
        if (reports != null) {
            parts += "${reports.synced} reports synced, ${reports.failed} failed"
        }
        return parts.joinToString(" · ")
    }

    companion object {
        private const val RECONNECT_DEBOUNCE_MS = 900L
        private const val MIN_CYCLE_INTERVAL_MS = 12_000L
    }
}
