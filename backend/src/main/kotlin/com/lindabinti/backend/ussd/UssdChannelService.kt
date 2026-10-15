package com.lindabinti.backend.ussd

import com.lindabinti.backend.Role
import com.lindabinti.backend.api.UssdInboundRequest
import com.lindabinti.backend.api.UssdInboundResponse
import com.lindabinti.backend.services.AuditService
import com.lindabinti.backend.services.ReportService
import com.lindabinti.backend.services.UserRepository
import java.util.UUID

/**
 * Provider-neutral USSD boundary:
 * Feature phone → licensed USSD gateway (e.g. Africa's Talking) → this service →
 * anonymized GBV/FGM case → NGO dashboard.
 *
 * Does not persist MSISDN.
 */
class UssdChannelService(
    private val users: UserRepository,
    private val reports: ReportService,
    private val audit: AuditService,
    private val gatewayAuth: UssdGatewayAuth
) {
    val gatewayConfigured: Boolean get() = gatewayAuth.configured

    fun ensureChannelUser() {
        if (users.findByEmail(CHANNEL_EMAIL) != null) return
        users.create(
            email = CHANNEL_EMAIL,
            password = UUID.randomUUID().toString() + "Aa1!",
            displayName = "USSD channel",
            role = Role.USER
        )
    }

    fun handleInbound(request: UssdInboundRequest, gatewayKey: String?): UssdInboundResponse {
        if (!gatewayAuth.accepts(gatewayKey)) {
            throw UssdUnauthorizedException()
        }
        return process(request)
    }

    /** In-browser / panel simulator — same menu, no telecom gateway key. */
    fun handleDemoInbound(request: UssdInboundRequest): UssdInboundResponse = process(request)

    private fun process(request: UssdInboundRequest): UssdInboundResponse {
        val menu = UssdMenuService.handle(request.sessionId, request.text, request.serviceCode)
        var reportAccepted = false
        var referenceId = ""
        if (menu.action != UssdAction.NONE) {
            ensureChannelUser()
            val channelUser = users.findByEmail(CHANNEL_EMAIL)
                ?: throw IllegalStateException("USSD channel user missing")
            val spec = persistSpec(menu)
            val clientUuid = persistKey(request.sessionId, menu.action)
            val stored = reports.upsertOwnedReport(
                userId = channelUser.id,
                clientUuid = clientUuid,
                categoryId = spec.categoryId,
                categoryLabel = spec.categoryLabel,
                body = spec.body,
                areaCode = menu.areaCode.ifBlank { null },
                areaLabel = menu.areaLabel.ifBlank { null },
                areaType = if (menu.areaCode.isNotBlank()) "SUB_COUNTY" else null,
                createdAt = System.currentTimeMillis(),
                includeBody = spec.body.isNotBlank()
            )
            reportAccepted = true
            referenceId = ReportService.referenceId(stored.serverId)
        }
        audit.log(
            "ussd",
            "USSD_INBOUND",
            "end=${menu.endSession};action=${menu.action};queued=$reportAccepted;msisdn=omitted"
        )
        return UssdInboundResponse(
            message = menu.message,
            endSession = menu.endSession,
            note = menu.note,
            channelStatus = if (gatewayAuth.configured) "gateway_key_configured" else "stub",
            reportAccepted = reportAccepted,
            referenceId = referenceId
        )
    }

    private fun persistSpec(menu: UssdResult): PersistSpec {
        val kind = when (menu.action) {
            UssdAction.REPORT_GBV -> "GBV"
            UssdAction.REPORT_FGM -> "FGM"
            UssdAction.SAFETY_REQUEST -> "safety"
            UssdAction.NONE -> ""
        }
        val categoryId = when (menu.action) {
            UssdAction.REPORT_GBV -> "other"
            UssdAction.REPORT_FGM -> "fgm_pressure"
            UssdAction.SAFETY_REQUEST -> "threats"
            UssdAction.NONE -> ""
        }
        val name = menu.reporterName.trim()
        val area = menu.areaLabel.ifBlank { "Kajiado" }
        val label = if (name.isNotBlank()) "USSD $kind · $name" else "USSD $kind report"
        val body = if (name.isNotBlank()) {
            "$name needs help with $kind in $area."
        } else {
            ""
        }
        return PersistSpec(categoryId, label, body)
    }

    private data class PersistSpec(
        val categoryId: String,
        val categoryLabel: String,
        val body: String
    )

    private fun persistKey(sessionId: String, action: UssdAction): String {
        val session = sessionId.trim().ifBlank { UUID.randomUUID().toString() }
        return "ussd:$session:${action.name}"
    }

    companion object {
        const val CHANNEL_EMAIL = "ussd.channel@linda.local"
    }
}
