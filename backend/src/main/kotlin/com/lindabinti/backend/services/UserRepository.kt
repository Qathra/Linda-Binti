package com.lindabinti.backend.services

import com.lindabinti.backend.Role
import com.lindabinti.backend.db.Database
import com.lindabinti.backend.security.PasswordHasher
import java.sql.ResultSet
import java.util.UUID

data class UserRecord(
    val id: String,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val role: Role,
    val createdAt: Long,
    val mustChangePassword: Boolean = false
)

class UserRepository(private val db: Database) {

    fun findByEmail(email: String): UserRecord? {
        db.conn().prepareStatement(
            "SELECT id, email, password_hash, display_name, role, created_at, must_change_password FROM users WHERE email = ?"
        ).use { ps ->
            ps.setString(1, email.lowercase().trim())
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return mapUser(rs)
            }
        }
    }

    fun findById(id: String): UserRecord? {
        db.conn().prepareStatement(
            "SELECT id, email, password_hash, display_name, role, created_at, must_change_password FROM users WHERE id = ?"
        ).use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (!rs.next()) return null
                return mapUser(rs)
            }
        }
    }

    fun create(
        email: String,
        password: String,
        displayName: String,
        role: Role,
        mustChangePassword: Boolean = false
    ): UserRecord {
        val normalized = email.lowercase().trim()
        require(normalized.contains("@")) { "Invalid email" }
        require(password.length >= 8) { "Password must be at least 8 characters" }
        require(findByEmail(normalized) == null) { "Email already registered" }
        val user = UserRecord(
            id = UUID.randomUUID().toString(),
            email = normalized,
            passwordHash = PasswordHasher.hash(password),
            displayName = displayName.trim().ifBlank { normalized.substringBefore("@") },
            role = role,
            createdAt = System.currentTimeMillis(),
            mustChangePassword = mustChangePassword
        )
        db.conn().prepareStatement(
            """
            INSERT INTO users(id, email, password_hash, display_name, role, created_at, must_change_password)
            VALUES (?,?,?,?,?,?,?)
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, user.id)
            ps.setString(2, user.email)
            ps.setString(3, user.passwordHash)
            ps.setString(4, user.displayName)
            ps.setString(5, user.role.name)
            ps.setLong(6, user.createdAt)
            ps.setInt(7, if (user.mustChangePassword) 1 else 0)
            ps.executeUpdate()
        }
        return user
    }

    fun updatePassword(userId: String, newPassword: String, mustChangePassword: Boolean) {
        require(newPassword.length >= 8) { "Password must be at least 8 characters" }
        val hash = PasswordHasher.hash(newPassword)
        db.conn().prepareStatement(
            "UPDATE users SET password_hash = ?, must_change_password = ? WHERE id = ?"
        ).use { ps ->
            ps.setString(1, hash)
            ps.setInt(2, if (mustChangePassword) 1 else 0)
            ps.setString(3, userId)
            ps.executeUpdate()
        }
    }

    fun listStaff(): List<UserRecord> {
        db.conn().prepareStatement(
            """
            SELECT id, email, password_hash, display_name, role, created_at, must_change_password
            FROM users WHERE role IN ('NGO_STAFF','NGO_ADMIN') ORDER BY created_at
            """.trimIndent()
        ).use { ps ->
            ps.executeQuery().use { rs ->
                val out = mutableListOf<UserRecord>()
                while (rs.next()) {
                    out += mapUser(rs)
                }
                return out
            }
        }
    }

    private fun mapUser(rs: ResultSet): UserRecord =
        UserRecord(
            id = rs.getString(1),
            email = rs.getString(2),
            passwordHash = rs.getString(3),
            displayName = rs.getString(4),
            role = Role.parse(rs.getString(5)),
            createdAt = rs.getLong(6),
            mustChangePassword = rs.getInt(7) == 1
        )
}
