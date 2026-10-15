package com.lindabinti.backend.security

import com.lindabinti.backend.Role

object RoleGuard {
    fun requireRole(actual: Role, vararg allowed: Role) {
        if (actual !in allowed) {
            throw ForbiddenException("Role ${actual.name} is not permitted for this action")
        }
    }

    fun canReadAggregates(role: Role): Boolean =
        role == Role.NGO_STAFF || role == Role.NGO_ADMIN

    fun canManageStaff(role: Role): Boolean = role == Role.NGO_ADMIN

    fun canReadOwnReports(role: Role): Boolean = role == Role.USER

    /** NGO roles must never receive raw private report bodies. */
    fun canReadRawReports(role: Role): Boolean = role == Role.USER

    fun canReadReportFlags(role: Role): Boolean =
        role == Role.NGO_STAFF || role == Role.NGO_ADMIN

    fun canAccessDashboard(role: Role): Boolean =
        role == Role.NGO_STAFF || role == Role.NGO_ADMIN

    fun canUpdateCaseStatus(role: Role): Boolean =
        role == Role.NGO_STAFF || role == Role.NGO_ADMIN

    fun requireDashboardDataAccess(mustChangePassword: Boolean) {
        if (mustChangePassword) {
            throw PasswordChangeRequiredException()
        }
    }
}

class ForbiddenException(message: String) : RuntimeException(message)