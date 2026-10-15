package com.example.lindabinti.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BackendAccountRepositoryTest {

    private fun auth(role: String = "USER") = AuthResponseDto(
        accessToken = "tok-$role",
        refreshToken = "ref",
        role = role,
        userId = "u1",
        email = "a@example.com",
        expiresAtEpochMs = System.currentTimeMillis() + 60_000
    )

    private class FakeApi(
        var registerResult: AuthResponseDto? = null,
        var loginResult: AuthResponseDto? = null,
        var summary: AnalyticsSummaryDto? = null,
        var registerError: Throwable? = null,
        var loginError: Throwable? = null,
        var retryFlag: ReportFlagDto? = null
    ) : LindaBintiApi {
        override suspend fun register(body: RegisterRequestDto): AuthResponseDto {
            registerError?.let { throw it }
            return registerResult ?: error("no register")
        }
        override suspend fun login(body: LoginRequestDto): AuthResponseDto {
            loginError?.let { throw it }
            return loginResult ?: error("no login")
        }
        override suspend fun deviceSession(body: DeviceSessionRequestDto): AuthResponseDto {
            return loginResult ?: AuthResponseDto(
                accessToken = "tok-USER",
                refreshToken = "ref",
                role = "USER",
                userId = "u1",
                email = "a@example.com",
                expiresAtEpochMs = System.currentTimeMillis() + 60_000
            )
        }
        override suspend fun refresh(body: RefreshRequestDto): AuthResponseDto = AuthResponseDto(
            accessToken = "tok-USER",
            refreshToken = "ref",
            role = "USER",
            userId = "u1",
            email = "a@example.com",
            expiresAtEpochMs = System.currentTimeMillis() + 60_000
        )
        override suspend fun logout(bearer: String, body: LogoutRequestDto): Response<Unit> = Response.success(Unit)
        override suspend fun submitReport(
            bearer: String,
            idempotencyKey: String,
            body: ReportSubmitRequestDto
        ): ReportSubmitResponseDto = error("unused")
        override suspend fun ingestAnalytics(
            bearer: String,
            body: AnalyticsIngestRequestDto
        ): Response<Unit> = Response.success(Unit)
        override suspend fun analyticsSummary(bearer: String): AnalyticsSummaryDto =
            summary ?: error("no summary")
        override suspend fun reportFlags(bearer: String): List<ReportFlagDto> = emptyList()
        override suspend fun retryReportFlagAnalysis(bearer: String, serverReportId: String): ReportFlagDto =
            retryFlag ?: error("unused")
        override suspend fun health(): Map<String, String> = mapOf("status" to "ok", "version" to "1.0")
        override suspend fun aiCapabilities() = AiCapabilitiesResponseDto(onlineEnabled = false)
        override suspend fun analyzeReport(
            bearer: String,
            body: AiAnalyzeReportRequestDto
        ): Response<AiAnalyzeReportResponseDto> = error("unused")
        override suspend fun assistantChat(
            bearer: String,
            body: AssistantChatRequestDto
        ): Response<AssistantChatResponseDto> = error("unused")
        override suspend fun supportServices() = SupportServicesCatalogDto()
        override suspend fun educationContent(language: String?, category: String?, type: String?) =
            EducationContentListDto()
        override suspend fun educationContentById(id: String) = EducationContentDto(id = id)
    }

    @Test
    fun registerStoresUserSession() = runBlocking {
        val session = FakeBackendSession()
        val api = FakeApi(registerResult = auth("USER"))
        val repo = BackendAccountRepository(api, session)
        val result = repo.register("a@example.com", "Password123!", "Amina")
        assertTrue(result.isSuccess)
        assertTrue(session.isLoggedIn())
        assertTrue(session.isUserRole())
        assertEquals("Bearer tok-USER", session.bearer())
    }

    @Test
    fun loginStoresSession() = runBlocking {
        val session = FakeBackendSession()
        val api = FakeApi(loginResult = auth("USER"))
        val repo = BackendAccountRepository(api, session)
        assertTrue(repo.login("a@example.com", "Password123!").isSuccess)
        assertTrue(session.isLoggedIn())
    }

    @Test
    fun ngoSummaryRejectedForUserRole() = runBlocking {
        val session = FakeBackendSession()
        session.saveSession(auth("USER"))
        val api = FakeApi(summary = AnalyticsSummaryDto(note = "x"))
        val repo = BackendAccountRepository(api, session)
        val result = repo.fetchNgoSummary()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun ngoSummaryAllowedForStaff() = runBlocking {
        val session = FakeBackendSession()
        session.saveSession(auth("NGO_STAFF"))
        val api = FakeApi(summary = AnalyticsSummaryDto(note = "aggregates", minThreshold = 3))
        val repo = BackendAccountRepository(api, session)
        val result = repo.fetchNgoSummary()
        assertTrue(result.isSuccess)
        assertEquals("aggregates", result.getOrNull()?.note)
    }

    @Test
    fun ngoFlagsRejectedForUserRole() = runBlocking {
        val session = FakeBackendSession()
        session.saveSession(auth("USER"))
        val repo = BackendAccountRepository(FakeApi(), session)
        val result = repo.fetchNgoReportFlags()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun ngoRetryAnalysisRejectedForUserRole() = runBlocking {
        val session = FakeBackendSession()
        session.saveSession(auth("USER"))
        val repo = BackendAccountRepository(FakeApi(), session)
        val result = repo.retryReportFlagAnalysis("srv-1")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun ngoRetryAnalysisAllowedForStaff() = runBlocking {
        val session = FakeBackendSession()
        session.saveSession(auth("NGO_STAFF"))
        val flag = ReportFlagDto(
            serverReportId = "srv-1",
            categoryId = "fgm_pressure",
            categoryLabel = "FGM threat",
            urgency = "HIGH",
            explanation = "Report indicates immediate pressure/threat of FGM.",
            aiStatus = "complete",
            gbvFgmRelevance = "FGM",
            incidentCategory = "FGM threat",
            recommendedPriority = "HIGH"
        )
        val repo = BackendAccountRepository(FakeApi(retryFlag = flag), session)
        val result = repo.retryReportFlagAnalysis("srv-1")
        assertTrue(result.isSuccess)
        assertEquals("HIGH", result.getOrNull()?.urgency)
        assertEquals("complete", result.getOrNull()?.aiStatus)
        assertEquals("FGM", result.getOrNull()?.gbvFgmRelevance)
    }

    @Test
    fun reportFlagDtoDefaultsMarkMissingAiResultAsUnavailable() {
        val dto = ReportFlagDto(serverReportId = "s1", categoryId = "sexual", urgency = "HIGH")
        assertEquals("unavailable", dto.aiStatus)
        assertEquals("", dto.gbvFgmRelevance)
        assertEquals("rules", dto.classifierSource)
    }

    @Test
    fun nullApiMeansNotConfigured() {
        val repo = BackendAccountRepository(null, FakeBackendSession())
        assertFalse(repo.backendConfigured())
    }
}