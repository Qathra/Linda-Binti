package com.lindabinti.backend.services

import com.lindabinti.backend.db.Database

/** Persists refresh token IDs so logout can revoke them. */
class RefreshTokenStore(private val db: Database) {

    fun store(tokenId: String, userId: String) {
        db.conn().prepareStatement(
            "INSERT OR REPLACE INTO refresh_tokens(token_id, user_id, revoked, created_at) VALUES (?,?,0,?)"
        ).use { ps ->
            ps.setString(1, tokenId)
            ps.setString(2, userId)
            ps.setLong(3, System.currentTimeMillis())
            ps.executeUpdate()
        }
    }

    fun isActive(tokenId: String): Boolean {
        db.conn().prepareStatement(
            "SELECT revoked FROM refresh_tokens WHERE token_id = ? LIMIT 1"
        ).use { ps ->
            ps.setString(1, tokenId)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return false
                return rs.getInt(1) == 0
            }
        }
    }

    fun revoke(tokenId: String) {
        db.conn().prepareStatement(
            "UPDATE refresh_tokens SET revoked = 1 WHERE token_id = ?"
        ).use { ps ->
            ps.setString(1, tokenId)
            ps.executeUpdate()
        }
    }

    fun revokeAllForUser(userId: String) {
        db.conn().prepareStatement(
            "UPDATE refresh_tokens SET revoked = 1 WHERE user_id = ?"
        ).use { ps ->
            ps.setString(1, userId)
            ps.executeUpdate()
        }
    }
}
