package com.example.lindabinti.data.content



/**

 * Bundled GBV/FGM support directory for Kajiado County.

 *

 * Phones are listed only when independently verified (national short codes).

 * Known public facilities without a verified switchboard have no phone.

 * Remote packs can replace text or add services later without redesigning screens.

 */

enum class SupportServiceArea {

    KENYA,

    KAJIADO

}



data class SupportService(

    val id: String,

    val name: String,

    val helpsWith: String,

    val kind: SupportServiceKind,

    val alsoKinds: Set<SupportServiceKind> = emptySet(),

    val phone: String?,

    val alternatePhone: String? = null,

    val phoneVerified: Boolean,

    val location: String,

    val locationVerified: Boolean,

    val referralGuidance: String,

    val area: SupportServiceArea

) {

    fun matches(kind: SupportServiceKind?): Boolean =

        kind == null || this.kind == kind || kind in alsoKinds



    fun callNumbers(): List<String> = buildList {

        if (phoneVerified) {

            phone?.takeIf { it.isNotBlank() }?.let(::add)

            alternatePhone?.takeIf { it.isNotBlank() }?.let(::add)

        }

    }

}



data class SupportServiceRecord(

    val id: String,

    val kind: SupportServiceKind,

    val alsoKinds: Set<SupportServiceKind> = emptySet(),

    val name: Copy,

    val helpsWith: Copy,

    val phone: String? = null,

    val alternatePhone: String? = null,

    val phoneVerified: Boolean = false,

    val location: Copy,

    val locationVerified: Boolean = false,

    val referralGuidance: Copy,

    val area: SupportServiceArea

) {

    fun resolve(language: String): SupportService = SupportService(

        id = id,

        name = name.t(language),

        helpsWith = helpsWith.t(language),

        kind = kind,

        alsoKinds = alsoKinds,

        phone = phone.takeIf { phoneVerified },

        alternatePhone = alternatePhone.takeIf { phoneVerified },

        phoneVerified = phoneVerified && !phone.isNullOrBlank(),

        location = location.t(language),

        locationVerified = locationVerified,

        referralGuidance = referralGuidance.t(language),

        area = area

    )

}



object SupportServicesCatalog {



    const val VERSION = 1



    val REQUIRED_PHONES = setOf("999", "112", "1195", "116")



    val directoryKinds = listOf(

        SupportServiceKind.EMERGENCY,

        SupportServiceKind.GBV,

        SupportServiceKind.CHILD_PROTECTION,

        SupportServiceKind.MEDICAL,

        SupportServiceKind.COUNSELLING,

        SupportServiceKind.POLICE,

        SupportServiceKind.FGM

    )



    val records: List<SupportServiceRecord> = listOf(

        SupportServiceRecord(

            id = "police_emergency",

            kind = SupportServiceKind.EMERGENCY,

            alsoKinds = setOf(SupportServiceKind.POLICE),

            name = copy(

                "Kenya Police / Emergency",

                "Polisi / Dharura ya Kenya",

                "Polisi / Oldeke le Kenya"

            ),

            helpsWith = copy(

                "Immediate danger, including GBV happening now.",

                "Hatari ya mara moja, pamoja na GBV inayotokea sasa.",

                "Olchani sidai, o ncho GBV ne aji sidai."

            ),

            phone = "999",

            alternatePhone = "112",

            phoneVerified = true,

            location = copy("Kenya-wide", "Kenya nzima", "Kenya eng'eni"),

            locationVerified = true,

            referralGuidance = copy(

                "Call if you are in danger now. Ask for the gender desk when you can. Hang up if staying on the line is unsafe. You do not have to explain everything first.",

                "Ukiwa hatarini sasa, piga simu. Uliza dawati la jinsia unapoweza. Kata simu ikiwa kubaki kwenye line si salama. Si lazima ueleze yote kwanza.",

                "Ne olchani sidai, aisho esimu. Aom gender desk ne aji. Aar esimu ne kubaki enkop line meji enkigwanak. Melo nchoo aeleze eng'eni oleng'ata."

            ),

            area = SupportServiceArea.KENYA

        ),

        SupportServiceRecord(

            id = "gbv_helpline",

            kind = SupportServiceKind.GBV,

            alsoKinds = setOf(SupportServiceKind.COUNSELLING),

            name = copy(

                "National GBV Helpline",

                "Simu ya Kitaifa ya GBV",

                "Esimu oldeke GBV"

            ),

            helpsWith = copy(

                "Confidential support for gender-based violence, including referral to counselling.",

                "Msaada wa faragha kwa unyanyasaji wa kijinsia, pamoja na rufaa ya ushauri nasaha.",

                "Minto eng'eni ne olchani olkiret oltungani, o ncho enkop olaiteng'en olchani."

            ),

            phone = "1195",

            phoneVerified = true,

            location = copy("Kenya-wide", "Kenya nzima", "Kenya eng'eni"),

            locationVerified = true,

            referralGuidance = copy(

                "This app is not counselling. 1195 can listen and guide you toward psychosocial support. Call when it is safe to speak.",

                "Programu hii si ushauri nasaha. 1195 inaweza kusikiliza na kukuongoza kwenye msaada wa kisaikolojia. Piga simu ikiwa ni salama kuzungumza.",

                "App eno meji olaiteng'en olchani. 1195 ne aji olng'obani na aing'oru enkop minto olchani. Aisho esimu ne enkigwanak olng'obani."

            ),

            area = SupportServiceArea.KENYA

        ),

        SupportServiceRecord(

            id = "childline",

            kind = SupportServiceKind.CHILD_PROTECTION,

            alsoKinds = setOf(SupportServiceKind.FGM),

            name = copy("Childline Kenya", "Childline Kenya", "Childline Kenya"),

            helpsWith = copy(

                "Child abuse, forced child marriage, or a girl at risk of FGM.",

                "Unyanyasaji wa watoto, ndoa ya kulazimishwa ya mtoto, au msichana aliye hatarini kwa FGM.",

                "Olchani nchorro, enkaiya enkaina nchorro, kata enkitok olchani FGM."

            ),

            phone = "116",

            phoneVerified = true,

            location = copy("Kenya-wide", "Kenya nzima", "Kenya eng'eni"),

            locationVerified = true,

            referralGuidance = copy(

                "An adult should call if a child is at risk. You can also tell a trusted adult. Do not confront abusers alone.",

                "Mtu mzima anapaswa kupiga simu mtoto akiwa hatarini. Unaweza pia kumwambia mtu mzima wa kuaminika. Usikabiliane na wanyanyasaji peke yako.",

                "Oltungani oleng'ata ne aji aisho esimu nchorro olchani. Ne aji pia aitum oltungani oleng'ata olaiteng'en. Melo aikabiliane na olaita olchani eng'eni."

            ),

            area = SupportServiceArea.KENYA

        ),

        SupportServiceRecord(

            id = "kajiado_referral_hospital",

            kind = SupportServiceKind.MEDICAL,

            alsoKinds = setOf(SupportServiceKind.FGM),

            name = copy(

                "Kajiado County Referral Hospital",

                "Hospital enkop Kajiado",

                "Hospital enkop Kajiado"

            ),

            helpsWith = copy(

                "Medical care after GBV or FGM-related harm.",

                "Matibabu baada ya GBV au madhara yanayohusiana na FGM.",

                "Minto olchani oleng'ata GBV kata olchani le FGM."

            ),

            phoneVerified = false,

            location = copy("Kajiado Town", "Mji wa Kajiado", "Enkop Kajiado"),

            locationVerified = true,

            referralGuidance = copy(

                "Ask for gender-based violence or reproductive health support when you arrive. Confirm the desk and hours locally. No switchboard is listed here because it is not verified in this app.",

                "Uliza msaada wa GBV au afya ya uzazi unapofika. Thibitisha dawati na saa za kazi pale. Hakuna nambari ya ofisi hapa kwa sababu haijathibitishwa katika programu hii.",

                "Aom minto GBV kata enkop olchani ne aing'oru. Nchoo gender desk na saa oleng'ata eno. Melo Namba ofisi ene ne meji sidai app eno."

            ),

            area = SupportServiceArea.KAJIADO

        ),

        SupportServiceRecord(

            id = "kajiado_gender_desk",

            kind = SupportServiceKind.GBV,

            name = copy(

                "County Gender Desk",

                "Gender desk enkop Kajiado",

                "Gender desk enkop Kajiado"

            ),

            helpsWith = copy(

                "Report GBV in Kajiado County and ask for a local referral.",

                "Ripoti GBV katika Kaunti ya Kajiado na uulize rufaa ya eneo.",

                "Ripoti GBV Kaunti ya Kajiado na aom enkop olaiteng'en enkop."

            ),

            phoneVerified = false,

            location = copy(

                "Kajiado County offices — confirm the current desk locally",

                "Ofisi za Kaunti ya Kajiado — thibitisha dawati la sasa",

                "Ofisi enkop Kajiado — nchoo gender desk eno sidai"

            ),

            locationVerified = false,

            referralGuidance = copy(

                "Confirm where the desk sits today before you travel. Do not wait at an office if it is unsafe. In danger, call 999 or 1195 instead.",

                "Thibitisha mahali dawati lilipo leo kabla ya kusafiri. Usisubiri ofisini ikiwa si salama. Ukiwa hatarini, piga 999 au 1195.",

                "Nchoo enkop gender desk eno oleng'ata aitubul. Melo eng'eti ofisini ne meji enkigwanak. Ne olchani sidai, aisho 999 kata 1195."

            ),

            area = SupportServiceArea.KAJIADO

        )

    )



    fun services(language: String, source: List<SupportServiceRecord> = records): List<SupportService> =

        source.map { it.resolve(language) }



    fun emergencyNumbers(): List<String> = listOf("999", "112", "1195", "116")

}


