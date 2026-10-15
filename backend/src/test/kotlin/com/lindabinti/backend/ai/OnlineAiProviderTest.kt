package com.lindabinti.backend.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineAiProviderTest {

    private val sampleInput = PiiMinimizer.toInput(
        categoryId = "fgm_pressure",
        categoryLabel = "FGM threat or pressure",
        body = "Immediate pressure tonight. Call 0712345678",
        includeBody = true
    )

    private val configured = AiConfig(
        apiKey = "server-only-key",
        baseUrl = "https://ai.example.test",
        model = "gpt-4o-mini",
        timeoutMs = 800,
        maxRetries = 1,
        enabledFlag = true
    )

    @Test
    fun parserAcceptsExtendedReportSchema() {
        val parsed = AiReportAnalysisParser.parse(VALID_REPORT_JSON, "FGM threat")
        assertEquals("FGM_THREAT", parsed.category)
        assertEquals("HIGH", parsed.severity)
        assertTrue(parsed.confidence in 0.0..1.0)
        assertTrue(parsed.indicators.isNotEmpty())
        assertTrue(parsed.explanation.isNotBlank())
        assertTrue(parsed.recommendations.isNotEmpty())
        assertTrue(parsed.educationRecommendations.isNotEmpty())
    }

    @Test(expected = InvalidAiResultException::class)
    fun parserRejectsMalformedJson() {
        AiReportAnalysisParser.parse("not-json", "test")
    }

    @Test(expected = InvalidAiResultException::class)
    fun parserRejectsMissingExplanation() {
        AiReportAnalysisParser.parse("""{"category":"GBV","severity":"HIGH"}""", "test")
    }

    @Test
    fun providerReturnsStructuredOnlineResult() {
        val http = ScriptedHttpClient(AiHttpResponse(200, chatCompletion(VALID_REPORT_JSON)))
        val provider = OpenAiCompatibleProvider(configured, http)
        val result = provider.analyzeReport(sampleInput)
        assertEquals("ONLINE", result.source)
        assertEquals("FGM_THREAT", result.category)
        assertEquals("openai-compatible", result.providerId)
        assertEquals("gpt-4o-mini", result.modelId)
        assertEquals(AiStatus.COMPLETE, result.aiStatus)
        assertFalse(http.lastBody.contains("0712345678"))
    }

    @Test
    fun providerRetriesAfterTimeoutThenSucceeds() {
        val http = ScriptedHttpClient(
            listOf(
                { throw java.net.SocketTimeoutException("timeout") },
                { AiHttpResponse(200, chatCompletion(VALID_REPORT_JSON)) }
            )
        )
        val provider = OpenAiCompatibleProvider(configured, http)
        val result = provider.analyzeReport(sampleInput)
        assertEquals("FGM_THREAT", result.category)
        assertEquals(2, http.calls)
    }

    @Test(expected = AiProviderException::class)
    fun providerFailsOnMalformedResponseWithoutRetry() {
        val http = ScriptedHttpClient(AiHttpResponse(200, chatCompletion("""{"severity":"HIGH"}""")))
        OpenAiCompatibleProvider(configured, http).analyzeReport(sampleInput)
    }

    @Test
    fun missingApiKeyThrowsProviderException() {
        val config = configured.copy(apiKey = "")
        try {
            OpenAiCompatibleProvider(config, ScriptedHttpClient(AiHttpResponse(200, "{}")))
                .analyzeReport(sampleInput)
            throw AssertionError("Expected AiProviderException")
        } catch (e: AiProviderException) {
            assertEquals("api_key_missing", e.code)
        }
    }

    @Test
    fun disabledAiThrowsProviderException() {
        val config = configured.copy(enabledFlag = false)
        try {
            OpenAiCompatibleProvider(config, ScriptedHttpClient(AiHttpResponse(200, "{}")))
                .analyzeReport(sampleInput)
            throw AssertionError("Expected AiProviderException")
        } catch (e: AiProviderException) {
            assertEquals("ai_disabled", e.code)
        }
    }

    @Test
    fun invalidApiKeyHttp401() {
        val http = ScriptedHttpClient(AiHttpResponse(401, """{"error":"invalid_api_key"}"""))
        try {
            OpenAiCompatibleProvider(configured.copy(maxRetries = 0), http).analyzeReport(sampleInput)
            throw AssertionError("Expected AiProviderException")
        } catch (e: AiProviderException) {
            assertEquals("invalid_api_key", e.code)
        }
    }

    @Test
    fun providerUnavailableAfterHttpError() {
        val http = ScriptedHttpClient(AiHttpResponse(503, """{"error":"busy"}"""))
        try {
            OpenAiCompatibleProvider(configured.copy(maxRetries = 0), http).analyzeReport(sampleInput)
            throw AssertionError("Expected AiProviderException")
        } catch (e: AiProviderException) {
            assertEquals("provider_unavailable", e.code)
        }
    }

    private class ScriptedHttpClient(
        private val scripts: List<() -> AiHttpResponse>
    ) : AiHttpClient {
        constructor(response: AiHttpResponse) : this(listOf({ response }))

        var calls = 0
        var lastBody: String = ""

        override fun postJson(
            url: String,
            headers: Map<String, String>,
            body: String,
            timeoutMs: Int
        ): AiHttpResponse {
            lastBody = body
            val script = scripts.getOrNull(calls) ?: scripts.last()
            calls++
            return script()
        }
    }

    companion object {
        private val VALID_REPORT_JSON = """
            {
              "category":"FGM_THREAT",
              "severity":"HIGH",
              "confidence":0.86,
              "urgencyIndicators":["immediate pressure/threat of FGM"],
              "safetyConcerns":["possible lack of safe protection"],
              "explanation":"Report indicates immediate pressure/threat of FGM and possible lack of safe protection.",
              "recommendations":["Seek immediate protection for the person at risk."],
              "educationRecommendations":["fgm","child_protection"]
            }
        """.trimIndent()

        private fun chatCompletion(content: String): String {
            val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            return """{"choices":[{"message":{"content":"$escaped"}}]}"""
        }
    }
}
