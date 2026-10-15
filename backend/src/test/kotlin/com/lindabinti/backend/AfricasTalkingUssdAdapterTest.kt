package com.lindabinti.backend

import com.lindabinti.backend.ussd.AfricasTalkingUssdAdapter
import com.lindabinti.backend.ussd.UssdGatewayAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AfricasTalkingUssdAdapterTest {

    @Test
    fun parseFormIgnoresCaseAndMarksPhonePresentWithoutExposingItInResponse() {
        val form = AfricasTalkingUssdAdapter.parseForm(
            mapOf(
                "sessionId" to "sess-1",
                "phoneNumber" to "+254700000000",
                "serviceCode" to "*384*55#",
                "text" to "1"
            )
        )
        assertEquals("sess-1", form.sessionId)
        assertEquals("*384*55#", form.serviceCode)
        assertEquals("1", form.text)
        assertTrue(form.phoneNumberPresent)
    }

    @Test
    fun toAtResponseUsesConAndEndPrefixes() {
        assertEquals(
            "CON Hello",
            AfricasTalkingUssdAdapter.toAtResponse(
                AfricasTalkingUssdAdapter.UssdInboundMenu("Hello", endSession = false)
            )
        )
        assertEquals(
            "END Bye",
            AfricasTalkingUssdAdapter.toAtResponse(
                AfricasTalkingUssdAdapter.UssdInboundMenu("Bye", endSession = true)
            )
        )
    }

    @Test
    fun africasTalkingEndpointReturnsConMenuAndOmitsMsisdn() = testApplication {
        application { moduleForTests(ussdGatewayAuth = UssdGatewayAuth("at-secret")) }
        val res = client.submitForm(
            url = "/v1/ussd/africas-talking?key=at-secret",
            formParameters = parametersOf(
                "sessionId" to listOf("s-at"),
                "phoneNumber" to listOf("+254722222222"),
                "serviceCode" to listOf("*384*55#"),
                "text" to listOf("")
            )
        )
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.startsWith("CON "))
        assertTrue(body.contains("1 Report GBV"))
        assertFalse(body.contains("254722222222"))
    }

    @Test
    fun africasTalkingEndpointEndsSessionOnHotlineChoice() = testApplication {
        application { moduleForTests(ussdGatewayAuth = UssdGatewayAuth("at-secret")) }
        val res = client.submitForm(
            url = "/v1/ussd/africas-talking?key=at-secret",
            formParameters = parametersOf(
                "sessionId" to listOf("s-at-1"),
                "phoneNumber" to listOf("+254733333333"),
                "text" to listOf("3")
            )
        )
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.startsWith("END "))
        assertTrue(body.contains("999"))
        assertFalse(body.contains("254733333333"))
    }

    @Test
    fun africasTalkingRejectsBadKeyWhenConfigured() = testApplication {
        application { moduleForTests(ussdGatewayAuth = UssdGatewayAuth("at-secret")) }
        val denied = client.submitForm(
            url = "/v1/ussd/africas-talking?key=wrong",
            formParameters = parametersOf("sessionId" to listOf("x"), "text" to listOf(""))
        )
        assertEquals(HttpStatusCode.Unauthorized, denied.status)
    }

    @Test
    fun healthReportsConfiguredWhenGatewaySecretInjected() = testApplication {
        application { moduleForTests(ussdGatewayAuth = UssdGatewayAuth("live-secret")) }
        val health = client.get("/v1/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertTrue(health.bodyAsText().contains("\"ussd\":\"gateway_key_configured\""))
    }
}
