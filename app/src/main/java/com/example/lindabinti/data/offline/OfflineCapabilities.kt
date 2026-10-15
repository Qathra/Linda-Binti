package com.example.lindabinti.data.offline

/**
 * Declares which Linda Binti features work without internet, and what else they need.
 * Used by the Feature Phone Help screen and offline-first tests.
 */
enum class ConnectivityNeed {
    NONE,
    CELLULAR_SMS,
    INTERNET,
    USSD_PROVIDER
}

data class OfflineCapability(
    val id: String,
    val title: String,
    val worksWithoutInternet: Boolean,
    val need: ConnectivityNeed,
    val androidPermissions: List<String>,
    val needsBackend: Boolean,
    val needsUssdProvider: Boolean
)

object OfflineCapabilities {

    val education = OfflineCapability(
        id = "education",
        title = "Education",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val safetyGuidance = OfflineCapability(
        id = "safety_guidance",
        title = "Safety guidance",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val survivorSupport = OfflineCapability(
        id = "survivor_support",
        title = "Survivor emotional support",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val riskQuestions = OfflineCapability(
        id = "risk_questions",
        title = "Risk awareness questions",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val localReports = OfflineCapability(
        id = "local_reports",
        title = "Local report storage",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val trustedContacts = OfflineCapability(
        id = "trusted_contacts",
        title = "Trusted contacts",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val sosSms = OfflineCapability(
        id = "sos_sms",
        title = "SOS SMS",
        worksWithoutInternet = true,
        need = ConnectivityNeed.CELLULAR_SMS,
        androidPermissions = listOf("SEND_SMS"),
        needsBackend = false,
        needsUssdProvider = false
    )
    val emergencyGuidance = OfflineCapability(
        id = "emergency_guidance",
        title = "Emergency guidance",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val supportServices = OfflineCapability(
        id = "support_services",
        title = "Support-service information",
        worksWithoutInternet = true,
        need = ConnectivityNeed.NONE,
        androidPermissions = emptyList(),
        needsBackend = false,
        needsUssdProvider = false
    )
    val reportSync = OfflineCapability(
        id = "report_sync",
        title = "Optional cloud report sync",
        worksWithoutInternet = false,
        need = ConnectivityNeed.INTERNET,
        androidPermissions = emptyList(),
        needsBackend = true,
        needsUssdProvider = false
    )
    val stakeholderDashboard = OfflineCapability(
        id = "stakeholder_dashboard",
        title = "Stakeholder dashboard",
        worksWithoutInternet = false,
        need = ConnectivityNeed.INTERNET,
        androidPermissions = emptyList(),
        needsBackend = true,
        needsUssdProvider = false
    )
    val ussdFeaturePhone = OfflineCapability(
        id = "ussd_feature_phone",
        title = "USSD / feature-phone channel",
        worksWithoutInternet = false,
        need = ConnectivityNeed.USSD_PROVIDER,
        androidPermissions = emptyList(),
        needsBackend = true,
        needsUssdProvider = true
    )

    val all: List<OfflineCapability> = listOf(
        education,
        safetyGuidance,
        survivorSupport,
        riskQuestions,
        localReports,
        trustedContacts,
        sosSms,
        emergencyGuidance,
        supportServices,
        reportSync,
        stakeholderDashboard,
        ussdFeaturePhone
    )

    fun offlineCore(): List<OfflineCapability> = all.filter { it.worksWithoutInternet }

    const val NO_LIVE_SHORTCODE =
        "This Android app is not a USSD service and has no live nationwide shortcode."
    const val NO_SAFARICOM_CLAIM =
        "No Safaricom or other telecom USSD integration is configured unless a licensed gateway secret is set on the backend."
}
