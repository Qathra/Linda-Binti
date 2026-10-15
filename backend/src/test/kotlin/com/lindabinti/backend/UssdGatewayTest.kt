package com.lindabinti.backend

import com.google.gson.JsonParser
import com.lindabinti.backend.ussd.UssdGatewayAuth
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

class UssdGatewayTest {

    @Test
    fun inboundMenuDoesNotPersistMsisdnAndDoesNotClaimLiveShortcode() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val res = client.post("/v1/ussd/inbound") {
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId":"s1","msisdn":"+254700000000","text":"","serviceCode":"*XXX#"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("1 Report GBV"))
        assertFalse(body.contains("254700000000"))
        assertFalse(body.contains("Safaricom", ignoreCase = true))
        assertTrue(body.contains("stub"))
    }

    @Test
    fun gbvReportChoiceCreatesAnonymizedFlagForNgo() = testApplication {
        application { moduleForTests(seedAdmin = true) }
        val inbound = client.post("/v1/ussd/inbound") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"sessionId":"sess-gbv","msisdn":"+254711111111","text":"1*1*Jane Wanjiku","serviceCode":""}"""
            )
        }
        assertEquals(HttpStatusCode.OK, inbound.status)
        val inboundJson = JsonParser.parseString(inbound.bodyAsText()).asJsonObject
        assertTrue(inboundJson.get("reportAccepted").asBoolean)
        assertTrue(inboundJson.get("endSession").asBoolean)
        assertTrue(inboundJson.get("referenceId").asString.startsWith("LB-"))
        assertFalse(inbound.bodyAsText().contains("254711111111"))

        val admin = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        val token = JsonParser.parseString(admin.bodyAsText()).asJsonObject.get("accessToken").asString
        val flags = client.get("/v1/dashboard/cases") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, flags.status)
        val flagsText = flags.bodyAsText()
        assertTrue(flagsText.contains("USSD GBV · Jane Wanjiku"))
        assertTrue(flagsText.contains("kajiado_central") || flagsText.contains("Kajiado Central"))
        assertFalse(flagsText.contains("254711111111"))
        assertFalse(flagsText.contains("ussd.channel@linda.local"))
    }

    @Test
    fun configuredGatewayRejectsMissingKey() = testApplication {
        application { moduleForTests(ussdGatewayAuth = UssdGatewayAuth("gateway-secret")) }
        val denied = client.post("/v1/ussd/inbound") {
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId":"s","text":"1"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, denied.status)

        val allowed = client.post("/v1/ussd/inbound") {
            contentType(ContentType.Application.Json)
            header(UssdGatewayAuth.HEADER, "gateway-secret")
            setBody("""{"sessionId":"s","text":"1"}""")
        }
        assertEquals(HttpStatusCode.OK, allowed.status)
        assertTrue(allowed.bodyAsText().contains("Kajiado Central"))
    }

    @Test
    fun demoInboundSkipsGatewayKeyAndFlagsNgo() = testApplication {
        application { moduleForTests(seedAdmin = true, ussdGatewayAuth = UssdGatewayAuth("gateway-secret")) }
        val denied = client.post("/v1/ussd/inbound") {
            contentType(ContentType.Application.Json)
            setBody("""{"sessionId":"blocked","text":"1"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, denied.status)

        val demo = client.post("/v1/ussd/demo") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"sessionId":"demo-fgm","msisdn":"+254700000000","text":"2*1*Mary Nkini","serviceCode":"*384*1102#"}"""
            )
        }
        assertEquals(HttpStatusCode.OK, demo.status)
        val demoJson = JsonParser.parseString(demo.bodyAsText()).asJsonObject
        assertTrue(demoJson.get("reportAccepted").asBoolean)
        assertFalse(demo.bodyAsText().contains("254700000000"))

        val admin = client.post("/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@linda.local","password":"ChangeMeAdmin1!"}""")
        }
        val token = JsonParser.parseString(admin.bodyAsText()).asJsonObject.get("accessToken").asString
        val flags = client.get("/v1/dashboard/cases") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        val flagsText = flags.bodyAsText()
        assertTrue(flagsText.contains("USSD FGM · Mary Nkini"))
        assertTrue(flagsText.contains("kajiado_central") || flagsText.contains("Kajiado Central"))
    }

    @Test
    fun healthReportsUssdStubWhenNoGatewaySecret() = testApplication {
        application { moduleForTests() }
        val health = client.get("/v1/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertTrue(health.bodyAsText().contains("\"ussd\":\"stub\""))
    }
}
