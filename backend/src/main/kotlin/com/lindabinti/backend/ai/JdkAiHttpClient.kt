package com.lindabinti.backend.ai

import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class JdkAiHttpClient : AiHttpClient {
    override fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMs: Int
    ): AiHttpResponse {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.readText().orEmpty()
            return AiHttpResponse(code, text)
        } catch (e: SocketTimeoutException) {
            throw AiTransportException.Timeout(e)
        } catch (e: java.io.IOException) {
            throw AiTransportException.Network(e)
        } finally {
            conn.disconnect()
        }
    }
}
