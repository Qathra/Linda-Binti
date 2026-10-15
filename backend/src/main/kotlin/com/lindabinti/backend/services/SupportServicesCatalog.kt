package com.lindabinti.backend.services

import com.lindabinti.backend.api.LocalizedTextDto
import com.lindabinti.backend.api.SupportServiceDto
import com.lindabinti.backend.api.SupportServicesCatalogDto

/**
 * Public GBV/FGM directory for Kajiado. Same IDs as the Android bundled pack.
 * Edit this list to update phones/text later; the app overlays it when online.
 * Do not add phone numbers that are not verified.
 */
object SupportServicesCatalog {

    const val VERSION = 1

    fun dto(): SupportServicesCatalogDto = SupportServicesCatalogDto(
        version = VERSION,
        source = "backend",
        services = services
    )

    private val services = listOf(
        SupportServiceDto(
            id = "police_emergency",
            kind = "emergency",
            alsoKinds = listOf("police"),
            name = text("Kenya Police / Emergency", "Polisi / Dharura ya Kenya"),
            helpsWith = text(
                "Immediate danger, including GBV happening now.",
                "Hatari ya mara moja, pamoja na GBV inayotokea sasa."
            ),
            phone = "999",
            alternatePhone = "112",
            phoneVerified = true,
            location = text("Kenya-wide", "Kenya nzima"),
            locationVerified = true,
            referralGuidance = text(
                "Call if you are in danger now. Ask for the gender desk when you can. Hang up if staying on the line is unsafe. You do not have to explain everything first.",
                "Ukiwa hatarini sasa, piga simu. Uliza dawati la jinsia unapoweza. Kata simu ikiwa kubaki kwenye line si salama. Si lazima ueleze yote kwanza."
            ),
            area = "kenya"
        ),
        SupportServiceDto(
            id = "gbv_helpline",
            kind = "gbv",
            alsoKinds = listOf("counselling"),
            name = text("National GBV Helpline", "Simu ya Kitaifa ya GBV"),
            helpsWith = text(
                "Confidential support for gender-based violence, including referral to counselling.",
                "Msaada wa faragha kwa unyanyasaji wa kijinsia, pamoja na rufaa ya ushauri nasaha."
            ),
            phone = "1195",
            phoneVerified = true,
            location = text("Kenya-wide", "Kenya nzima"),
            locationVerified = true,
            referralGuidance = text(
                "This app is not counselling. 1195 can listen and guide you toward psychosocial support. Call when it is safe to speak.",
                "Programu hii si ushauri nasaha. 1195 inaweza kusikiliza na kukuongoza kwenye msaada wa kisaikolojia. Piga simu ikiwa ni salama kuzungumza."
            ),
            area = "kenya"
        ),
        SupportServiceDto(
            id = "childline",
            kind = "child_protection",
            alsoKinds = listOf("fgm"),
            name = text("Childline Kenya", "Childline Kenya"),
            helpsWith = text(
                "Child abuse, forced child marriage, or a girl at risk of FGM.",
                "Unyanyasaji wa watoto, ndoa ya kulazimishwa ya mtoto, au msichana aliye hatarini kwa FGM."
            ),
            phone = "116",
            phoneVerified = true,
            location = text("Kenya-wide", "Kenya nzima"),
            locationVerified = true,
            referralGuidance = text(
                "An adult should call if a child is at risk. You can also tell a trusted adult. Do not confront abusers alone.",
                "Mtu mzima anapaswa kupiga simu mtoto akiwa hatarini. Unaweza pia kumwambia mtu mzima wa kuaminika. Usikabiliane na wanyanyasaji peke yako."
            ),
            area = "kenya"
        ),
        SupportServiceDto(
            id = "kajiado_referral_hospital",
            kind = "medical",
            alsoKinds = listOf("fgm"),
            name = text(
                "Kajiado County Referral Hospital",
                "Hospitali ya Rufaa ya Kaunti ya Kajiado"
            ),
            helpsWith = text(
                "Medical care after GBV or FGM-related harm.",
                "Matibabu baada ya GBV au madhara yanayohusiana na FGM."
            ),
            phoneVerified = false,
            location = text("Kajiado Town", "Mji wa Kajiado"),
            locationVerified = true,
            referralGuidance = text(
                "Ask for gender-based violence or reproductive health support when you arrive. Confirm the desk and hours locally. No switchboard is listed here because it is not verified in this app.",
                "Uliza msaada wa GBV au afya ya uzazi unapofika. Thibitisha dawati na saa za kazi pale. Hakuna nambari ya ofisi hapa kwa sababu haijathibitishwa katika programu hii."
            ),
            area = "kajiado"
        ),
        SupportServiceDto(
            id = "kajiado_gender_desk",
            kind = "gbv",
            name = text("County Gender Desk", "Dawati la Jinsia la Kaunti"),
            helpsWith = text(
                "Report GBV in Kajiado County and ask for a local referral.",
                "Ripoti GBV katika Kaunti ya Kajiado na uulize rufaa ya eneo."
            ),
            phoneVerified = false,
            location = text(
                "Kajiado County offices — confirm the current desk locally",
                "Ofisi za Kaunti ya Kajiado — thibitisha dawati la sasa"
            ),
            locationVerified = false,
            referralGuidance = text(
                "Confirm where the desk sits today before you travel. Do not wait at an office if it is unsafe. In danger, call 999 or 1195 instead.",
                "Thibitisha mahali dawati lilipo leo kabla ya kusafiri. Usisubiri ofisini ikiwa si salama. Ukiwa hatarini, piga 999 au 1195."
            ),
            area = "kajiado"
        )
    )

    private fun text(en: String, sw: String) = LocalizedTextDto(en = en, sw = sw)
}
