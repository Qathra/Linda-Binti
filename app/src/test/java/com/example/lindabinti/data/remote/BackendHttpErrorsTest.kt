package com.example.lindabinti.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class BackendHttpErrorsTest {

    @Test
    fun parsesJsonErrorField() {
        val body = """{"error":"Email already registered"}"""
            .toResponseBody("application/json".toMediaType())
        val http = HttpException(Response.error<Unit>(400, body))
        assertEquals("Email already registered", BackendHttpErrors.toUserMessage(http))
    }

    @Test
    fun fallsBackToHttpCode() {
        val body = "nope".toResponseBody("text/plain".toMediaType())
        val http = HttpException(Response.error<Unit>(503, body))
        assertTrue(BackendHttpErrors.toUserMessage(http).contains("503"))
    }

    @Test
    fun networkErrorsIncludeConnectHint() {
        val io = java.net.SocketTimeoutException(
            "failed to connect to /192.168.100.44 (port 8080) from /10.0.0.2 (port 48191) after 20000ms"
        )
        assertTrue(BackendHttpErrors.isConnectFailure(io))
        assertTrue(BackendHttpErrors.toUserMessage(io).startsWith("Network error:"))
    }
}
