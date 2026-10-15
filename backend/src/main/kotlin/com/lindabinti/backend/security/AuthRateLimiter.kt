package com.lindabinti.backend.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Simple in-memory rate limit for auth endpoints (login/register/refresh).
 * Keyed by client identifier (IP + action). Not a substitute for edge WAF.
 */
class AuthRateLimiter(
    private val maxAttempts: Int = 25,
    private val windowMs: Long = 60_000L
) {
    private val hits = ConcurrentHashMap<String, MutableList<Long>>()

    fun checkOrThrow(key: String) {
        val now = System.currentTimeMillis()
        val list = hits.getOrPut(key) { mutableListOf() }
        synchronized(list) {
            list.removeAll { now - it > windowMs }
            if (list.size >= maxAttempts) {
                throw RateLimitException("Too many attempts. Try again shortly.")
            }
            list += now
        }
    }
}

class RateLimitException(message: String) : RuntimeException(message)
