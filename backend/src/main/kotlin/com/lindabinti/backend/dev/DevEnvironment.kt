package com.lindabinti.backend.dev

/**
 * Guards development-only tooling. Production deployments must not enable these paths.
 */
object DevEnvironment {

    fun allowsAdminPasswordReset(): Boolean {
        if (System.getenv("LINDA_ENV")?.equals("production", ignoreCase = true) == true) {
            return false
        }
        if (System.getenv("LINDA_DEV_ADMIN_RESET") != "1") {
            return false
        }
        val jdbc = System.getenv("LINDA_DB_URL") ?: "jdbc:sqlite:./data/linda_binti_backend.db"
        return jdbc.startsWith("jdbc:sqlite:") && !jdbc.contains(":memory:")
    }
}
