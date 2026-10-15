package com.lindabinti.backend

import com.google.gson.JsonParser
import com.lindabinti.backend.ai.AiConfig
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

class NgoIntelligenceTest {

    @Test
    fun userCannotAccessIntelligenceEndpoint() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("intel-user@example.com", "Password123!", "User")
        val res = client.get("/v1/dashboard/intelligence") {
            header(HttpHeaders.Authorization, user.bearer())
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun ngoStaffReceivesIntelligenceWithoutPrivateFields() = testApplication {
        application { moduleForTests(seedAdmin = true, minThreshold = 3) }
        val now = System.currentTimeMillis()
        val user = client.register("survivor-intel@example.com", "Password123!", "Amina")
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "intel-case-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"intel-case-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"private survivor narrative with phone 0712345678","createdAtEpochMs":$now,
                 "includeBody":true,"areaCode":"kajiado_north","areaLabel":"Kajiado North","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        client.post("/v1/admin/staff") {
            header(HttpHeaders.Authorization, admin.bearer())
            contentType(ContentType.Application.Json)
            setBody("""{"email":"intel-staff@ngo.local","password":"StaffPass1!","role":"NGO_STAFF"}""")
        }
        val staff = client.login("intel-staff@ngo.local", "StaffPass1!")

        val res = client.get("/v1/dashboard/intelligence?periodDays=30") {
            header(HttpHeaders.Authorization, staff.bearer())
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        val json = JsonParser.parseString(body).asJsonObject

        assertTrue(json.get("dashboardTitle").asString.contains("AI SAFETY & RESPONSE INTELLIGENCE"))
        assertTrue(json.has("commandSummary"))
        assertTrue(json.has("priorityQueue"))
        assertTrue(json.has("aiEngine"))
        assertTrue(json.has("priorityCases"))
        assertTrue(json.has("severityDisplay"))
        assertTrue(json.has("privacyIndicator"))
        assertTrue(json.has("aiDisclaimer"))
        assertFalse(body.contains("private survivor narrative"))
        assertFalse(body.contains("0712345678"))
        assertFalse(body.contains("survivor-intel@example.com"))

        assertTrue(json.has("caseFlags"))
        assertTrue(json.has("trendComparison"))
        assertTrue(json.has("aiSafetySectionTitle"))
        assertEquals("AI Safety Intelligence", json.get("aiSafetySectionTitle").asString)

        val engine = json.getAsJsonObject("aiEngine")
        assertEquals("OFFLINE", engine.get("mode").asString)
        assertEquals("NOT CONFIGURED", engine.get("statusLabel").asString)
        assertEquals("RULE-BASED AI", engine.get("activeSourceLabel").asString)

        val queue = json.getAsJsonArray("priorityQueue")
        assertTrue(queue.size() >= 1)
        val first = queue[0].asJsonObject
        assertTrue(first.has("confidencePercent"))
        assertTrue(first.has("whyFlagged"))
        assertTrue(first.has("indicators"))
        assertTrue(first.has("sourceLabel"))
        assertTrue(first.has("aiStatusLabel"))
        assertTrue(first.has("scope"))
        assertTrue(first.has("incidentType"))
        assertEquals("RULE-BASED AI", first.get("sourceLabel").asString)
    }

    @Test
    fun mapGeoAreasFlagsSingleReportedSubCounty() = testApplication {
        application { moduleForTests(seedAdmin = true, minThreshold = 3) }
        val now = System.currentTimeMillis()
        val user = client.register("map-flag@example.com", "Password123!", "Reporter")
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "map-flag-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"map-flag-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"private details","createdAtEpochMs":$now,"includeBody":true,
                 "areaCode":"kajiado_east","areaLabel":"Kajiado East","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val intel = client.get("/v1/dashboard/intelligence?periodDays=30") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, intel.status)
        val json = JsonParser.parseString(intel.bodyAsText()).asJsonObject
        val mapAreas = json.getAsJsonArray("mapGeoAreas")
        assertTrue(mapAreas.size() >= 1)
        val east = mapAreas.first { el ->
            el.asJsonObject.get("areaCode").asString == "kajiado_east"
        }.asJsonObject
        assertEquals(1, east.get("totalCount").asInt)
        assertFalse(east.get("suppressed").asBoolean)
        assertEquals("ELEVATED", east.get("hotspotLevel").asString)
    }

    @Test
    fun sosWithAreaCountsTowardAreaHotspotAndIsCritical() = testApplication {
        application { moduleForTests(seedAdmin = true, minThreshold = 1) }
        val now = System.currentTimeMillis()
        val user = client.register("sos-area@example.com", "Password123!", "Reporter")
        // Seed volume so East becomes HIGH by count.
        repeat(3) { i ->
            client.post("/v1/reports") {
                header(HttpHeaders.Authorization, user.bearer())
                header("Idempotency-Key", "sos-vol-$i")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {"clientUuid":"sos-vol-$i","categoryId":"domestic","categoryLabel":"Domestic",
                     "body":"","createdAtEpochMs":$now,"includeBody":false,
                     "areaCode":"kajiado_east","areaLabel":"Kajiado East","areaType":"SUB_COUNTY"}
                    """.trimIndent()
                )
            }
        }
        val sos = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "sos-live-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"sos-live-1","categoryId":"sos","categoryLabel":"SOS alert",
                 "body":"SOS activated","createdAtEpochMs":$now,"includeBody":true,
                 "areaCode":"kajiado_east","areaLabel":"Kajiado East","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, sos.status)

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val intel = client.get("/v1/dashboard/intelligence?periodDays=30") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, intel.status)
        val json = JsonParser.parseString(intel.bodyAsText()).asJsonObject
        val east = json.getAsJsonArray("mapGeoAreas").first { el ->
            el.asJsonObject.get("areaCode").asString == "kajiado_east"
        }.asJsonObject
        assertTrue(east.get("totalCount").asInt >= 4)
        assertTrue(east.get("sosCount").asInt >= 1)
        assertEquals("HIGH", east.get("hotspotLevel").asString)

        val cases = json.getAsJsonArray("caseFlags")
        val sosFlag = cases.first { el ->
            el.asJsonObject.get("categoryId").asString == "sos"
        }.asJsonObject
        assertEquals("CRITICAL", sosFlag.get("severity").asString.uppercase())
        assertEquals("kajiado_east", sosFlag.get("areaCode").asString)
    }

    @Test
    fun trendInsightsRequireMinimumThreshold() = testApplication {
        application { moduleForTests(seedAdmin = true, minThreshold = 3) }
        val user = client.register("low-data@example.com", "Password123!", "Reporter")
        val bearer = user.bearer()
        val now = System.currentTimeMillis()
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, bearer)
            header("Idempotency-Key", "low-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"low-1","categoryId":"physical","categoryLabel":"Physical violence",
                 "body":"need support","createdAtEpochMs":$now,"includeBody":true}
                """.trimIndent()
            )
        }

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val intel = client.get("/v1/dashboard/intelligence?periodDays=30") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, intel.status)
        val json = JsonParser.parseString(intel.bodyAsText()).asJsonObject
        assertTrue(json.getAsJsonArray("trendInsights").size() == 0)
        assertTrue(json.get("geoInsufficient").asBoolean)
    }

    @Test
    fun intelligenceAuditLoggedForPrivilegedRead() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        client.get("/v1/dashboard/intelligence?periodDays=7") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        val audit = client.get("/v1/admin/audit") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertTrue(audit.bodyAsText().contains("NGO_INTELLIGENCE_READ"))
    }

    @Test
    fun onlineAiEngineStatusWhenConfigured() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = AiConfig("key", "https://api.example.com/v1", "gpt-4o-mini", 8000, 1, true)
            )
        }
        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val res = client.get("/v1/dashboard/intelligence") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val engine = JsonParser.parseString(res.bodyAsText()).asJsonObject.getAsJsonObject("aiEngine")
        assertEquals("ONLINE", engine.get("mode").asString)
        assertEquals("WORKING", engine.get("statusLabel").asString)
        assertTrue(engine.get("onlineConfigured").asBoolean)
    }

    @Test
    fun degradedHealthWhenOnlineButRulesFallbackInUse() = testApplication {
        application {
            moduleForTests(
                seedAdmin = true,
                aiConfig = AiConfig("key", "https://api.example.com/v1", "gpt-4o-mini", 8000, 1, true)
            )
        }
        val now = System.currentTimeMillis()
        val user = client.register("degraded@example.com", "Password123!", "Reporter")
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "deg-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"deg-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"report text","createdAtEpochMs":$now,"includeBody":true}
                """.trimIndent()
            )
        }
        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val res = client.get("/v1/dashboard/intelligence") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        val engine = JsonParser.parseString(res.bodyAsText()).asJsonObject.getAsJsonObject("aiEngine")
        assertEquals("DEGRADED", engine.get("health").asString)
        assertEquals("FALLBACK", engine.get("statusLabel").asString)
        assertTrue(engine.get("degradedReason").asString.isNotBlank())
    }

    @Test
    fun flagDtoIncludesConfidenceAndIndicators() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("flag-dto@example.com", "Password123!", "Reporter")
        val now = System.currentTimeMillis()
        client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "flag-dto-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"flag-dto-1","categoryId":"domestic","categoryLabel":"Domestic violence",
                 "body":"threat at home","createdAtEpochMs":$now,"includeBody":true}
                """.trimIndent()
            )
        }
        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val flags = client.get("/v1/reports/flags") {
            header(HttpHeaders.Authorization, admin.bearer())
        }
        val json = JsonParser.parseString(flags.bodyAsText()).asJsonArray[0].asJsonObject
        assertTrue(json.has("confidencePercent"))
        assertTrue(json.get("confidencePercent").asInt > 0)
        assertTrue(json.has("indicators"))
        assertTrue(json.has("whyFlagged"))
        assertTrue(json.has("severity"))
    }

    private suspend fun io.ktor.client.HttpClient.register(email: String, password: String, name: String) =
        com.google.gson.JsonParser.parseString(
            post("/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$email","password":"$password","displayName":"$name"}""")
            }.bodyAsText()
        ).asJsonObject

    private suspend fun io.ktor.client.HttpClient.login(email: String, password: String) =
        com.google.gson.JsonParser.parseString(
            post("/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"$email","password":"$password"}""")
            }.bodyAsText()
        ).asJsonObject

    private fun com.google.gson.JsonObject.bearer(): String = "Bearer " + get("accessToken").asString
}
