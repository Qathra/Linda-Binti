package com.lindabinti.backend.ussd

import com.lindabinti.backend.EnvFileLoader

/**
 * Shared secret for licensed USSD gateways (Africa's Talking, etc.).
 * Empty secret = development stub (inbound allowed without key).
 */
data class UssdGatewayAuth(val secret: String) {
    val configured: Boolean get() = secret.isNotBlank()

    fun accepts(providedKey: String?): Boolean {
        if (!configured) return true
        val got = providedKey?.trim().orEmpty()
        return got.isNotEmpty() && got == secret
    }

    companion object {
        const val HEADER = "X-Linda-Ussd-Key"
        const val QUERY_PARAM = "key"

        fun fromEnv(): UssdGatewayAuth {
            val primary = (
                EnvFileLoader.get("LINDA_USSD_GATEWAY_SECRET")
                    ?: System.getenv("LINDA_USSD_GATEWAY_SECRET")
                ).orEmpty().trim()
            val atKey = (
                EnvFileLoader.get("LINDA_USSD_AT_API_KEY")
                    ?: System.getenv("LINDA_USSD_AT_API_KEY")
                ).orEmpty().trim()
            return UssdGatewayAuth(primary.ifBlank { atKey })
        }

        fun disabled(): UssdGatewayAuth = UssdGatewayAuth("")
    }
}

class UssdUnauthorizedException : RuntimeException("ussd_gateway_unauthorized")
