package com.example.lindabinti.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridAiRouterTest {

    private val offline = OfflineAiAnalysisEngine()

    @Test
    fun usesOfflineWhenCloudConsentDisabled() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.valid()),
            policy = policy(consent = false, onlineConfigured = true)
        )
        val result = router.analyze(sampleInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
    }

    @Test
    fun usesOfflineWhenNetworkUnavailable() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.valid()),
            policy = policy(consent = true, network = false, onlineConfigured = true)
        )
        val result = router.analyze(sampleInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
    }

    @Test
    fun usesOnlineWhenAvailableAndConsented() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.valid()),
            policy = policy(consent = true, onlineConfigured = true)
        )
        val result = router.analyze(sampleInput())
        assertEquals(AiAnalysisSource.ONLINE, result.source)
        assertTrue(result.consentGranted)
    }

    @Test
    fun fallsBackToOfflineWhenOnlineFails() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.failing()),
            policy = policy(consent = true, onlineConfigured = true)
        )
        val result = router.analyze(sampleInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
        assertEquals("OFFLINE AI", result.sourceLabel)
    }

    @Test
    fun fallsBackToOfflineOnMalformedOnlineResponse() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.malformed()),
            policy = policy(consent = true, onlineConfigured = true)
        )
        val result = router.analyze(sampleInput())
        assertEquals(AiAnalysisSource.OFFLINE, result.source)
    }

    @Test
    fun neverReturnsWithoutAnalysisWhenOnlineUnavailable() = runBlocking {
        val router = HybridAiRouter(
            offline = offline,
            online = OnlineAiAnalysisEngine(FakeOnlineClient.failing()),
            policy = policy(consent = false, onlineConfigured = false)
        )
        val result = router.analyze(sampleInput())
        assertTrue(result.explanation.isNotBlank())
        assertTrue(result.recommendations.isNotEmpty())
    }

    private fun sampleInput() = AiAnalysisInput(
        categoryId = "physical",
        categoryLabel = "Physical violence",
        description = "He threatened me with a knife tonight"
    )

    private fun policy(
        consent: Boolean,
        network: Boolean = true,
        onlineConfigured: Boolean = false,
        session: Boolean = true,
        backend: Boolean = true
    ) = AiRoutingPolicy(
        isBackendConfigured = { backend },
        isNetworkAvailable = { network },
        hasCloudAiConsent = { consent },
        isUserSessionActive = { session },
        isOnlineAiConfigured = { onlineConfigured }
    )

    private class FakeOnlineClient private constructor(
        private val mode: String
    ) : OnlineAiClient {
        override suspend fun isOnlineAiConfigured(): Boolean = mode != "unconfigured"

        override suspend fun analyzeReport(input: AiAnalysisInput): AiAnalysisResult = when (mode) {
            "valid" -> AiAnalysisResult(
                analysisId = "online-1",
                source = AiAnalysisSource.ONLINE,
                category = AiAnalysisCategory.GBV,
                severity = AiSeverity.HIGH,
                confidence = 0.82,
                indicators = listOf("Online indicator"),
                explanation = "Online AI Safety Analysis: elevated GBV priority from cloud provider.",
                recommendations = listOf("Contact a trusted support service."),
                educationRecommendations = listOf("gbv_basics"),
                timestamp = System.currentTimeMillis(),
                providerId = "test-provider",
                modelId = "test-model",
                processingDurationMs = 12L,
                consentGranted = true
            )
            "malformed" -> AiAnalysisResult(
                analysisId = "bad-1",
                source = AiAnalysisSource.ONLINE,
                category = AiAnalysisCategory.GBV,
                severity = AiSeverity.HIGH,
                confidence = 0.5,
                indicators = emptyList(),
                explanation = "",
                recommendations = emptyList(),
                educationRecommendations = emptyList(),
                timestamp = System.currentTimeMillis()
            )
            else -> throw AiAnalysisException("Online AI unavailable")
        }

        companion object {
            fun valid() = FakeOnlineClient("valid")
            fun failing() = FakeOnlineClient("fail")
            fun malformed() = FakeOnlineClient("malformed")
        }
    }
}
