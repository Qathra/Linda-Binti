package com.lindabinti.backend.services

import com.lindabinti.backend.Role
import com.lindabinti.backend.db.Database
import com.lindabinti.backend.security.ForbiddenException
import com.lindabinti.backend.security.JwtService
import com.lindabinti.backend.security.PasswordHasher
import com.lindabinti.backend.security.TokenPair

class AuthService(
    private val users: UserRepository,
    private val jwt: JwtService,
    private val audit: AuditService,
    private val refreshTokens: RefreshTokenStore
) {
    constructor(
        users: UserRepository,
        jwt: JwtService,
        audit: AuditService,
        db: Database
    ) : this(users, jwt, audit, RefreshTokenStore(db))

    fun registerUser(email: String, password: String, displayName: String): AuthResult {
        val user = users.create(email, password, displayName, Role.USER)
        val tokens = issueAndStore(user)
        audit.log(user.id, "USER_REGISTER", "email=${user.email}")
        return AuthResult(tokens, user.id, user.role, user.email, user.mustChangePassword)
    }

    fun login(email: String, password: String): AuthResult {
        val user = users.findByEmail(email) ?: throw AuthException("Invalid credentials")
        if (!PasswordHasher.verify(password, user.passwordHash)) {
            audit.log(user.id, "LOGIN_FAILED", "email=${user.email}")
            throw AuthException("Invalid credentials")
        }
        val tokens = issueAndStore(user)
        audit.log(user.id, "LOGIN_SUCCESS", "role=${user.role};must_change=${user.mustChangePassword}")
        return AuthResult(tokens, user.id, user.role, user.email, user.mustChangePassword)
    }

    /**
     * Silent phone identity: opening the app is consent.
     * Device UUID is hashed into an email so NGO never sees a hardware id.
     */
    fun ensureDeviceUser(deviceId: String): AuthResult {
        val id = deviceId.trim().lowercase()
        require(id.matches(Regex("[a-z0-9\\-]{8,64}"))) { "Invalid device id" }
        val email = "device.${id.take(36)}@linda.device"
        val user = users.findByEmail(email) ?: users.create(
            email = email,
            password = java.util.UUID.randomUUID().toString() + "Aa1!",
            displayName = "Phone",
            role = Role.USER
        )
        val tokens = issueAndStore(user)
        audit.log(user.id, "DEVICE_SESSION", "role=${user.role}")
        return AuthResult(tokens, user.id, user.role, user.email, user.mustChangePassword)
    }

    fun createStaff(actorId: String, email: String, password: String, role: Role): UserRecord {
        require(role == Role.NGO_STAFF || role == Role.NGO_ADMIN) { "Invalid staff role" }
        val created = users.create(email, password, displayName = email.substringBefore("@"), role = role)
        audit.log(actorId, "STAFF_CREATE", "email=${created.email};role=${created.role}")
        return created
    }

    fun refresh(refreshToken: String): AuthResult {
        val claims = try {
            jwt.parseRefreshClaims(refreshToken)
        } catch (_: Exception) {
            throw AuthException("Invalid refresh token")
        }
        if (!refreshTokens.isActive(claims.tokenId)) {
            audit.log(claims.userId, "TOKEN_REFRESH_DENIED", "revoked_or_unknown")
            throw AuthException("Invalid refresh token")
        }
        val user = users.findById(claims.userId) ?: throw AuthException("Invalid refresh token")
        refreshTokens.revoke(claims.tokenId)
        val tokens = issueAndStore(user)
        audit.log(user.id, "TOKEN_REFRESH", "role=${user.role};rotated=true")
        return AuthResult(tokens, user.id, user.role, user.email, user.mustChangePassword)
    }

    fun logout(userId: String, refreshToken: String? = null) {
        if (!refreshToken.isNullOrBlank()) {
            runCatching {
                val claims = jwt.parseRefreshClaims(refreshToken)
                if (claims.userId == userId) refreshTokens.revoke(claims.tokenId)
            }
        }
        refreshTokens.revokeAllForUser(userId)
        audit.log(userId, "LOGOUT", "refresh_revoked=true")
    }

    fun changePassword(userId: String, currentPassword: String, newPassword: String): AuthResult {
        val user = users.findById(userId) ?: throw AuthException("Invalid credentials")
        if (user.role != Role.NGO_ADMIN) {
            throw ForbiddenException("Only NGO administrators may change passwords here")
        }
        if (!PasswordHasher.verify(currentPassword, user.passwordHash)) {
            audit.log(userId, "PASSWORD_CHANGE_FAILED", "invalid_current")
            throw AuthException("Invalid current password")
        }
        require(newPassword.length >= 8) { "Password must be at least 8 characters" }
        require(newPassword != currentPassword) { "New password must differ from current password" }
        users.updatePassword(userId, newPassword, mustChangePassword = false)
        refreshTokens.revokeAllForUser(userId)
        audit.log(userId, "PASSWORD_CHANGED", "must_change_cleared=true;sessions_revoked=true")
        val refreshed = users.findById(userId) ?: user
        val tokens = issueAndStore(refreshed)
        return AuthResult(tokens, refreshed.id, refreshed.role, refreshed.email, mustChangePassword = false)
    }

    private fun issueAndStore(user: UserRecord): TokenPair {
        val tokens = jwt.issue(user.id, user.email, user.role)
        if (tokens.refreshTokenId.isNotBlank()) {
            refreshTokens.store(tokens.refreshTokenId, user.id)
        }
        return tokens
    }
}

data class AuthResult(
    val tokens: TokenPair,
    val userId: String,
    val role: Role,
    val email: String,
    val mustChangePassword: Boolean = false
)

class AuthException(message: String) : RuntimeException(message)
