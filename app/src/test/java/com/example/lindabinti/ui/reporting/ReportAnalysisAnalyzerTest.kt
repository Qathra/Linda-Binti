package com.example.lindabinti.ui.reporting

import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisException
import com.example.lindabinti.ai.AiAnalysisInput
import com.example.lindabinti.ai.AiAnalysisMerger
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.ai.AiRoutingPolicy
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.ai.OfflineAiAnalysisEngine
import com.example.lindabinti.ai.OnlineAiAnalysisEngine
import com.example.lindabinti.ai.OnlineAiClient
import com.example.lindabinti.assistant.AssistantEngine
import com.example.lindabinti.assistant.LocalAssistantEngine
import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.ReportDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReportAnalysisAnalyzerTest {

    @Test
    fun orchestratorPersistsOfflineAnalysisLocally() = runBlocking {
        val dao = CapturingReportDao()
        val orchestrator = AiOrchestrator(
            offline = OfflineAiAnalysisEngine(),
            online = OnlineAiAnalysisEngine(FailingOnlineClient()),
            localAssistant = LocalAssistantEngine(),
            remoteAssistant = null,
            policy = offlinePolicy()
        )
        val report = Report(
            id = 1L,
            category = "FGM pressure",
            categoryId = "fgm_pressure",
            body = "They will cut my sister tomorrow"
        )
        val result = ReportAnalysisAnalyzer.analyzeReport(orchestrator, report, dao)
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
        assertEquals("OFFLINE AI", result.sourceLabel)
        assertNotNull(dao.savedJson)
        assertEquals("OFFLINE AI", dao.savedSource)
    }

    @Test
    fun mergePrefersOnlineWithOfflineValidation() {
        val offline = AiAnalysisResult(
            analysisId = "off-1",
            source = AiAnalysisSource.OFFLINE,
            category = AiAnalysisCategory.FGM_THREAT,
            severity = AiSeverity.CRITICAL,
            confidence = 0.7,
            indicators = listOf("Imminent FGM"),
            explanation = "Offline",
            recommendations = listOf("Reach safety"),
            educationRecommendations = listOf("fgm"),
            timestamp = 1L
        )
        val online = offline.copy(
            analysisId = "on-1",
            source = AiAnalysisSource.ONLINE,
            severity = AiSeverity.HIGH,
            confidence = 0.88,
            explanation = "Online analysis",
            providerId = "provider",
            modelId = "model",
            consentGranted = true
        )
        val merged = AiAnalysisMerger.merge(offline, online)
        assertEquals(AiSeverity.CRITICAL, merged.severity)
        assertEquals(AiAnalysisSource.HYBRID, merged.source)
        assertEquals("HYBRID AI", merged.sourceLabel)
    }

    private fun offlinePolicy() = AiRoutingPolicy(
        isBackendConfigured = { false },
        isNetworkAvailable = { false },
        hasCloudAiConsent = { false },
        isUserSessionActive = { false },
        isOnlineAiConfigured = { false }
    )

    private class FailingOnlineClient : OnlineAiClient {
        override suspend fun analyzeReport(input: AiAnalysisInput) =
            throw AiAnalysisException("fail")

        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class CapturingReportDao : ReportDao {
        var savedJson: String? = null
        var savedSource: String? = null

        override suspend fun updateAiAnalysis(id: Long, json: String, analyzedAt: Long, source: String) {
            savedJson = json
            savedSource = source
        }

        override suspend fun getById(id: Long): Report? = null
        override fun observeAll(): Flow<List<Report>> = throw UnsupportedOperationException()
        override suspend fun getAll(): List<Report> = emptyList()
        override suspend fun getPendingOrFailed(): List<Report> = emptyList()
        override suspend fun insert(report: Report): Long = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        override suspend fun updateSyncState(id: Long, status: String, serverReportId: String?, attempts: Int, lastAttemptAt: Long) =
            throw UnsupportedOperationException()
        override suspend fun updateSyncStatusOnly(id: Long, status: String) = throw UnsupportedOperationException()
        override suspend fun markAllLocalAsPending(): Int = 0
        override suspend fun countSyncedByClientUuid(clientUuid: String): Int = 0
    }
}
