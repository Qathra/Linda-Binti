package com.lindabinti.backend.dev

import com.lindabinti.backend.Role
import com.lindabinti.backend.db.Database
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.UserRepository

/**
 * Development-only CLI entry point. Prints a one-time temporary password to stdout.
 * Never logs or persists the plaintext password.
 */
fun main() {
    if (!DevEnvironment.allowsAdminPasswordReset()) {
        System.err.println(
            "ERROR: Dev admin password reset is disabled. " +
                "Set LINDA_DEV_ADMIN_RESET=1 and use a local SQLite database outside production."
        )
        System.exit(1)
    }

    val email = (System.getenv("LINDA_SEED_ADMIN_EMAIL") ?: "admin@linda.local").lowercase().trim()
    val jdbc = System.getenv("LINDA_DB_URL") ?: "jdbc:sqlite:./data/linda_binti_backend.db"
    val db = when {
        jdbc.startsWith("jdbc:sqlite:") -> Database.fileDb(jdbc.removePrefix("jdbc:sqlite:"))
        else -> {
            System.err.println("ERROR: Dev admin reset requires a local SQLite database.")
            System.exit(1)
            throw IllegalStateException()
        }
    }
    db.migrate()

    val users = UserRepository(db)
    val audit = AuditService(db)
    val tempPassword = SecurePasswordGenerator.generate()

    val existing = users.findByEmail(email)
    if (existing == null) {
        users.create(email, tempPassword, "NGO Admin", Role.NGO_ADMIN, mustChangePassword = true)
    } else {
        require(existing.role == Role.NGO_ADMIN) {
            "Account $email exists but is not NGO_ADMIN (role=${existing.role}). Reset aborted."
        }
        users.updatePassword(existing.id, tempPassword, mustChangePassword = true)
    }
    audit.log("system", "DEV_ADMIN_PASSWORD_RESET", "email=$email;must_change=true")

    println("=== Linda Binti development admin password reset ===")
    println("Account: $email")
    println("Role: NGO_ADMIN")
    println("Temporary password: $tempPassword")
    println("Password change required after first login: YES")
    println("Development-only reset: YES")
    println("This password is shown once here only. It is not stored in logs, API responses, or source code.")
}
