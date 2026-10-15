package com.lindabinti.backend

import com.lindabinti.backend.db.Database
import com.lindabinti.backend.security.AuthRateLimiter
import com.lindabinti.backend.security.JwtService
import com.lindabinti.backend.security.RateLimitException
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.AuthService
import com.lindabinti.backend.services.UserRepository
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSecurityHardeningTest {

    @Test
    fun refreshTokenIsRevokedAfterLogout() {
        val db = Database.memory()
        db.migrate()
        val audit = AuditService(db)
        val users = UserRepository(db)
        val jwt = JwtService("test-secret-key-linda-binti-phase1-32b")
        val auth = AuthService(users, jwt, audit, db)
        val login = auth.registerUser("secure@example.com", "Password123!", "Sec")
        auth.logout(login.userId, login.tokens.refreshToken)
        assertThrows(Exception::class.java) {
            auth.refresh(login.tokens.refreshToken)
        }
    }

    @Test
    fun rateLimiterTripsAfterMaxAttempts() {
        val limiter = AuthRateLimiter(maxAttempts = 3, windowMs = 60_000)
        limiter.checkOrThrow("login:test")
        limiter.checkOrThrow("login:test")
        limiter.checkOrThrow("login:test")
        assertThrows(RateLimitException::class.java) {
            limiter.checkOrThrow("login:test")
        }
    }

    @Test
    fun rotatedRefreshInvalidatesPreviousToken() {
        val db = Database.memory()
        db.migrate()
        val auth = AuthService(UserRepository(db), JwtService("test-secret-key-linda-binti-phase1-32b"), AuditService(db), db)
        val first = auth.registerUser("rotate@example.com", "Password123!", "Rot")
        val second = auth.refresh(first.tokens.refreshToken)
        assertTrue(second.tokens.accessToken.isNotBlank())
        assertThrows(Exception::class.java) {
            auth.refresh(first.tokens.refreshToken)
        }
    }
}
