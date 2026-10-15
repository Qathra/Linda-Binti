package com.example.lindabinti.data.remote

import com.example.lindabinti.BuildConfig
import com.example.lindabinti.data.prefs.SecurePrefs

class BackendAccountRepository(
    private val api: LindaBintiApi?,
    private val session: BackendSession
) {
    fun backendConfigured(): Boolean = api != null

    fun configuredBaseUrl(): String = BuildConfig.BACKEND_BASE_URL

    suspend fun register(email: String, password: String, displayName: String): Result<AuthResponseDto> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        return runCatching {
            client.register(RegisterRequestDto(email.trim(), password, displayName.trim())).also {
                session.saveSession(it)
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }

    suspend fun login(email: String, password: String): Result<AuthResponseDto> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        return runCatching {
            client.login(LoginRequestDto(email.trim(), password)).also {
                session.saveSession(it)
            }
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }

    suspend fun logout() {
        val client = api
        val bearer = session.bearer()
        val refresh = session.refreshToken()
        if (client != null && bearer != null) {
            runCatching {
                client.logout(bearer, LogoutRequestDto(refreshToken = refresh.orEmpty()))
            }
        }
        session.clear()
    }

    suspend fun checkHealth(): Result<String> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        return runCatching {
            val map = client.health()
            map["status"] ?: "ok"
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }

    /** Refresh expired access token. Does not change the user's consent flags. */
    suspend fun prepareAssistantSession(prefs: SecurePrefs): Boolean {
        if (!backendConfigured()) return false
        refreshSessionIfNeeded(prefs)
        return checkHealth().isSuccess
    }

    suspend fun ensureValidSession(prefs: SecurePrefs): Boolean = ensureDeviceSession(prefs)

    /**
     * Opens (or reuses) a USER session for this phone so SOS can flag the NGO map.
     * Does not flip sync or cloud-AI consent — those stay as the user set them.
     */
    suspend fun ensureDeviceSession(prefs: SecurePrefs): Boolean {
        if (session.isLoggedIn() && session.isUserRole()) return true
        if (refreshSessionIfNeeded(prefs) && session.isUserRole()) return true
        val client = api ?: return false
        var id = prefs.deviceId.trim()
        if (id.length < 8) {
            id = java.util.UUID.randomUUID().toString()
            prefs.deviceId = id
        }
        return runCatching {
            client.deviceSession(DeviceSessionRequestDto(id)).also { session.saveSession(it) }
            session.isLoggedIn() && session.isUserRole()
        }.getOrDefault(false)
    }

    private suspend fun refreshSessionIfNeeded(prefs: SecurePrefs): Boolean {
        if (session.isLoggedIn()) return true
        val client = api ?: return false
        val refreshToken = prefs.backendRefreshToken?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            client.refresh(RefreshRequestDto(refreshToken)).also { session.saveSession(it) }
            session.isLoggedIn()
        }.getOrDefault(false)
    }

    suspend fun fetchNgoSummary(): Result<AnalyticsSummaryDto> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        val bearer = session.bearer() ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (!session.isNgoRole()) {
            return Result.failure(SecurityException("NGO role required"))
        }
        return runCatching { client.analyticsSummary(bearer) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }

    suspend fun fetchNgoReportFlags(): Result<List<ReportFlagDto>> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        val bearer = session.bearer() ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (!session.isNgoRole()) {
            return Result.failure(SecurityException("NGO role required"))
        }
        return runCatching { client.reportFlags(bearer) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }

    suspend fun retryReportFlagAnalysis(serverReportId: String): Result<ReportFlagDto> {
        val client = api ?: return Result.failure(IllegalStateException("Backend not configured"))
        val bearer = session.bearer() ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (!session.isNgoRole()) {
            return Result.failure(SecurityException("NGO role required"))
        }
        return runCatching { client.retryReportFlagAnalysis(bearer, serverReportId) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException(BackendHttpErrors.toUserMessage(it), it)) }
        )
    }
}
