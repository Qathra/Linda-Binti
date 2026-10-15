package com.lindabinti.backend.services

import com.lindabinti.backend.db.Database

data class AuditEntry(
    val id: Long,
    val actorUserId: String,
    val action: String,
    val detail: String,
    val createdAt: Long
)

class AuditService(private val db: Database) {
    fun log(actorUserId: String, action: String, detail: String) {
        db.conn().prepareStatement(
            "INSERT INTO audit_log(actor_user_id, action, detail, created_at) VALUES (?,?,?,?)"
        ).use { ps ->
            ps.setString(1, actorUserId)
            ps.setString(2, action)
            ps.setString(3, detail.take(500))
            ps.setLong(4, System.currentTimeMillis())
            ps.executeUpdate()
        }
    }

    fun listRecent(limit: Int = 100): List<AuditEntry> {
        db.conn().prepareStatement(
            "SELECT id, actor_user_id, action, detail, created_at FROM audit_log ORDER BY id DESC LIMIT ?"
        ).use { ps ->
            ps.setInt(1, limit)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<AuditEntry>()
                while (rs.next()) {
                    out += AuditEntry(
                        id = rs.getLong(1),
                        actorUserId = rs.getString(2),
                        action = rs.getString(3),
                        detail = rs.getString(4),
                        createdAt = rs.getLong(5)
                    )
                }
                return out
            }
        }
    }
}