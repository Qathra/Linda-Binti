package com.example.lindabinti.data.stats

import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.ReportDao
import com.example.lindabinti.data.local.RiskAssessment
import com.example.lindabinti.data.local.RiskDao
import com.example.lindabinti.data.local.SosDao
import com.example.lindabinti.data.local.SosDelivery
import com.example.lindabinti.data.local.SosEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsRepositoryHonestyTest {

    private val emptySos = object : SosDao {
        override suspend fun insertEvent(event: SosEvent): Long = 0
        override suspend fun updateEventStatus(id: Long, status: String) = Unit
        override suspend fun insertDelivery(delivery: SosDelivery): Long = 0
        override suspend fun updateDelivery(id: Long, status: String, error: String?) = Unit
        override suspend fun latestEvent(): SosEvent? = null
        override fun observeLatestEvent(): Flow<SosEvent?> = flowOf(null)
        override suspend fun getAllEvents(): List<SosEvent> = emptyList()
        override suspend fun getDelivery(id: Long): SosDelivery? = null
        override suspend fun getDeliveriesForEvent(eventId: Long): List<SosDelivery> = emptyList()
    }

    private val emptyReports = object : ReportDao {
        override fun observeAll(): Flow<List<Report>> = flowOf(emptyList())
        override suspend fun getAll(): List<Report> = emptyList()
        override suspend fun getById(id: Long): Report? = null
        override suspend fun getPendingOrFailed(): List<Report> = emptyList()
        override suspend fun insert(report: Report): Long = 0
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun updateSyncState(id: Long, status: String, serverReportId: String?, attempts: Int, lastAttemptAt: Long) = Unit
        override suspend fun updateSyncStatusOnly(id: Long, status: String) = Unit
        override suspend fun markAllLocalAsPending(): Int = 0
        override suspend fun countSyncedByClientUuid(clientUuid: String): Int = 0
        override suspend fun updateAiAnalysis(id: Long, json: String, analyzedAt: Long, source: String) = Unit
    }

    private val emptyRisk = object : RiskDao {
        override fun observeLatest(): Flow<RiskAssessment?> = flowOf(null)
        override fun observeRecent(): Flow<List<RiskAssessment>> = flowOf(emptyList())
        override suspend fun getAll(): List<RiskAssessment> = emptyList()
        override suspend fun insert(assessment: RiskAssessment): Long = 0
    }

    @Test
    fun emptyDeviceShowsDemoSampleNotDeviceData() = runBlocking {
        val repo = StatsRepository(emptySos, emptyReports, emptyRisk)
        val snap = repo.buildSnapshot(includeDemoIfEmpty = true)
        assertTrue(snap.isDemoSample)
        assertFalse(snap.fromDevice)
        assertTrue(snap.note.contains("DEMO", ignoreCase = true))
    }

    @Test
    fun prepareBucketsEmptyWhenOptOut() = runBlocking {
        val repo = StatsRepository(emptySos, emptyReports, emptyRisk)
        assertEquals(emptyList<AnonymizedBucket>(), repo.prepareAnonymizedBuckets(shareEnabled = false))
    }

    @Test
    fun prepareBucketsEmptyForDemoOnlyDevice() = runBlocking {
        val repo = StatsRepository(emptySos, emptyReports, emptyRisk)
        // Opt-in but no real events → still no buckets (demo must never be treated as uploadable)
        assertEquals(emptyList<AnonymizedBucket>(), repo.prepareAnonymizedBuckets(shareEnabled = true))
    }
}
