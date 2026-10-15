package com.example.lindabinti.data.remote

/**
 * Cloud session separate from Access PIN / SOS. Implementations must never store calculator codes.
 */
interface BackendSession {
    fun saveSession(auth: AuthResponseDto)
    fun clear()
    fun isLoggedIn(): Boolean
    fun bearer(): String?
    fun refreshToken(): String?
    fun role(): String?
    fun isNgoRole(): Boolean
    fun isUserRole(): Boolean
}
