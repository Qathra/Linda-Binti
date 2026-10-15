package com.example.lindabinti.ai

import com.example.lindabinti.ai.AssistantOnlineBlocker
import com.example.lindabinti.assistant.AssistantEngine
import com.example.lindabinti.assistant.AssistantChatTurn
import com.example.lindabinti.assistant.AssistantMessage
import com.example.lindabinti.assistant.AssistantRemoteClient
import com.example.lindabinti.assistant.AssistantSource
import com.example.lindabinti.data.content.AssistantKnowledge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiOrchestratorTest {

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

    private fun testLocalAssistant(): AssistantEngine = object : AssistantEngine {
        override suspend fun answer(userText: String, history: List<AssistantChatTurn>): AssistantMessage {
            val reply = AssistantKnowledge.answer(userText, "en")
            return AssistantMessage(
                text = reply.text,
                relatedAction = reply.relatedAction,
                source = AssistantSource.Local,
                riskLevel = reply.riskLevel
            )
        }
    }

    private fun orchestrator(
        onlineClient: OnlineAiClient = FailingOnlineClient(),
        remote: AssistantRemoteClient? = null,
        policy: AiRoutingPolicy = offlinePolicy(),
        assistantConfigured: Boolean = true,
        local: AssistantEngine = testLocalAssistant()
    ) = AiOrchestrator(
        offline = OfflineAiAnalysisEngine(),
        online = OnlineAiAnalysisEngine(onlineClient),
        localAssistant = local,
        remoteAssistant = remote,
        policy = policy,
        isOnlineAssistantConfigured = { assistantConfigured }
    )

    // --- Report analysis ---

    @Test
    fun offlineReportAnalysisWhenNoConsent() = runBlocking {
        val result = orchestrator(policy = offlinePolicy()).analyze(fgmInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
        assertEquals("OFFLINE AI", result.sourceLabel)
        assertEquals(AiAnalysisCategory.FGM_THREAT, result.category)
        assertTrue(result.severity.ordinal >= AiSeverity.HIGH.ordinal)
    }

    @Test
    fun onlineReportAnalysisWhenCloudResponds() = runBlocking {
        val result = orchestrator(
            onlineClient = StubOnlineClient(),
            policy = onlinePolicy()
        ).analyze(physicalInput())
        assertEquals(AiAnalysisSource.HYBRID, result.source)
        assertEquals("HYBRID AI", result.sourceLabel)
        assertEquals("test-provider", result.providerId)
        assertTrue(result.consentGranted)
    }

    @Test
    fun onlineReportFailureFallsBackToOffline() = runBlocking {
        val result = orchestrator(
            onlineClient = FailingOnlineClient(),
            policy = onlinePolicy()
        ).analyze(fgmInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
        assertEquals("OFFLINE AI", result.sourceLabel)
        assertTrue(result.indicators.isNotEmpty())
    }

    @Test
    fun consentDeniedSkipsCloudReportRequest() = runBlocking {
        val tracking = TrackingOnlineClient()
        val result = orchestrator(
            onlineClient = tracking,
            policy = onlinePolicy(consent = false)
        ).analyze(fgmInput())
        assertEquals(0, tracking.calls)
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
    }

    @Test
    fun gbvClassificationOffline() = runBlocking {
        val result = orchestrator().analyze(
            AiAnalysisInput("physical", "Physical violence", "He hit me at home", true)
        )
        assertEquals(AiAnalysisCategory.GBV, result.category)
    }

    @Test
    fun fgmClassificationOffline() = runBlocking {
        val result = orchestrator().analyze(
            AiAnalysisInput("fgm_incident", "FGM incident", "She was cut", true)
        )
        assertEquals(AiAnalysisCategory.FGM, result.category)
    }

    @Test
    fun severityCriticalForImminentThreat() = runBlocking {
        val result = orchestrator().analyze(
            AiAnalysisInput(
                "fgm_pressure",
                "FGM pressure",
                "They will cut my daughter tonight with a knife",
                true
            )
        )
        assertEquals(AiSeverity.CRITICAL, result.severity)
    }

    @Test
    fun resolveSourceLabelNeverOnlineWithoutOnlineResults() {
        val offlineOnly = listOf(
            AiAnalysisResult(
                analysisId = "1",
                source = AiAnalysisSource.OFFLINE,
                category = AiAnalysisCategory.GBV,
                severity = AiSeverity.HIGH,
                confidence = 0.7,
                indicators = emptyList(),
                explanation = "x",
                recommendations = emptyList(),
                educationRecommendations = emptyList(),
                timestamp = 1L
            )
        )
        assertEquals("OFFLINE AI", AiOrchestrator.resolveAnalysisSourceLabel(offlineOnly))
    }

    // --- Assistant ---

    @Test
    fun offlineAssistantWithoutConsent() = runBlocking {
        val orch = orchestrator(
            remote = StubRemoteAssistant(),
            policy = offlinePolicy()
        )
        val reply = orch.assistantReply("My husband keeps threatening me").message
        assertEquals(AssistantSource.Local, reply.source)
        assertEquals("OFFLINE AI", reply.source.modeLabel)
    }

    @Test
    fun onlineAssistantWhenRemoteResponds() = runBlocking {
        val orch = orchestrator(
            remote = StubRemoteAssistant(text = "Cloud safety guidance."),
            policy = onlinePolicy()
        )
        val reply = orch.assistantReply("I am scared to go home").message
        assertEquals(AssistantSource.Remote, reply.source)
        assertEquals("ONLINE AI", reply.source.modeLabel)
    }

    @Test
    fun assistantOnlineFailureFallsBackToOffline() = runBlocking {
        val orch = orchestrator(
            remote = FailingRemoteAssistant(),
            policy = onlinePolicy()
        )
        val reply = orch.assistantReply("Where can I report GBV?").message
        assertEquals(AssistantSource.Local, reply.source)
        assertTrue(reply.text.isNotBlank())
    }

    @Test
    fun consentDeniedSkipsCloudAssistantRequest() = runBlocking {
        val tracking = TrackingRemoteAssistant()
        val orch = orchestrator(
            remote = tracking,
            policy = onlinePolicy(consent = false)
        )
        val reply = orch.assistantReply("Help me").message
        assertEquals(0, tracking.calls)
        assertEquals(AssistantSource.Local, reply.source)
    }

    @Test
    fun assistantAvailabilityLabelOfflineWithoutCloud() = runBlocking {
        val label = orchestrator(policy = offlinePolicy()).assistantAvailabilityLabel()
        assertEquals("OFFLINE AI", label)
    }

    @Test
    fun assistantStatusLabelStaysOnlineWhenRemoteFailsButSessionReady() = runBlocking {
        val orch = orchestrator(
            remote = FailingRemoteAssistant(),
            policy = onlinePolicy()
        )
        val reply = orch.assistantReply("Where can I report GBV?")
        assertEquals(AssistantSource.Local, reply.message.source)
        assertEquals(AssistantOnlineBlocker.ONLINE_REQUEST_FAILED, reply.blocker)
        assertEquals("ONLINE AI", orch.assistantStatusLabel())
    }

    @Test
    fun assistantStatusLabelOfflineWhenNotSignedIn() = runBlocking {
        val orch = orchestrator(policy = offlinePolicy())
        assertEquals("OFFLINE AI", orch.assistantStatusLabel())
    }

    @Test
    fun sensitiveDataRedactedBeforeOnlineAnalysis() {
        val raw = "Call me Amina at 0712345678 PIN 1102 amina@test.com"
        val redacted = PrivacyFilter.redactForCloudAnalysis(raw)
        assertFalse(redacted.contains("0712345678"))
        assertFalse(redacted.contains("1102"))
        assertFalse(redacted.contains("amina@test.com"))
        val prepared = PrivacyFilter.prepareOnlineInput(
            AiAnalysisInput("physical", "Physical", raw, includeDescription = true)
        )
        assertFalse(prepared.description.contains("0712345678"))
    }

    private fun fgmInput() = AiAnalysisInput(
        categoryId = "fgm_pressure",
        categoryLabel = "FGM",
        description = "They will cut me tomorrow",
        includeDescription = true
    )

    private fun physicalInput() = AiAnalysisInput(
        categoryId = "physical",
        categoryLabel = "Physical",
        description = "hit me",
        includeDescription = true
    )

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

    private class StubOnlineClient : OnlineAiClient {
        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult =
            AiAnalysisResult(
                analysisId = "online-1",
                source = AiAnalysisSource.ONLINE,
                category = AiAnalysisCategory.GBV,
                severity = AiSeverity.HIGH,
                confidence = 0.9,
                indicators = listOf("Violence"),
                explanation = "Online",
                recommendations = listOf("Safety"),
                educationRecommendations = emptyList(),
                timestamp = System.currentTimeMillis(),
                providerId = "test-provider",
                modelId = "test-model",
                consentGranted = true
            )
        override suspend fun isOnlineAiConfigured(): Boolean = true
    }

    private class StubRemoteAssistant(
        private val text: String = "Online reply"
    ) : AssistantRemoteClient {
        override suspend fun answer(
            userText: String,
            history: List<AssistantChatTurn>
        ): AssistantMessage? = AssistantMessage(
            text = text,
            source = AssistantSource.Remote
        )
    }

    private class FailingRemoteAssistant : AssistantRemoteClient {
        override suspend fun answer(
            userText: String,
            history: List<AssistantChatTurn>
        ): AssistantMessage? = throw RuntimeException("network down")
    }

    private class TrackingRemoteAssistant : AssistantRemoteClient {
        var calls = 0
        override suspend fun answer(
            userText: String,
            history: List<AssistantChatTurn>
        ): AssistantMessage? {
            calls++
            return AssistantMessage("should not be used")
        }
    }
}
