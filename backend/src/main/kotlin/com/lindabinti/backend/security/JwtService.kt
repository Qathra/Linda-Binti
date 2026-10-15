package com.lindabinti.backend.security

import com.lindabinti.backend.Role
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochMs: Long,
    val refreshTokenId: String = ""
)

data class AccessClaims(
    val userId: String,
    val role: Role,
    val email: String
) : io.ktor.server.auth.Principal

data class RefreshClaims(
    val userId: String,
    val tokenId: String
)

class JwtService(
    secret: String,
    private val accessTtlMs: Long = 60 * 60 * 1000L,
    private val refreshTtlMs: Long = 30L * 24 * 60 * 60 * 1000L
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(
        secret.toByteArray(StandardCharsets.UTF_8).let { bytes ->
            if (bytes.size >= 32) bytes else (bytes + ByteArray(32 - bytes.size))
        }
    )

    fun issue(userId: String, email: String, role: Role): TokenPair {
        val now = System.currentTimeMillis()
        val exp = now + accessTtlMs
        val access = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("email", email)
            .claim("role", role.name)
            .issuedAt(Date(now))
            .expiration(Date(exp))
            .signWith(key)
            .compact()
        val refreshId = UUID.randomUUID().toString()
        val refresh = Jwts.builder()
            .id(refreshId)
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(Date(now))
            .expiration(Date(now + refreshTtlMs))
            .signWith(key)
            .compact()
        return TokenPair(access, refresh, exp, refreshTokenId = refreshId)
    }

    fun parseAccess(token: String): AccessClaims {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        if (claims["type"] == "refresh") {
            throw io.jsonwebtoken.JwtException("refresh_token_not_allowed")
        }
        val roleRaw = claims["role"] as? String
            ?: throw io.jsonwebtoken.JwtException("missing_role")
        val email = claims["email"] as? String
            ?: throw io.jsonwebtoken.JwtException("missing_email")
        return AccessClaims(
            userId = claims.subject,
            role = Role.parse(roleRaw),
            email = email
        )
    }

    fun parseRefresh(token: String): String = parseRefreshClaims(token).userId

    fun parseRefreshClaims(token: String): RefreshClaims {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        if (claims["type"] != "refresh") {
            throw io.jsonwebtoken.JwtException("not_refresh_token")
        }
        val id = claims.id ?: throw io.jsonwebtoken.JwtException("missing_token_id")
        return RefreshClaims(userId = claims.subject, tokenId = id)
    }
}