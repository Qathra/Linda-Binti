package com.example.lindabinti.ai

import com.example.lindabinti.data.remote.AiAnalyzeReportRequestDto
import com.example.lindabinti.data.remote.AiAnalyzeReportResponseDto
import com.example.lindabinti.data.remote.AiCapabilitiesResponseDto
import com.example.lindabinti.data.remote.AnalyticsIngestRequestDto
import com.example.lindabinti.data.remote.AnalyticsSummaryDto
import com.example.lindabinti.data.remote.AssistantChatRequestDto
import com.example.lindabinti.data.remote.AssistantChatResponseDto
import com.example.lindabinti.data.remote.AuthResponseDto
import com.example.lindabinti.data.remote.LindaBintiApi
import com.example.lindabinti.data.remote.LoginRequestDto
import com.example.lindabinti.data.remote.LogoutRequestDto
import com.example.lindabinti.data.remote.RefreshRequestDto
import com.example.lindabinti.data.remote.RegisterRequestDto
import com.example.lindabinti.data.remote.ReportFlagDto
import com.example.lindabinti.data.remote.ReportSubmitRequestDto
import com.example.lindabinti.data.remote.ReportSubmitResponseDto
import com.example.lindabinti.data.remote.SupportServicesCatalogDto
import retrofit2.Response

/** Test double for [LindaBintiApi] — only health/capabilities are functional. */
class FakeLindaBintiApi(
    private val caps: AiCapabilitiesResponseDto,
    private val healthMap: Map<String, String> = mapOf("ai" to "configured")
) : LindaBintiApi {
    override suspend fun register(body: RegisterRequestDto): AuthResponseDto = unsupported()
    override suspend fun login(body: LoginRequestDto): AuthResponseDto = unsupported()
    override suspend fun deviceSession(body: com.example.lindabinti.data.remote.DeviceSessionRequestDto): AuthResponseDto = unsupported()
    override suspend fun refresh(body: RefreshRequestDto): AuthResponseDto = unsupported()
    override suspend fun logout(bearer: String, body: LogoutRequestDto): Response<Unit> = unsupported()
    override suspend fun submitReport(
        bearer: String,
        idempotencyKey: String,
        body: ReportSubmitRequestDto
    ): ReportSubmitResponseDto = unsupported()
    override suspend fun ingestAnalytics(bearer: String, body: AnalyticsIngestRequestDto): Response<Unit> =
        unsupported()
    override suspend fun analyticsSummary(bearer: String): AnalyticsSummaryDto = unsupported()
    override suspend fun reportFlags(bearer: String): List<ReportFlagDto> = unsupported()
    override suspend fun retryReportFlagAnalysis(bearer: String, serverReportId: String): ReportFlagDto =
        unsupported()
    override suspend fun health(): Map<String, String> = healthMap
    override suspend fun aiCapabilities(): AiCapabilitiesResponseDto = caps
    override suspend fun analyzeReport(
        bearer: String,
        body: AiAnalyzeReportRequestDto
    ): Response<AiAnalyzeReportResponseDto> = unsupported()
    override suspend fun assistantChat(
        bearer: String,
        body: AssistantChatRequestDto
    ): Response<AssistantChatResponseDto> = unsupported()
    override suspend fun supportServices(): SupportServicesCatalogDto = unsupported()
    override suspend fun educationContent(
        language: String?,
        category: String?,
        type: String?
    ) = com.example.lindabinti.data.remote.EducationContentListDto()
    override suspend fun educationContentById(id: String) =
        com.example.lindabinti.data.remote.EducationContentDto(id = id)

    private fun <T> unsupported(): T = throw UnsupportedOperationException("Not used in test")
}
