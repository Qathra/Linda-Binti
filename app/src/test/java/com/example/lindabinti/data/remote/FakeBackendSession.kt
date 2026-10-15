package com.example.lindabinti.data.remote

class FakeBackendSession : BackendSession {
    var auth: AuthResponseDto? = null

    override fun saveSession(auth: AuthResponseDto) {
        this.auth = auth
    }

    override fun clear() {
        auth = null
    }

    override fun isLoggedIn(): Boolean = auth != null && !auth!!.accessToken.isBlank()

    override fun bearer(): String? = auth?.accessToken?.let { "Bearer $it" }

    override fun refreshToken(): String? = auth?.refreshToken

    override fun role(): String? = auth?.role

    override fun isNgoRole(): Boolean {
        val r = auth?.role ?: return false
        return r == "NGO_STAFF" || r == "NGO_ADMIN"
    }

    override fun isUserRole(): Boolean = auth?.role == "USER"
}