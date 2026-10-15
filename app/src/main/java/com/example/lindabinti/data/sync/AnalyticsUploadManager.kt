package com.example.lindabinti.data.sync

import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.remote.AnalyticsEventDto
import com.example.lindabinti.data.remote.AnalyticsIngestRequestDto
import com.example.lindabinti.data.remote.BackendHttpErrors
import com.example.lindabinti.data.remote.BackendSession
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.stats.AnonymizedBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnalyticsUploadManager(
    private val api: LindaBintiApi?,
    private val session: BackendSession,
    private val prefs: SecurePrefs
) {
    suspend fun uploadBuckets(buckets: List<AnonymizedBucket>): Result<Int> = withContext(Dispatchers.IO) {
        if (api == null) return@withContext Result.failure(IllegalStateException("Backend not configured"))
        if (!prefs.shareAnonymousStats) {
            return@withContext Result.failure(IllegalStateException("Anonymous stats opt-in is off"))
        }
        if (!session.isLoggedIn() || !session.isUserRole()) {
            return@withContext Result.failure(IllegalStateException("USER session required"))
        }
        val bearer = session.bearer()
            ?: return@withContext Result.failure(IllegalStateException("Missing token"))

        val events = buckets.mapNotNull { b ->
            val cat = PrivacyFilter.sanitizeCategoryOrBand(b.categoryOrBand) ?: return@mapNotNull null
            PrivacyFilter.assertNoPii(
                mapOf("eventType" to b.eventType, "categoryOrBand" to cat, "weekLabel" to b.weekLabel)
            )
            AnalyticsEventDto(
                eventType = b.eventType,
                categoryOrBand = cat,
                weekLabel = b.weekLabel,
                count = b.count
            )
        }
        if (events.isEmpty()) return@withContext Result.success(0)
        runCatching {
            api.ingestAnalytics(bearer, AnalyticsIngestRequestDto(events))
            events.size
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }
}
