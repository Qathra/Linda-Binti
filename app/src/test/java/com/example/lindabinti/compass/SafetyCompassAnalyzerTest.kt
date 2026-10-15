package com.example.lindabinti.compass

import com.example.lindabinti.ai.AiAnalysisCategory
import com.example.lindabinti.ai.AiAnalysisException
import com.example.lindabinti.ai.AiAnalysisInput
import com.example.lindabinti.ai.AiAnalysisResult
import com.example.lindabinti.ai.AiAnalysisSource
import com.example.lindabinti.ai.AiAnalysisStorage
import com.example.lindabinti.ai.AiOrchestrator
import com.example.lindabinti.ai.AiRoutingPolicy
import com.example.lindabinti.ai.AiSeverity
import com.example.lindabinti.ai.OfflineAiAnalysisEngine
import com.example.lindabinti.ai.OnlineAiAnalysisEngine
import com.example.lindabinti.ai.OnlineAiClient
import com.example.lindabinti.ai.PrivacyFilter
import com.example.lindabinti.assistant.AssistantEngine
import com.example.lindabinti.assistant.AssistantChatTurn
import com.example.lindabinti.assistant.AssistantMessage
import com.example.lindabinti.assistant.AssistantSource
import com.example.lindabinti.data.content.LocalContent
import com.example.lindabinti.data.local.Report
import com.example.lindabinti.data.local.RiskAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyCompassAnalyzerTest {

    private fun offlinePolicy(consent: Boolean = false) = AiRoutingPolicy(
        isBackendConfigured = { false },
        isNetworkAvailable = { true },
        hasCloudAiConsent = { consent },
        isUserSessionActive = { true },
        isOnlineAiConfigured = { true }
    )

    private fun onlinePolicy(consent: Boolean = true) = AiRoutingPolicy(
        isBackendConfigured = { true },
        isNetworkAvailable = { true },
        hasCloudAiConsent = { consent },
        isUserSessionActive = { true },
        isOnlineAiConfigured = { true }
    )

    private fun orchestrator(
        onlineClient: OnlineAiClient = FailingOnlineClient(),
        policy: AiRoutingPolicy = offlinePolicy()
    ) = AiOrchestrator(
        offline = OfflineAiAnalysisEngine(),
        online = OnlineAiAnalysisEngine(onlineClient),
        localAssistant = noopAssistant(),
        remoteAssistant = null,
        policy = policy
    )

    @Test
    fun offlineCompassUsesLocalAnalysisOnly() = runBlocking {
        val report = report(
            categoryId = "physical",
            category = "Physical violence",
            body = "He threatened me at home"
        )
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 1
        )
        assertEquals(CompassSourceLabel.OFFLINE, snapshot.aiSourceLabel)
        assertTrue(snapshot.gbvFgmConcerns.isNotEmpty())
        assertTrue(snapshot.nextSteps.isNotEmpty())
        assertEquals(1, snapshot.trustedContactCount)
    }

    @Test
    fun onlineCompassMergesCloudWithLocalBaseline() = runBlocking {
        val report = report(
            categoryId = "physical",
            category = "Physical violence",
            body = "He hit me repeatedly"
        )
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(
                onlineClient = StubOnlineClient(),
                policy = onlinePolicy()
            ),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertEquals(CompassSourceLabel.HYBRID, snapshot.aiSourceLabel)
    }

    @Test
    fun onlineFailureFallsBackToOfflineCompass() = runBlocking {
        val report = report(
            categoryId = "fgm_pressure",
            category = "FGM pressure",
            body = "They will cut my daughter tomorrow"
        )
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(
                onlineClient = FailingOnlineClient(),
                policy = onlinePolicy()
            ),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertEquals(CompassSourceLabel.OFFLINE, snapshot.aiSourceLabel)
        assertTrue(snapshot.status.ordinal >= SafetyCompassStatus.HIGH.ordinal)
    }

    @Test
    fun consentDeniedNeverShowsOnlineLabel() = runBlocking {
        val tracking = TrackingOnlineClient()
        val report = report(categoryId = "physical", category = "Physical", body = "Violence at home")
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(onlineClient = tracking, policy = onlinePolicy(consent = false)),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertEquals(0, tracking.calls)
        assertEquals(CompassSourceLabel.OFFLINE, snapshot.aiSourceLabel)
    }

    @Test
    fun privacyRedactionBeforeCompassOnlineAnalysis() {
        val raw = "Call Amina at 0712345678 PIN 1102 amina@test.com"
        val prepared = SafetyCompassAnalyzer.compassInputFor(
            report(categoryId = "physical", category = "Physical", body = raw)
        )
        assertFalse(prepared.description.contains("0712345678"))
        assertFalse(prepared.description.contains("1102"))
        assertFalse(prepared.description.contains("amina@test.com"))
        assertTrue(prepared.includeDescription)
    }

    @Test
    fun privacyRedactionAppliedToCloudPayload() = runBlocking {
        val tracking = CapturingOnlineClient()
        val report = report(
            categoryId = "physical",
            category = "Physical",
            body = "Call me at 0712345678 about violence"
        )
        SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(onlineClient = tracking, policy = onlinePolicy()),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertTrue(tracking.lastInput != null)
        assertFalse(tracking.lastInput!!.description.contains("0712345678"))
    }

    @Test
    fun gbvAndFgmReportsSurfaceInCompassConcerns() = runBlocking {
        val gbv = report(categoryId = "physical", category = "Physical", body = "He hit me")
        val fgm = report(categoryId = "fgm_incident", category = "FGM", body = "She was cut")
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(gbv, fgm),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertTrue(snapshot.gbvFgmConcerns.size >= 2)
        assertEquals(SafetyCompassStatus.HIGH, snapshot.status)
    }

    @Test
    fun criticalRiskPreservedWhenOnlineUnderestimates() = runBlocking {
        val report = report(
            categoryId = "fgm_pressure",
            category = "FGM pressure",
            body = "They will cut my daughter tonight with a knife"
        )
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(
                onlineClient = LowSeverityOnlineClient(),
                policy = onlinePolicy()
            ),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertEquals(CompassSourceLabel.HYBRID, snapshot.aiSourceLabel)
        assertEquals(SafetyCompassStatus.CRITICAL, snapshot.status)
    }

    @Test
    fun cachedOfflineAnalysisUsedWhenOnlineUnavailable() = runBlocking {
        val offlineResult = orchestrator().analyze(
            AiAnalysisInput("physical", "Physical", "hit me", true)
        )
        val cachedJson = AiAnalysisStorage.toJson(offlineResult)
        val report = report(
            categoryId = "physical",
            category = "Physical",
            body = "hit me",
            aiAnalysisJson = cachedJson
        )
        val tracking = TrackingOnlineClient()
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(onlineClient = tracking, policy = offlinePolicy()),
            language = "en",
            latestRisk = null,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = listOf(report),
            completedEducationIds = emptySet(),
            trustedContactCount = 0
        )
        assertEquals(0, tracking.calls)
        assertEquals(CompassSourceLabel.OFFLINE, snapshot.aiSourceLabel)
    }

    @Test
    fun compassSourceLabelNeverOnlineWithoutSuccessfulOnlineResult() {
        assertEquals(CompassSourceLabel.OFFLINE, CompassSourceLabel.resolve(emptyList()))
        val offlineOnly = listOf(sampleResult(AiAnalysisSource.OFFLINE))
        assertEquals(CompassSourceLabel.OFFLINE, CompassSourceLabel.resolve(offlineOnly))
        val hybrid = listOf(sampleResult(AiAnalysisSource.HYBRID))
        assertEquals(CompassSourceLabel.HYBRID, CompassSourceLabel.resolve(hybrid))
        val pureOnline = listOf(sampleResult(AiAnalysisSource.ONLINE))
        assertEquals(CompassSourceLabel.ONLINE, CompassSourceLabel.resolve(pureOnline))
    }

    @Test
    fun riskAssessmentAndEducationIncludedOffline() = runBlocking {
        val risk = RiskAssessment(
            id = 1L,
            answersJson = "q1=true;q2=true;q3=false;q4=true;q5=false",
            score = 4,
            band = "high",
            createdAt = System.currentTimeMillis()
        )
        val snapshot = SafetyCompassAnalyzer.buildSnapshot(
            orchestrator = orchestrator(),
            language = "en",
            latestRisk = risk,
            riskQuestions = LocalContent.riskQuestions("en"),
            reports = emptyList(),
            completedEducationIds = setOf("gbv"),
            trustedContactCount = 2
        )
        assertEquals(CompassSourceLabel.OFFLINE, snapshot.aiSourceLabel)
        assertEquals(SafetyCompassStatus.HIGH, snapshot.status)
        assertTrue(snapshot.safetyFactors.isNotEmpty())
        assertTrue(snapshot.showTrustedContacts)
    }

    private fun report(
        categoryId: String,
        category: String,
        body: String,
        aiAnalysisJson: String? = null
    ) = Report(
        id = 1L,
        category = category,
        categoryId = categoryId,
        body = body,
        aiAnalysisJson = aiAnalysisJson
    )

    private fun sampleResult(source: AiAnalysisSource) = AiAnalysisResult(
        analysisId = "x",
        source = source,
        category = AiAnalysisCategory.GBV,
        severity = AiSeverity.HIGH,
        confidence = 0.7,
        indicators = emptyList(),
        explanation = "x",
        recommendations = emptyList(),
        educationRecommendations = emptyList(),
        timestamp = 1L
    )

    private fun noopAssistant(): AssistantEngine = object : AssistantEngine {
        override suspend fun answer(userText: String, history: List<AssistantChatTurn>): AssistantMessage =
            AssistantMessage("ok", source = AssistantSource.Local)
    }

    private class FailingOnlineClient : OnlineAiClient {
        override suspend fun analyzeReport(input: AiAnalysisInput) =
            throw AiAnalysisException("offline only")
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class TrackingOnlineClient : OnlineAiClient {
        var calls = 0
        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult {
            calls++
            throw AiAnalysisException("should not be called")
        }
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class CapturingOnlineClient : OnlineAiClient {
        var lastInput: AiAnalysisInput? = null
        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult {
            lastInput = input
            return StubOnlineClient().analyzeReport(input)
        }
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class StubOnlineClient : OnlineAiClient {
        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult =
            AiAnalysisResult(
                analysisId = "online-1",
                source = AiAnalysisSource.ONLINE,
                category = AiAnalysisCategory.GBV,
                severity = AiSeverity.HIGH,
                confidence = 0.9,
                indicators = listOf("Violence"),
                explanation = "Online analysis from cloud provider.",
                recommendations = listOf("Safety"),
                educationRecommendations = emptyList(),
                timestamp = System.currentTimeMillis(),
                providerId = "test-provider",
                modelId = "test-model",
                consentGranted = true
            )
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class LowSeverityOnlineClient : OnlineAiClient {
        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult =
            AiAnalysisResult(
                analysisId = "online-low",
                source = AiAnalysisSource.ONLINE,
                category = AiAnalysisCategory.FGM,
                severity = AiSeverity.LOWER,
                confidence = 0.6,
                indicators = listOf("FGM mention"),
                explanation = "Online underestimated severity.",
                recommendations = listOf("Learn more"),
                educationRecommendations = emptyList(),
                timestamp = System.currentTimeMillis(),
                providerId = "test-provider",
                consentGranted = true
            )
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }
}
