package com.example.lindabinti.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSyncPlannerTest {

    private fun queued(
        status: String,
        attempts: Int = 0,
        lastAt: Long? = null,
        createdAt: Long = 1_000L
    ) = QueuedReport(
        localId = 1L,
        clientUuid = "uuid-1",
        categoryId = "physical",
        categoryLabel = "Physical",
        body = "",
        createdAt = createdAt,
        areaCode = null,
        areaLabel = null,
        areaType = null,
        syncStatus = status,
        syncAttempts = attempts,
        includeBodyOnSync = false,
        lastSyncAttemptAt = lastAt
    )

    @Test
    fun pendingNeverAttemptedIsReadyImmediately() {
        assertTrue(ReportSyncPlanner.isReadyForRetry(queued(SyncStatus.PENDING)))
    }

    @Test
    fun failedRespectsBackoffWindow() {
        val now = 100_000L
        val last = now - 1_000L
        val item = queued(SyncStatus.FAILED, attempts = 1, lastAt = last)
        assertFalse(ReportSyncPlanner.isReadyForRetry(item, nowMs = now))
        assertTrue(
            ReportSyncPlanner.isReadyForRetry(
                item,
                nowMs = last + ReportSyncPlanner.backoffMs(1) + 1
            )
        )
    }

    @Test
    fun selectForUploadSkipsItemsStillInBackoff() {
        val now = 50_000L
        val ready = queued(SyncStatus.PENDING, attempts = 0, createdAt = 1)
        val waiting = queued(SyncStatus.FAILED, attempts = 2, lastAt = now - 100, createdAt = 2)
            .copy(localId = 2, clientUuid = "uuid-2")
        val selected = ReportSyncPlanner.selectForUpload(listOf(ready, waiting), nowMs = now)
        assertEquals(1, selected.size)
        assertEquals("uuid-1", selected[0].clientUuid)
    }

    @Test
    fun backoffIncreasesThenCaps() {
        assertTrue(ReportSyncPlanner.backoffMs(0) < ReportSyncPlanner.backoffMs(3))
        assertEquals(ReportSyncPlanner.backoffMs(10), ReportSyncPlanner.backoffMs(20))
    }

    @Test
    fun forceRetryIncludesItemsStillInBackoff() {
        val now = 50_000L
        val waiting = queued(SyncStatus.FAILED, attempts = 2, lastAt = now - 100, createdAt = 2)
        val withoutForce = ReportSyncPlanner.selectForUpload(listOf(waiting), nowMs = now, forceRetry = false)
        assertTrue(withoutForce.isEmpty())
        val withForce = ReportSyncPlanner.selectForUpload(listOf(waiting), nowMs = now, forceRetry = true)
        assertEquals(1, withForce.size)
    }

    @Test
    fun duplicateClientUuidIsSkipped() {
        assertTrue(ReportSyncPlanner.shouldSkipDuplicate(setOf("a", "b"), "a"))
        assertFalse(ReportSyncPlanner.shouldSkipDuplicate(setOf("a", "b"), "c"))
    }
}
