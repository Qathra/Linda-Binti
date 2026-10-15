package com.lindabinti.backend

import com.google.gson.JsonParser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationContentApiTest {

    @Test
    fun publicListReturnsSeededDemoVideos() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val res = client.get("/v1/education/content")
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        val items = json.getAsJsonArray("items")
        assertTrue(items.size() >= 3)
        assertTrue(items.any { it.asJsonObject.get("contentType").asString == "video" })
    }

    @Test
    fun adminCanCreateAndDeactivate() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val login = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val token = JsonParser.parseString(login.bodyAsText()).asJsonObject.get("accessToken").asString

        val created = client.post("/v1/admin/education/content") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"contentType":"video","title":"Test clip","description":"Demo",
                 "language":"en","category":"legal_rights",
                 "urlOrPath":"https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                 "durationSeconds":12,"active":true,"sortOrder":99}
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val id = JsonParser.parseString(created.bodyAsText()).asJsonObject.get("id").asString

        val deactivated = client.post("/v1/admin/education/content/$id/active") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"active":false}""")
        }
        assertEquals(HttpStatusCode.OK, deactivated.status)

        val publicList = client.get("/v1/education/content")
        val publicItems = JsonParser.parseString(publicList.bodyAsText()).asJsonObject.getAsJsonArray("items")
        assertTrue(publicItems.none { it.asJsonObject.get("id").asString == id })
    }
}
