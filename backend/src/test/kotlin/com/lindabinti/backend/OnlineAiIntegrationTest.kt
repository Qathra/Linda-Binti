package com.lindabinti.backend

import com.google.gson.JsonParser
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiHttpClient
import com.lindabinti.backend.ai.AiHttpResponse
import com.lindabinti.backend.ai.AiProviderException
import com.lindabinti.backend.ai.AiStatus
import com.lindabinti.backend.ai.OpenAiCompatibleProvider
import com.lindabinti.backend.ai.PiiMinimizer
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end online AI integration tests using scripted HTTP (not mocks in business logic).
 * Covers survivor analyze, assistant chat, NGO flagging, consent, privacy, and fallbacks.
 */
class OnlineAiIntegrationTest {

    private val configuredAi = AiConfig(
        apiKey = "server-only-test-key",
        baseUrl = "https://ai.example.test",
        model = "gpt-4o-mini",
        timeoutMs = 800,
        maxRetries = 0,
        enabledFlag = true
    )

    private val disabledAi = AiConfig("", "", "gpt-4o-mini", 8000, 0, false)

    @Test
    fun healthShowsOnlineAiStatusWithoutKey() = testApplication {
        application { moduleForTests(seedAdmin = true, aiConfig = configuredAi) }
        val res = client.get("/v1/health")
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("ONLINE AI: CONFIGURED", json.get("onlineAiStatus").asString)
        assertFalse(res.bodyAsText().contains("server-only-test-key"))
    }

    @Test
    fun healthShowsNotConfiguredWhenDisabled() = testApplication {
        application { moduleForTests(seedAdmin = true, aiConfig = disabledAi) }
        val json = JsonParser.parseString(client.get("/v1/health").bodyAsText()).asJsonObject
        assertEquals("ONLINE AI: NOT CONFIGURED", json.get("onlineAiStatus").asString)
    }

    @Test
    fun capabilitiesReflectConfiguredProvider() = testApplication {
        application {
            moduleForTests(seedAdmin = true, aiConfig = configuredAi, aiHttpClient = successHttp())
        }
        val json = JsonParser.parseString(client.get("/v1/ai/capabilities").bodyAsText()).asJsonObject
        assertTrue(json.get("onlineEnabled").asBoolean)
        assertEquals("ONLINE AI: CONFIGURED", json.get("statusLabel").asString)
        assertEquals("openai-compatible", json.get("providerId").asString)
        assertTrue(json.get("configurationReason").asString.contains("gpt-4o-mini"))
    }

    @Test
    fun survivorAnalyzeReturnsOnlineResult() = testApplication {
        application {
            moduleForTests(seedAdmin = true, aiConfig = configuredAi, aiHttpClient = successHttp())
        }
        val user = client.registerUser("survivor-online@example.com")
        val res = client.analyzeReport(user, consent = true, categoryId = "fgm_pressure", body = "They will cut my sister")
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("ONLINE AI", json.get("source").asString)
        assertEquals("FGM_THREAT", json.get("category").asString)
        assertEquals(AiStatus.COMPLETE, json.get("aiStatus").asString)
    }

    @Test
    fun gbvClassificationViaOnlineProvider() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi,
                aiHttpClient = successHttp(gbvJson())
            )
        }
        val user = client.registerUser("gbv-online@example.com")
        val json = JsonParser.parseString(
            client.analyzeReport(user, true, "physical", "He hit me at home").bodyAsText()
        ).asJsonObject
        assertEquals("GBV", json.get("category").asString)
    }

    @Test
    fun fgmClassificationViaOnlineProvider() = testApplication {
        application {
            moduleForTests(seedAdmin = true, aiConfig = configuredAi, aiHttpClient = successHttp())
        }
        val user = client.registerUser("fgm-online@example.com")
        val json = JsonParser.parseString(
            client.analyzeReport(user, true, "fgm_incident", "She was cut yesterday").bodyAsText()
        ).asJsonObject
        assertEquals("FGM_THREAT", json.get("category").asString)
    }

    @Test
    fun consentDeniedBlocksCloudAnalyze() = testApplication {
        application {
            moduleForTests(seedAdmin = true, aiConfig = configuredAi, aiHttpClient = trackingHttp())
        }
        val user = client.registerUser("noconsent2@example.com")
        val res = client.analyzeReport(user, consent = false, categoryId = "physical", body = "help")
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertEquals("consent_required", JsonParser.parseString(res.bodyAsText()).asJsonObject.get("error").asString)
    }

    @Test
    fun privacyRedactsPhoneBeforeProviderCall() = testApplication {
        val tracking = trackingHttp()
        application {
            moduleForTests(seedAdmin = true, aiConfig = configuredAi, aiHttpClient = tracking)
        }
        val user = client.registerUser("privacy@example.com")
        client.analyzeReport(user, true, "physical", "Call me Amina at 0712345678 PIN 1102 amina@test.com")
        assertFalse(tracking.lastBody.contains("0712345678"))
        assertFalse(tracking.lastBody.contains("1102"))
        assertFalse(tracking.lastBody.contains("amina@test.com"))
    }

    @Test
    fun providerTimeoutReturnsFallbackRecommendation() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi.copy(maxRetries = 0),
                aiHttpClient = timeoutHttp()
            )
        }
        val user = client.registerUser("timeout@example.com")
        val res = client.analyzeReport(user, true, "physical", "threat at home")
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("timeout", json.get("error").asString)
        assertTrue(json.get("fallbackRecommended").asBoolean)
    }

    @Test
    fun invalidApiKeyReturnsExplicitError() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi.copy(maxRetries = 0),
                aiHttpClient = object : AiHttpClient {
                    override fun postJson(
                        url: String,
                        headers: Map<String, String>,
                        body: String,
                        timeoutMs: Int
                    ) = AiHttpResponse(401, """{"error":"invalid_api_key"}""")
                }
            )
        }
        val user = client.registerUser("badkey@example.com")
        val res = client.analyzeReport(user, true, "physical", "help")
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertEquals(
            "invalid_api_key",
            JsonParser.parseString(res.bodyAsText()).asJsonObject.get("error").asString
        )
    }

    @Test
    fun offlineFallbackWhenAiDisabled() = testApplication {
        application { moduleForTests(seedAdmin = true, aiConfig = disabledAi) }
        val user = client.registerUser("offline@example.com")
        val res = client.analyzeReport(user, true, "fgm_pressure", "pressure to cut")
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertTrue(json.get("fallbackRecommended").asBoolean)
        assertFalse(json.has("source"))
    }

    @Test
    fun assistantChatOnlineFlow() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi,
                aiHttpClient = assistantHttp()
            )
        }
        val user = client.registerUser("assistant@example.com")
        val res = client.post("/v1/ai/assistant-chat") {
            header(HttpHeaders.Authorization, user.bearer())
            contentType(ContentType.Application.Json)
            setBody("""{"message":"I am scared to go home","language":"en","cloudAiConsent":true}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("ONLINE AI", json.get("source").asString)
        assertTrue(json.get("reply").asString.isNotBlank())
    }

    @Test
    fun reportToNgoFlagToIntelligenceWithOnlineAi() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                minThreshold = 1,
                aiConfig = configuredAi,
                aiHttpClient = ngoFlagHttp()
            )
        }
        val now = System.currentTimeMillis()
        val user = client.registerUser("ngo-flow@example.com")
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "ngo-online-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"ngo-online-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"private survivor narrative 0712345678","createdAtEpochMs":$now,"includeBody":true}
                """.trimIndent()
            )
        }
        val admin = client.loginAdmin()
        val flags = JsonParser.parseString(
            client.get("/v1/reports/flags") { header(HttpHeaders.Authorization, admin.bearer()) }.bodyAsText()
        ).asJsonArray
        assertTrue(flags.size() >= 1)
        assertFalse(flags.toString().contains("private survivor narrative"))
        assertFalse(flags.toString().contains("0712345678"))

        val intel = JsonParser.parseString(
            client.get("/v1/dashboard/intelligence?periodDays=30") {
                header(HttpHeaders.Authorization, admin.bearer())
            }.bodyAsText()
        ).asJsonObject
        assertTrue(intel.getAsJsonArray("priorityQueue").size() >= 1)
    }

    @Test
    fun openAiCompatibleProviderInvalidKeyThrows() {
        val http = object : AiHttpClient {
            override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int) =
                AiHttpResponse(401, """{"error":"invalid_api_key"}""")
        }
        try {
            OpenAiCompatibleProvider(configuredAi.copy(maxRetries = 0), http).analyzeReport(sampleInput())
            throw AssertionError("Expected AiProviderException")
        } catch (e: AiProviderException) {
            assertEquals("invalid_api_key", e.code)
        }
        assertFalse(http.toString().contains("server-only-test-key"))
    }

    private fun sampleInput() = PiiMinimizer.toInput(
        "fgm_pressure", "FGM pressure", "They will cut my daughter", true
    )

    private suspend fun HttpClient.registerUser(email: String) = JsonParser.parseString(
        post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"Password123!","displayName":"Test"}""")
        }.bodyAsText()
    ).asJsonObject

    private suspend fun HttpClient.loginAdmin() = JsonParser.parseString(
        post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }.bodyAsText()
    ).asJsonObject

    private suspend fun HttpClient.analyzeReport(
        user: com.google.gson.JsonObject,
        consent: Boolean,
        categoryId: String,
        body: String
    ) = post("/v1/ai/analyze-report") {
        header(HttpHeaders.Authorization, user.bearer())
        contentType(ContentType.Application.Json)
        setBody(
            """
            {"categoryId":"$categoryId","categoryLabel":"Test category",
             "description":"$body","includeDescription":true,"cloudAiConsent":$consent}
            """.trimIndent()
        )
    }

    private fun com.google.gson.JsonObject.bearer() = "Bearer ${get("accessToken").asString}"

    private fun successHttp(reportJson: String = FGM_REPORT_JSON) = object : TrackingHttp() {
        override fun respond(): AiHttpResponse = AiHttpResponse(200, chatCompletion(reportJson))
    }

    private fun gbvJson() = """
        {
          "category":"GBV",
          "severity":"HIGH",
          "confidence":0.84,
          "urgencyIndicators":["physical violence"],
          "safetyConcerns":["immediate harm"],
          "explanation":"Indicators of physical GBV requiring prioritized response.",
          "recommendations":["Reach a safe place if possible."],
          "educationRecommendations":["gbv_basics"]
        }
    """.trimIndent()

    private fun ngoFlagHttp() = object : TrackingHttp() {
        override fun respond(): AiHttpResponse = AiHttpResponse(200, chatCompletion(NGO_FLAG_JSON))
    }

    private fun assistantHttp() = object : TrackingHttp() {
        override fun respond(): AiHttpResponse = AiHttpResponse(200, chatCompletion(ASSISTANT_JSON))
    }

    private fun timeoutHttp() = object : AiHttpClient {
        override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
            throw java.net.SocketTimeoutException("timeout")
        }
    }

    private fun trackingHttp() = successHttp()

    private open class TrackingHttp : AiHttpClient {
        var lastBody: String = ""
        open fun respond(): AiHttpResponse = AiHttpResponse(200, chatCompletion(FGM_REPORT_JSON))
        override fun postJson(url: String, headers: Map<String, String>, body: String, timeoutMs: Int): AiHttpResponse {
            lastBody = body
            return respond()
        }
    }

    companion object {
        private val FGM_REPORT_JSON = """
            {
              "category":"FGM_THREAT",
              "severity":"HIGH",
              "confidence":0.86,
              "urgencyIndicators":["imminent FGM pressure"],
              "safetyConcerns":["lack of safe protection"],
              "explanation":"Report indicates immediate FGM pressure.",
              "recommendations":["Seek immediate protection."],
              "educationRecommendations":["fgm"]
            }
        """.trimIndent()

        private val NGO_FLAG_JSON = """
            {
              "riskLevel":"HIGH",
              "gbvFgmRelevance":"FGM",
              "incidentCategory":"fgm_pressure",
              "urgencyIndicators":["FGM pressure"],
              "safetyConcerns":["child protection"],
              "recommendedPriority":"HIGH",
              "explanation":"FGM pressure indicators present."
            }
        """.trimIndent()

        private val ASSISTANT_JSON = """
            {
              "reply":"Your safety comes first. If you are in immediate danger, call 999 or 1195.",
              "relatedAction":"emergency",
              "riskLevel":"HIGH"
            }
        """.trimIndent()

        private fun chatCompletion(content: String): String {
            val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            return """{"choices":[{"message":{"content":"$escaped"}}]}"""
        }
    }
}
