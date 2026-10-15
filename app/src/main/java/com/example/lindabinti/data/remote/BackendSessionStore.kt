package com.example.lindabinti.data.remote

import com.example.lindabinti.data.prefs.SecurePrefs

/**
 * Cloud account session — completely separate from Access PIN / SOS hashes.
 * Tokens are stored encrypted; PIN/SOS are never written here.
 */
class BackendSessionStore(private val prefs: SecurePrefs) : BackendSession {

    override fun saveSession(auth: AuthResponseDto) {
        prefs.backendAccessToken = auth.accessToken
        prefs.backendRefreshToken = auth.refreshToken
        prefs.backendUserId = auth.userId
        prefs.backendEmail = auth.email
        prefs.backendRole = auth.role
        prefs.backendExpiresAt = auth.expiresAtEpochMs
    }

    override fun clear() {
        prefs.backendAccessToken = null
        prefs.backendRefreshToken = null
        prefs.backendUserId = null
        prefs.backendEmail = null
        prefs.backendRole = null
        prefs.backendExpiresAt = 0L
    }

    override fun isLoggedIn(): Boolean =
        !prefs.backendAccessToken.isNullOrBlank() &&
            prefs.backendExpiresAt > System.currentTimeMillis() - 30_000L

    override fun bearer(): String? =
        prefs.backendAccessToken?.let { "Bearer $it" }

    override fun refreshToken(): String? = prefs.backendRefreshToken

    override fun role(): String? = prefs.backendRole

    override fun isNgoRole(): Boolean {
        val r = prefs.backendRole ?: return false
        return r == "NGO_STAFF" || r == "NGO_ADMIN"
    }

    override fun isUserRole(): Boolean = prefs.backendRole == "USER"
}
