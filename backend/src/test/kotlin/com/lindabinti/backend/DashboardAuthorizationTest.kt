package com.lindabinti.backend

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lindabinti.backend.security.RoleGuard
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

class DashboardAuthorizationTest {

    @Test
    fun unauthenticatedDashboardApiReturnsUnauthorized() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val overview = client.get("/v1/dashboard/overview")
        assertEquals(HttpStatusCode.Unauthorized, overview.status)
        val flags = client.get("/v1/reports/flags")
        assertEquals(HttpStatusCode.Unauthorized, flags.status)
        val summary = client.get("/v1/analytics/summary")
        assertEquals(HttpStatusCode.Unauthorized, summary.status)
        val staff = client.get("/v1/admin/staff")
        assertEquals(HttpStatusCode.Unauthorized, staff.status)
    }

    @Test
    fun dashboardHtmlIsPublicButDoesNotExposeCases() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val page = client.get("/dashboard/")
        assertEquals(HttpStatusCode.OK, page.status)
        val html = page.bodyAsText()
        assertTrue(html.contains("LINDA BINTI"))
        assertTrue(html.contains("not live police dispatch"))
        assertTrue(html.contains("not a live shortcode"))
        assertTrue(html.contains("do not replace professional judgment"))
        assertTrue(html.contains("Access restricted to authorized NGO staff and administrators"))
        assertTrue(html.contains("Privacy protected"))
        assertFalse(html.contains("private survivor"))
        assertFalse(html.contains("admin@linda.local"))
    }

    @Test
    fun userCannotAccessNgoDashboardOrAdminEndpoints() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("user@example.com", "Password123!", "Amina")
        val bearer = user.bearer()

        val forbiddenPaths = listOf(
            "/v1/dashboard/session",
            "/v1/dashboard/overview",
            "/v1/dashboard/cases",
            "/v1/dashboard/trends",
            "/v1/dashboard/intelligence",
            "/v1/reports/flags",
            "/v1/analytics/summary",
            "/v1/admin/staff",
            "/v1/admin/audit"
        )
        for (path in forbiddenPaths) {
            val res = client.get(path) { header(HttpHeaders.Authorization, bearer) }
            assertEquals("$path should be forbidden for USER", HttpStatusCode.Forbidden, res.status)
            assertTrue(res.bodyAsText().contains("not permitted") || res.bodyAsText().contains("forbidden"))
        }

        val statusUpdate = client.post("/v1/dashboard/cases/any-id/status") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"status":"REVIEWING"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, statusUpdate.status)

        val createStaff = client.post("/v1/admin/staff") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"x@ngo.local","password":"Password123!","role":"NGO_STAFF"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, createStaff.status)
    }

    @Test
    fun ngoStaffCanAccessAuthorizedCaseAndAggregateDataButNotAdmin() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("survivor@example.com", "Password123!", "Amina")
        val submitted = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, user.bearer())
            header("Idempotency-Key", "case-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"case-1","categoryId":"fgm_pressure","categoryLabel":"FGM threat",
                 "body":"private survivor text","createdAtEpochMs":1710000000000,"includeBody":true,
                 "areaCode":"kajiado_north","areaLabel":"Kajiado North","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, submitted.status)
        val serverId = JsonParser.parseString(submitted.bodyAsText()).asJsonObject
            .get("serverReportId").asString

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val created = client.post("/v1/admin/staff") {
            header(HttpHeaders.Authorization, admin.bearer())
            contentType(ContentType.Application.Json)
            setBody("""{"email":"staff@ngo.local","password":"StaffPass1!","role":"NGO_STAFF"}""")
        }
        assertEquals(HttpStatusCode.OK, created.status)
        assertEquals("NGO_STAFF", JsonParser.parseString(created.bodyAsText()).asJsonObject.get("role").asString)

        val staff = client.login("staff@ngo.local", "StaffPass1!")
        val staffBearer = staff.bearer()

        val overview = client.get("/v1/dashboard/overview") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.OK, overview.status)
        val overviewJson = JsonParser.parseString(overview.bodyAsText()).asJsonObject
        assertTrue(overviewJson.get("totalReports").asInt >= 1)
        assertTrue(overviewJson.get("totalFgmReports").asInt >= 1)
        assertTrue(overviewJson.get("highPriorityCases").asInt >= 1)
        assertFalse(overview.bodyAsText().contains("private survivor text"))
        assertFalse(overviewJson.get("gpsShown").asBoolean)

        val cases = client.get("/v1/dashboard/cases") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.OK, cases.status)
        val casesText = cases.bodyAsText()
        assertFalse(casesText.contains("private survivor text"))
        assertFalse(casesText.contains("survivor@example.com"))
        assertFalse(casesText.contains("latitude"))
        assertFalse(casesText.contains("longitude"))
        assertTrue(casesText.contains("NEW"))
        assertTrue(casesText.contains("LB-"))
        assertTrue(casesText.contains("Kajiado North"))

        val trends = client.get("/v1/dashboard/trends") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.OK, trends.status)
        assertTrue(trends.bodyAsText().contains("Kajiado"))
        assertFalse(trends.bodyAsText().contains("private survivor text"))
        assertFalse(trends.bodyAsText().contains("latitude"))
        assertFalse(trends.bodyAsText().contains("longitude"))
        assertTrue(trends.bodyAsText().contains("geoAreas"))
        assertTrue(trends.bodyAsText().contains("Kajiado North"))

        val flags = client.get("/v1/reports/flags") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.OK, flags.status)

        val summary = client.get("/v1/analytics/summary") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.OK, summary.status)
        val summaryBody = summary.bodyAsText()
        assertTrue(summaryBody.contains("FGM"))
        assertTrue(summaryBody.contains("HIGH") || summaryBody.contains("CRITICAL"))

        val updated = client.post("/v1/dashboard/cases/$serverId/status") {
            header(HttpHeaders.Authorization, staffBearer)
            contentType(ContentType.Application.Json)
            setBody("""{"status":"REFERRED"}""")
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        assertTrue(updated.bodyAsText().contains("REFERRED"))
        assertFalse(updated.bodyAsText().contains("private survivor text"))

        val invalid = client.post("/v1/dashboard/cases/$serverId/status") {
            header(HttpHeaders.Authorization, staffBearer)
            contentType(ContentType.Application.Json)
            setBody("""{"status":"GUILTY"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)

        val adminStaff = client.get("/v1/admin/staff") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.Forbidden, adminStaff.status)
        val audit = client.get("/v1/admin/audit") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.Forbidden, audit.status)
        val raw = client.get("/v1/reports/me") { header(HttpHeaders.Authorization, staffBearer) }
        assertEquals(HttpStatusCode.Forbidden, raw.status)
    }

    @Test
    fun invalidReportCategoryIsRejectedAndDuplicateClientUuidReturnsSameStoredReport() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("reporter@example.com", "Password123!", "Reporter")
        val bearer = user.bearer()

        val invalid = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, bearer)
            header("Idempotency-Key", "bad-cat-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"bad-cat-1","categoryId":"weather","categoryLabel":"Weather",
                 "body":"not in scope","createdAtEpochMs":1710000000000,"includeBody":true}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)
        assertTrue(invalid.bodyAsText().contains("invalid_gbv_fgm_category"))

        val first = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, bearer)
            header("Idempotency-Key", "dup-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"dup-1","categoryId":"physical","categoryLabel":"Physical violence",
                 "body":"need support","createdAtEpochMs":1710000000000,"includeBody":true}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, first.status)
        val firstJson = JsonParser.parseString(first.bodyAsText()).asJsonObject

        val duplicate = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, bearer)
            header("Idempotency-Key", "dup-1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"dup-1","categoryId":"physical","categoryLabel":"Physical violence",
                 "body":"need support","createdAtEpochMs":1710000000000,"includeBody":true}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, duplicate.status)
        val duplicateJson = JsonParser.parseString(duplicate.bodyAsText()).asJsonObject
        assertEquals(firstJson.get("serverReportId").asString, duplicateJson.get("serverReportId").asString)
        assertEquals(firstJson.get("clientUuid").asString, duplicateJson.get("clientUuid").asString)
    }

    @Test
    fun invalidAreaIsRejectedAndSmallAreaCountsAreSuppressed() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val user = client.register("geo@example.com", "Password123!", "Geo")
        val bearer = user.bearer()

        val invalidArea = client.post("/v1/reports") {
            header(HttpHeaders.Authorization, bearer)
            header("Idempotency-Key", "geo-invalid")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"clientUuid":"geo-invalid","categoryId":"physical","categoryLabel":"Physical violence",
                 "body":"need support","createdAtEpochMs":1710000000000,"includeBody":true,
                 "areaCode":"nairobi","areaLabel":"Nairobi","areaType":"SUB_COUNTY"}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.BadRequest, invalidArea.status)
        assertTrue(invalidArea.bodyAsText().contains("invalid_kajiado_area"))

        repeat(2) { index ->
            val res = client.post("/v1/reports") {
                header(HttpHeaders.Authorization, bearer)
                header("Idempotency-Key", "small-area-$index")
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {"clientUuid":"small-area-$index","categoryId":"physical","categoryLabel":"Physical violence",
                     "body":"need support","createdAtEpochMs":1710000000000,"includeBody":true,
                     "areaCode":"kajiado_west","areaLabel":"Kajiado West","areaType":"SUB_COUNTY"}
                    """.trimIndent()
                )
            }
            assertEquals(HttpStatusCode.OK, res.status)
        }

        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val trends = client.get("/v1/dashboard/trends") { header(HttpHeaders.Authorization, admin.bearer()) }
        assertEquals(HttpStatusCode.OK, trends.status)
        val body = trends.bodyAsText()
        assertTrue(body.contains("\"suppressed\":true"))
        assertTrue(body.contains("\"areaLabel\":\"Kajiado West\""))
    }

    @Test
    fun ngoAdminCanPerformAdministrativeOperations() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val admin = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val bearer = admin.bearer()

        val overview = client.get("/v1/dashboard/overview") { header(HttpHeaders.Authorization, bearer) }
        assertEquals(HttpStatusCode.OK, overview.status)

        val created = client.post("/v1/admin/staff") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"planner@ngo.local","password":"Planner1!","role":"NGO_STAFF"}""")
        }
        assertEquals(HttpStatusCode.OK, created.status)

        val staffList = client.get("/v1/admin/staff") { header(HttpHeaders.Authorization, bearer) }
        assertEquals(HttpStatusCode.OK, staffList.status)
        assertTrue(staffList.bodyAsText().contains("planner@ngo.local"))

        val audit = client.get("/v1/admin/audit") { header(HttpHeaders.Authorization, bearer) }
        assertEquals(HttpStatusCode.OK, audit.status)
        assertTrue(audit.bodyAsText().contains("STAFF_CREATE"))
    }

    @Test
    fun refreshEndpointIssuesNewAccessTokenForValidRefreshToken() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val login = client.login("admin@linda.local", "ChangeMeAdmin1!")
        val refreshToken = login.get("refreshToken").asString
        val accessToken = login.get("accessToken").asString

        val refreshed = client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshed.status)
        val refreshedJson = JsonParser.parseString(refreshed.bodyAsText()).asJsonObject
        assertEquals("NGO_ADMIN", refreshedJson.get("role").asString)
        assertTrue(refreshedJson.get("accessToken").asString.isNotBlank())
        assertNotEquals(accessToken, refreshedJson.get("accessToken").asString)

        val overview = client.get("/v1/dashboard/overview") {
            header(HttpHeaders.Authorization, "Bearer " + refreshedJson.get("accessToken").asString)
        }
        assertEquals(HttpStatusCode.OK, overview.status)
    }

    @Test
    fun refreshEndpointRejectsInvalidRefreshToken() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val res = client.post("/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"not-a-valid-token"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
        assertTrue(res.bodyAsText().contains("invalid_refresh_token"))
    }

    @Test
    fun roleGuardRejectsUserDashboardAccess() {
        assertFalse(RoleGuard.canAccessDashboard(Role.USER))
        assertTrue(RoleGuard.canAccessDashboard(Role.NGO_STAFF))
        assertTrue(RoleGuard.canAccessDashboard(Role.NGO_ADMIN))
        assertFalse(RoleGuard.canManageStaff(Role.NGO_STAFF))
        assertTrue(RoleGuard.canManageStaff(Role.NGO_ADMIN))
        assertFalse(RoleGuard.canUpdateCaseStatus(Role.USER))
        assertTrue(RoleGuard.canUpdateCaseStatus(Role.NGO_STAFF))
    }

    @Test
    fun deviceSessionIssuesUserTokenWithoutPassword() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val res = client.post("/v1/auth/device") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        assertEquals("USER", json.get("role").asString)
        assertTrue(json.get("accessToken").asString.isNotBlank())
        val again = client.post("/v1/auth/device") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"}""")
        }
        assertEquals(HttpStatusCode.OK, again.status)
        assertEquals(json.get("userId").asString, JsonParser.parseString(again.bodyAsText()).asJsonObject.get("userId").asString)
    }

    private suspend fun HttpClient.register(email: String, password: String, name: String): JsonObject {
        val res = post("/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password","displayName":"$name"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        return JsonParser.parseString(res.bodyAsText()).asJsonObject
    }

    private suspend fun HttpClient.login(email: String, password: String): JsonObject {
        val res = post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        return JsonParser.parseString(res.bodyAsText()).asJsonObject
    }

    private fun JsonObject.bearer(): String = "Bearer " + get("accessToken").asString
}
