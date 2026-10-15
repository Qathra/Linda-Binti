package com.lindabinti.backend

import com.google.gson.JsonParser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportServicesApiTest {

    @Test
    fun publicCatalogReturnsVerifiedHelplinesWithoutAuth() = testApplication {
        application { moduleForTests() }
        val res = client.get("/v1/support-services")
        assertEquals(HttpStatusCode.OK, res.status)
        val json = JsonParser.parseString(res.bodyAsText()).asJsonObject
        val services = json.getAsJsonArray("services")
        val phones = services.flatMap { item ->
            val obj = item.asJsonObject
            listOfNotNull(
                obj.get("phone")?.takeIf { !it.isJsonNull }?.asString,
                obj.get("alternatePhone")?.takeIf { !it.isJsonNull }?.asString
            )
        }.toSet()
        assertTrue(phones.containsAll(setOf("999", "112", "1195", "116")))
        services.forEach { item ->
            val obj = item.asJsonObject
            val phone = obj.get("phone")
            val verified = obj.get("phoneVerified").asBoolean
            if (phone == null || phone.isJsonNull) {
                assertFalse(obj.get("id").asString, verified)
            } else {
                assertTrue(obj.get("id").asString, verified)
            }
        }
        val blob = res.bodyAsText()
        assertTrue(blob.contains("Kajiado"))
        assertTrue(blob.contains("GBV"))
        assertTrue(blob.contains("FGM"))
        assertFalse(blob.contains("0700"))
    }
}
