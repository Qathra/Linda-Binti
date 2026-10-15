package com.example.lindabinti.data.remote

/**
 * Pure helpers for backend base URL handling (unit-testable without network).
 */
object BackendUrlConfig {

    /** Returns null when blank (backend disabled). Otherwise ensures a trailing slash. */
    fun normalize(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (trimmed.isEmpty()) return null
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    fun isConfigured(baseUrl: String): Boolean = !normalize(baseUrl).isNullOrBlank()

    /**
     * Physical tablets cannot reach the emulator gateway (10.0.2.2) or device loopback.
     * Debug LAN builds must use the laptop LAN IP (e.g. 192.168.x.x).
     */
    fun assertPhysicalDeviceSafe(normalizedBaseUrl: String) {
        val host = runCatching {
            normalizedBaseUrl.trimEnd('/').substringAfter("://").substringBefore("/").substringBefore(":")
        }.getOrNull().orEmpty().lowercase()
        require(host.isNotBlank()) { "Backend base URL missing host: $normalizedBaseUrl" }
        require(host != "localhost" && host != "127.0.0.1" && host != "::1" && host != "0.0.0.0") {
            "Backend host '$host' is not reachable from a physical device. Use the laptop LAN IP."
        }
        require(host != "10.0.2.2") {
            "Backend host 10.0.2.2 is emulator-only and will fail on a physical tablet."
        }
    }

    fun isLanIpHost(normalizedBaseUrl: String): Boolean {
        val host = normalizedBaseUrl.trimEnd('/').substringAfter("://").substringBefore("/").substringBefore(":")
        return host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) &&
            !host.startsWith("127.") &&
            host != "0.0.0.0" &&
            host != "10.0.2.2"
    }

    /** Release builds must not open a Retrofit client against cleartext HTTP. */
    fun rejectCleartextInRelease(normalizedUrl: String, debug: Boolean): Boolean {
        if (debug) return false
        return normalizedUrl.startsWith("http://", ignoreCase = true)
    }
}
