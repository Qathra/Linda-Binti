package com.lindabinti.backend.ai

import com.lindabinti.backend.Role
import com.lindabinti.backend.db.Database
import com.lindabinti.backend.services.AiOrchestrationService
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.ReportService
import com.lindabinti.backend.services.UserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AiRiskFlaggingTest {

    private val sampleInput = PiiMinimizer.toInput(
        categoryId = "fgm_pressure",
        categoryLabel = "FGM threat or pressure",
        body = "Immediate pressure tonight. Call 0712345678 or amina@example.com",
        includeBody = true
    )

    private val configured = AiConfig(
        apiKey = "test-key-not-for-android",
        baseUrl = "https://ai.example.test",
        model = "gpt-4o-mini",
        timeoutMs = 800,
        maxRetries = 0,
        enabledFlag = true
    )

    @Test
    fun successfulAiResponseIsParsedIntoStructuredResult() {
        val http = ScriptedHttpClient(
            AiHttpResponse(200, chatCompletion(VALID_AI_JSON))
        )
        val result = FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(configured, http)).analyze(sampleInput)
        assertEquals(AiStatus.COMPLETE, result.status)
        assertEquals(AiRiskLevel.HIGH, result.riskLevel)
        assertEquals(AiRelevance.FGM, result.gbvFgmRelevance)
        assertEquals("FGM threat", result.incidentCategory)
        assertEquals(AiRiskLevel.HIGH, result.recommendedPriority)
        assertTrue(result.urgencyIndicators.any { it.contains("pressure", ignoreCase = true) })
        assertTrue(result.safetyConcerns.isNotEmpty())
        assertTrue(result.explanation.contains("not a medical", ignoreCase = true))
        assertEquals("ai", result.source)
        assertEquals(1, http.calls)
        assertTrue(http.lastUrl!!.endsWith("/v1/chat/completions"))
        assertTrue(http.lastHeaders["Authorization"]!!.startsWith("Bearer "))
        assertFalse(http.lastBody.contains("0712345678"))
        assertFalse(http.lastBody.contains("amina@example.com"))
        assertFalse(http.lastBody.contains("Access PIN"))
        assertFalse(http.lastBody.contains("SOS"))
    }

    @Test
    fun invalidAiResponseFallsBackToRules() {
        val http = ScriptedHttpClient(
            AiHttpResponse(200, chatCompletion("{not-json"))
        )
        val result = FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(configured, http)).analyze(sampleInput)
        assertEquals(AiStatus.INVALID, result.status)
        assertEquals("rules", result.source)
        assertTrue(AiRiskLevel.isValid(result.riskLevel))
        assertTrue(result.explanation.contains("invalid", ignoreCase = true))
    }

    @Test
    fun networkFailureFallsBackWithoutThrowing() {
        val http = ScriptedHttpClient(listOf { throw AiTransportException.Network() })
        val result = FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(configured, http)).analyze(sampleInput)
        assertEquals(AiStatus.UNAVAILABLE, result.status)
        assertEquals("rules", result.source)
        assertTrue(AiRiskLevel.isValid(result.riskLevel))
    }

    @Test
    fun timeoutFallsBackWithoutThrowing() {
        val http = ScriptedHttpClient(listOf { throw AiTransportException.Timeout() })
        val result = FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(configured, http)).analyze(sampleInput)
        assertEquals(AiStatus.TIMEOUT, result.status)
        assertEquals("rules", result.source)
        assertTrue(result.explanation.contains("timed out", ignoreCase = true))
    }

    @Test
    fun missingAiResultFallsBackWithoutThrowing() {
        val http = ScriptedHttpClient(
            AiHttpResponse(200, """{"choices":[]}""")
        )
        val result = FallbackAiRiskAnalyzer(HttpAiRiskAnalyzer(configured, http)).analyze(sampleInput)
        assertEquals(AiStatus.MISSING, result.status)
        assertEquals("rules", result.source)
        assertTrue(result.explanation.contains("no result", ignoreCase = true))
    }

    @Test
    fun riskClassificationParsingAcceptsCanonicalAndAliases() {
        assertEquals(AiRiskLevel.CRITICAL, AiRiskLevel.parse("CRITICAL"))
        assertEquals(AiRiskLevel.HIGH, AiRiskLevel.parse("high"))
        assertEquals(AiRiskLevel.MEDIUM, AiRiskLevel.parse("elevated"))
        assertEquals(AiRiskLevel.MEDIUM, AiRiskLevel.parse("moderate"))
        assertEquals(AiRiskLevel.LOW, AiRiskLevel.parse("Low"))
        assertEquals(null, AiRiskLevel.parse("urgent"))
        assertEquals(null, AiRiskLevel.parse(""))
        assertEquals(0, AiRiskLevel.sortRank("CRITICAL"))
        assertEquals(1, AiRiskLevel.sortRank("HIGH"))
        assertTrue(AiRiskLevel.sortRank("CRITICAL") < AiRiskLevel.sortRank("LOW"))
    }

    @Test
    fun parserReadsMarkdownFencedJsonAndSanitizesDiagnosisLanguage() {
        val fenced = """
            ```json
            {"riskLevel":"HIGH","gbvFgmRelevance":"FGM","incidentCategory":"FGM threat",
             "urgencyIndicators":["immediate pressure"],"safetyConcerns":["possible lack of protection"],
             "recommendedPriority":"HIGH","explanation":"Report indicates immediate pressure/threat of FGM."}
            ```
        """.trimIndent()
        val parsed = AiRiskResultParser.parse(fenced, "FGM threat or pressure")
        assertEquals(AiRiskLevel.HIGH, parsed.riskLevel)
        assertEquals(AiRelevance.FGM, parsed.gbvFgmRelevance)
        assertEquals("FGM threat", parsed.incidentCategory)
        assertEquals(AiStatus.COMPLETE, parsed.status)

        val diagnosed = """
            {"riskLevel":"LOW","gbvFgmRelevance":"GBV","incidentCategory":"Emotional",
             "urgencyIndicators":[],"safetyConcerns":[],"recommendedPriority":"LOW",
             "explanation":"The person is diagnosed with PTSD and is guilty."}
        """.trimIndent()
        val sanitized = AiRiskResultParser.parse(diagnosed)
        assertFalse(sanitized.explanation.contains("PTSD", ignoreCase = true))
        assertFalse(sanitized.explanation.contains("guilty", ignoreCase = true))
        assertTrue(sanitized.explanation.contains("not a medical", ignoreCase = true))
    }

    @Test
    fun parserRejectsMissingExplanationAndUnknownLevel() {
        try {
            AiRiskResultParser.parse("""{"riskLevel":"HIGH","explanation":""}""")
            fail("expected invalid")
        } catch (e: InvalidAiResultException) {
            assertTrue(e.message!!.contains("Missing explanation"))
        }
        try {
            AiRiskResultParser.parse("""{"riskLevel":"EXTREME","explanation":"x"}""")
            fail("expected invalid")
        } catch (e: InvalidAiResultException) {
            assertTrue(e.message!!.contains("Unknown risk level"))
        }
    }

    @Test
    fun piiMinimizerStripsContactDetailsAndOmitsBodyWhenNotConsented() {
        val redacted = PiiMinimizer.minimizeDescription(
            "Call +254 712 345 678 or amina@example.com id 123456789"
        )
        assertFalse(redacted.contains("712"))
        assertFalse(redacted.contains("@"))
        assertTrue(redacted.contains("[redacted-phone]") || redacted.contains("[redacted-id]"))
        assertTrue(redacted.contains("[redacted-email]"))
        assertFalse(redacted.contains("123456789"))

        val omitted = PiiMinimizer.toInput("sexual", "Sexual violence", "private body text", includeBody = false)
        assertEquals("", omitted.redactedDescription)
        val included = PiiMinimizer.toInput("sexual", "Sexual violence", "private body text", includeBody = true)
        assertEquals("private body text", included.redactedDescription)

        val withPin = PiiMinimizer.minimizeDescription("private survivor text with PIN 1102")
        assertFalse(withPin.contains("1102"))
        assertTrue(withPin.contains("[redacted-code]"))
    }

    @Test
    fun reportIsSavedWhenAiFailsAndFlagsOmitPrivateFields() {
        val db = Database.memory()
        db.migrate()
        val audit = AuditService(db)
        val users = UserRepository(db)
        val user = users.create("survivor@example.com", "Password123!", "Amina", Role.USER)
        val staff = users.create("staff@ngo.local", "Password123!", "Staff", Role.NGO_STAFF)
        val orchestration = AiOrchestrationService.create(
            configured,
            audit,
            ScriptedHttpClient(listOf { throw AiTransportException.Network() })
        )
        val reports = ReportService(db, audit, orchestration)
        val stored = reports.upsertOwnedReport(
            user.id, "c-ai-1", "fgm_pressure", "FGM threat or pressure",
            "private survivor text with PIN 1102", 1L, includeBody = true
        )
        assertNotNull(stored.serverId)
        assertEquals("private survivor text with PIN 1102", stored.body)
        assertEquals(AiStatus.COMPLETE, stored.aiStatus)
        assertTrue(AiRiskLevel.isValid(stored.urgency))

        val flags = reports.listFlagsForNgo(staff.id)
        assertEquals(1, flags.size)
        assertEquals(AiStatus.COMPLETE, flags[0].aiStatus)
        val blob = flags.joinToString { "${it.explanation} ${it.categoryLabel} ${it.serverReportId} ${it.urgencyIndicators}" }
        assertFalse(blob.contains("private survivor text"))
        assertFalse(blob.contains(user.id))
        assertFalse(blob.contains("survivor@example.com"))
        assertFalse(blob.contains("1102"))
    }

    @Test
    fun retryReplacesUnavailableResultWhenAiSucceeds() {
        val db = Database.memory()
        db.migrate()
        val audit = AuditService(db)
        val users = UserRepository(db)
        val user = users.create("s@example.com", "Password123!", "S", Role.USER)
        val staff = users.create("staff@ngo.local", "Password123!", "Staff", Role.NGO_STAFF)
        val http = ScriptedHttpClient(
            listOf(
                { throw AiTransportException.Timeout() },
                { AiHttpResponse(200, chatCompletion(VALID_AI_JSON)) }
            )
        )
        val orchestration = AiOrchestrationService.create(configured, audit, http)
        val reports = ReportService(db, audit, orchestration)
        val stored = reports.upsertOwnedReport(
            user.id, "c-retry", "fgm_pressure", "FGM threat or pressure", "body", 1L, true
        )
        assertEquals(AiStatus.TIMEOUT, stored.aiStatus)
        val retried = reports.retryAiAnalysis(staff.id, stored.serverId)
        assertEquals(AiStatus.COMPLETE, retried.aiStatus)
        assertEquals(AiRiskLevel.HIGH, retried.urgency)
        assertEquals("FGM threat", retried.incidentCategory)
        assertEquals(AiRelevance.FGM, retried.gbvFgmRelevance)
        assertEquals(2, http.calls)
    }

    @Test
    fun disabledAiUsesRulesAndMarksUnavailable() {
        val result = AiAnalyzerFactory.fromConfig(
            AiConfig("", "", "gpt-4o-mini", 1000, 0, false)
        ).analyze(sampleInput)
        assertEquals(AiStatus.UNAVAILABLE, result.status)
        assertEquals("rules", result.source)
        assertEquals(AiRiskLevel.CRITICAL, result.riskLevel)
    }

    private class ScriptedHttpClient(
        private val scripts: List<() -> AiHttpResponse>
    ) : AiHttpClient {
        constructor(response: AiHttpResponse) : this(listOf({ response }))

        var calls = 0
        var lastUrl: String? = null
        var lastBody: String = ""
        var lastHeaders: Map<String, String> = emptyMap()

        override fun postJson(
            url: String,
            headers: Map<String, String>,
            body: String,
            timeoutMs: Int
        ): AiHttpResponse {
            lastUrl = url
            lastBody = body
            lastHeaders = headers
            val script = scripts.getOrNull(calls) ?: scripts.last()
            calls++
            return script()
        }
    }

    companion object {
        private val VALID_AI_JSON = """
            {
              "riskLevel":"HIGH",
              "gbvFgmRelevance":"FGM",
              "incidentCategory":"FGM threat",
              "urgencyIndicators":["immediate pressure/threat of FGM"],
              "safetyConcerns":["possible lack of safe protection"],
              "recommendedPriority":"HIGH",
              "explanation":"Report indicates immediate pressure/threat of FGM and possible lack of safe protection."
            }
        """.trimIndent()

        private fun chatCompletion(content: String): String {
            val escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            return """{"choices":[{"message":{"content":"$escaped"}}]}"""
        }
    }
}
