package com.lindabinti.backend

import com.google.gson.JsonParser
import com.lindabinti.backend.dev.DevEnvironment
import com.lindabinti.backend.dev.SecurePasswordGenerator
import com.lindabinti.backend.services.UserRepository
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

class DevAdminPasswordTest {

    @Test
    fun devResetIsBlockedWithoutExplicitFlag() {
        assertFalse(DevEnvironment.allowsAdminPasswordReset())
    }

    @Test
    fun temporaryAdminPasswordRequiresChangeBeforeDashboardAccess() = testApplication {
        application { moduleForTests(seedAdmin = true, adminMustChangePassword = true) }
        val login = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val loginJson = JsonParser.parseString(login.bodyAsText()).asJsonObject
        assertTrue(loginJson.get("mustChangePassword").asBoolean)
        val bearer = "Bearer " + loginJson.get("accessToken").asString

        val session = client.get("/v1/dashboard/session") { header(HttpHeaders.Authorization, bearer) }
        assertEquals(HttpStatusCode.OK, session.status)
        assertTrue(JsonParser.parseString(session.bodyAsText()).asJsonObject.get("mustChangePassword").asBoolean)

        val overview = client.get("/v1/dashboard/overview") { header(HttpHeaders.Authorization, bearer) }
        assertEquals(HttpStatusCode.Forbidden, overview.status)
        assertTrue(overview.bodyAsText().contains("password_change_required"))

        val changed = client.post("/v1/auth/change-password") {
            header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.Json)
            setBody("""{"currentPassword":"ChangeMeAdmin1!","newPassword":"AdminPass2!"}""")
        }
        assertEquals(HttpStatusCode.OK, changed.status)
        val changedJson = JsonParser.parseString(changed.bodyAsText()).asJsonObject
        assertFalse(changedJson.get("mustChangePassword").asBoolean)
        val newBearer = "Bearer " + changedJson.get("accessToken").asString

        val overviewAfter = client.get("/v1/dashboard/overview") { header(HttpHeaders.Authorization, newBearer) }
        assertEquals(HttpStatusCode.OK, overviewAfter.status)

        val oldLogin = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, oldLogin.status)
    }

    @Test
    fun securePasswordGeneratorMeetsMinimumRequirements() {
        repeat(10) {
            val password = SecurePasswordGenerator.generate()
            assertTrue(password.length >= 12)
            assertTrue(password.any { it.isUpperCase() })
            assertTrue(password.any { it.isLowerCase() })
            assertTrue(password.any { it.isDigit() })
            assertNotEquals(password, SecurePasswordGenerator.generate())
        }
    }
}
