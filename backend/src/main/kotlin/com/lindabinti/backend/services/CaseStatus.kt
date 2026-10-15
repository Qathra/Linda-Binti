package com.lindabinti.backend.services

/**
 * Responder workflow status. Not a legal finding and not a medical outcome.
 */
object CaseStatus {
    const val NEW = "NEW"
    const val REVIEWING = "REVIEWING"
    const val REFERRED = "REFERRED"
    const val RESPONDING = "RESPONDING"
    const val RESOLVED = "RESOLVED"

    private val allowed = setOf(NEW, REVIEWING, REFERRED, RESPONDING, RESOLVED)

    fun parse(raw: String?): String? {
        val v = raw?.trim()?.uppercase() ?: return null
        return v.takeIf { it in allowed }
    }

    fun requireValid(raw: String?): String =
        parse(raw) ?: throw IllegalArgumentException("Invalid case status: $raw")

    fun isOpen(status: String): Boolean = parse(status) != RESOLVED
}