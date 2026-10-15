package com.lindabinti.backend

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lindabinti.backend.ai.AiConfig
import com.lindabinti.backend.ai.AiHttpClient
import com.lindabinti.backend.ai.AiHttpResponse
import io.ktor.client.HttpClient
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

class AssistantChatApiTest {

    private val configuredAi = AiConfig(
        apiKey = "test-key-not-for-android",
        baseUrl = "https://ai.example.test",
        model = "gpt-4o-mini",
        timeoutMs = 800,
        maxRetries = 0,
        enabledFlag = true
    )

    @Test
    fun chatWithoutConsentReturns503() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.registerUser("nochat@example.com")
        val res = client.post("/v1/ai/assistant-chat") {
            header(HttpHeaders.Authorization, "Bearer ${user.get("accessToken").asString}")
            contentType(ContentType.Application.Json)
            setBody("""{"message":"I am scared to go home","cloudAiConsent":false}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("consent_required", json.get("error").asString)
    }

    @Test
    fun authenticatedUserCanChatWithOnlineAssistant() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = configuredAi,
                aiHttpClient = mockAssistantHttp()
            )
        }
        val user = client.registerUser("chat@example.com")
        val res = client.post("/v1/ai/assistant-chat") {
            header(HttpHeaders.Authorization, "Bearer ${user.get("accessToken").asString}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"message":"My husband keeps threatening me","language":"en","cloudAiConsent":true,
                 "history":[{"role":"user","content":"Hello"},{"role":"assistant","content":"How can I help?"}]}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("ONLINE AI", json.get("source").asString)
        assertTrue(json.get("reply").asString.isNotBlank())
        assertFalse(res.bodyAsText().contains("1102"))
    }

    private fun mockAssistantHttp(): AiHttpClient = object : AiHttpClient {
        override fun postJson(
            url: String,
            headers: Map<String, String>,
            body: String,
            timeoutMs: Int
        ): AiHttpResponse = AiHttpResponse(200, chatCompletion(VALID_ASSISTANT_JSON))
    }

    private suspend fun HttpClient.registerUser(email: String): JsonObject {
        val res = post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"Password123!","displayName":"Test"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        return JsonParser.parseString(res.bodyAsText()).asJsonObject
    }

    companion object {
        private val VALID_ASSISTANT_JSON = """
            {
              "reply":"Your safety comes first. If you are in immediate danger, call 999 or 1195. You can also open Emergency Assistance in Linda Binti.",
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
