package com.lindabinti.backend

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiHttpClient
import com.lindabinti.backend.ai.AiHttpResponse
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
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAnalyzeReportApiTest {

    private val configuredAi = AiConfig(
        apiKey = "test-key-not-for-android",
        baseUrl = "https://ai.example.test",
        model = "gpt-4o-mini",
        timeoutMs = 800,
        maxRetries = 0,
        enabledFlag = true
    )

    @Test
    fun capabilitiesReflectsOnlineState() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi,
                aiHttpClient = mockAiHttp()
            )
        }
        val res = client.get("/v1/ai/capabilities")
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertTrue(json.get("onlineEnabled").asBoolean)
        assertEquals("ONLINE AI: CONFIGURED", json.get("statusLabel").asString)
        assertEquals("openai-compatible", json.get("providerId").asString)
    }

    @Test
    fun analyzeWithoutConsentReturns503WithFallback() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.registerUser("noconsent@example.com")
        val bearer = "Bearer ${user.get("accessToken").asString}"

        val res = client.post("/v1/ai/analyze-report") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"categoryId":"fgm_pressure","categoryLabel":"FGM pressure",
                 "description":"They will cut my sister tomorrow","includeDescription":true,
                 "cloudAiConsent":false}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("consent_required", json.get("error").asString)
        assertTrue(json.get("fallbackRecommended").asBoolean)
    }

    @Test
    fun analyzeWhenAiDisabledReturns503WithFallback() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.registerUser("aidisabled@example.com")
        val bearer = "Bearer ${user.get("accessToken").asString}"

        val res = client.post("/v1/ai/analyze-report") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"categoryId":"fgm_pressure","categoryLabel":"FGM pressure",
                 "description":"They will cut my sister tomorrow","includeDescription":true,
                 "cloudAiConsent":true}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertTrue(json.get("fallbackRecommended").asBoolean)
    }

    @Test
    fun authenticatedUserCanAnalyzeReportWithOnlineProvider() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi,
                aiHttpClient = mockAiHttp()
            )
        }
        val user = client.registerUser("analyze@example.com")
        val bearer = "Bearer ${user.get("accessToken").asString}"

        val res = client.post("/v1/ai/analyze-report") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"categoryId":"fgm_pressure","categoryLabel":"FGM pressure",
                 "description":"They will cut my sister tomorrow","includeDescription":true,
                 "cloudAiConsent":true}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("ONLINE AI", json.get("source").asString)
        assertEquals("FGM_THREAT", json.get("category").asString)
        assertTrue(json.get("explanation").asString.isNotBlank())
        assertFalse(res.bodyAsText().contains("analyze@example.com"))
    }

    @Test
    fun ngoRoleCannotUseSurvivorAnalyzeEndpoint() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val admin = client.loginAdmin()
        val res = client.post("/v1/ai/analyze-report") {
            header(HttpHeaders.Authorization, "Bearer ${admin.get("accessToken").asString}")
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"physical","categoryLabel":"Physical","description":"test","cloudAiConsent":true}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun unauthenticatedAnalyzeReturnsUnauthorized() = testApplication {
        application { moduleForTests() }
        val res = client.post("/v1/ai/analyze-report") {
            contentType(ContentType.Application.Json)
            setBody("""{"categoryId":"physical","categoryLabel":"Physical","description":"test"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    private fun mockAiHttp(): AiHttpClient = object : AiHttpClient {
        override fun postJson(
            url: String,
            headers: Map<String, String>,
            body: String,
            timeoutMs: Int
        ): AiHttpResponse = AiHttpResponse(200, chatCompletion(VALID_REPORT_JSON))
    }

    private suspend fun HttpClient.registerUser(email: String): JsonObject {
        val res = post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"Password123!","displayName":"Test"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        return JsonParser.parseString(res.bodyAsText()).asJsonObject
    }

    private suspend fun HttpClient.loginAdmin(): JsonObject {
        val res = post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        return JsonParser.parseString(res.bodyAsText()).asJsonObject
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
