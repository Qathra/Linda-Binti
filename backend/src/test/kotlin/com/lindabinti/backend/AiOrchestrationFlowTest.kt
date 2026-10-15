package com.lindabinti.backend

import com.google.gson.JsonParser
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

class AiOrchestrationFlowTest {

    @Test
    fun reportSyncToNgoFlagToIntelligenceFlow() = testApplication {
        application { moduleForTests(seedAdmin = true, minThreshold = 1) }
        val now = System.currentTimeMillis()
        val user = client.register("flow-user@example.com", "Password123!", "Survivor")
        val submit = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "flow-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"flow-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"private survivor narrative","createdAtEpochMs":$now,"includeBody":true,
                 "areaCode":"kajiado_north","areaLabel":"Kajiado North","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, submit.status)

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val flags = client.get("/v1/reports/flags") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, flags.status)
        assertFalse(flags.bodyAsText().contains("private survivor narrative"))

        val intel = client.get("/v1/dashboard/intelligence?periodDays=30") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, intel.status)
        val intelJson = JsonParser.parseString(intel.bodyAsText()).asJsonObject
        assertTrue(intelJson.getAsJsonObject("commandSummary").get("totalReports").asInt >= 1)

        val blocked = client.get("/v1/dashboard/intelligence") {
            header(HttpHeaders.Authorization, user.bearer())
        }
        assertEquals(HttpStatusCode.Forbidden, blocked.status)
    }

    @Test
    fun survivorAnalyzeRequiresConsent() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("ai-user@example.com", "Password123!", "Survivor")
        val res = client.post("/v1/ai/analyze-report") {
            header(HttpHeaders.Authorization, user.bearer())
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"categoryId":"physical","categoryLabel":"Physical violence",
                 "description":"need help","includeDescription":true,"cloudAiConsent":false}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertTrue(res.bodyAsText().contains("consent_required"))
    }

    private suspend fun io.ktor.client.HttpClient.register(email: String, password: String, name: String) =
        JsonParser.parseString(
            post("/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$email","password":"$password","displayName":"$name"}""")
            }.bodyAsText()
        ).asJsonObject

    private suspend fun io.ktor.client.HttpClient.login(email: String, password: String) =
        JsonParser.parseString(
            post("/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$email","password":"$password"}""")
            }.bodyAsText()
        ).asJsonObject

    private fun com.google.gson.JsonObject.bearer(): String = "Bearer " + get("accessToken").asString
}
