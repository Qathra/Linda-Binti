package com.example.lindabinti.data.content

data class Hotline(
    val name: String,
    val number: String,
    val note: String
)

data class EducationModule(
    val id: String,
    val title: String,
    val summary: String,
    val body: String,
    /** Optional `res/raw` name for a short bundled educational clip. */
    val videoRawName: String? = null
)

data class RiskQuestion(
    val id: String,
    val text: String,
    /** Positive increases risk; negative decreases when answered Yes. */
    val weightIfYes: Int
)

object LocalContent {

    fun hotlines(language: String): List<Hotline> =
        when (SupportedLanguages.catalogCode(language)) {
            SupportedLanguages.SW -> hotlinesSw
            SupportedLanguages.MAA -> hotlinesMaa
            else -> hotlinesEn
        }

    fun supportServices(language: String): List<SupportService> =
        SupportServicesCatalog.services(language)

    fun educationModules(language: String): List<EducationModule> =
        EducationCatalog.allLessons(language)

    fun educationCategories(language: String): List<EducationCategory> =
        EducationCatalog.categories(language)

    fun riskQuestions(language: String): List<RiskQuestion> =
        when (SupportedLanguages.catalogCode(language)) {
            SupportedLanguages.SW -> riskSw
            SupportedLanguages.MAA -> riskMaa
            else -> riskEn
        }

    private val hotlinesEn = listOf(
        Hotline("Kenya Police / Emergency", "999", "National emergency"),
        Hotline("Emergency (alt)", "112", "Works on most networks"),
        Hotline("GBV Helpline", "1195", "Gender-based violence support"),
        Hotline("Childline Kenya", "116", "Children in danger / FGM risk")
    )

    private val hotlinesSw = listOf(
        Hotline("Polisi / Dharura ya Kenya", "999", "Dharura ya kitaifa"),
        Hotline("Dharura (mbadala)", "112", "Inafanya kazi kwenye mitandao mingi"),
        Hotline("Simu ya GBV", "1195", "Msaada wa unyanyasaji wa kijinsia"),
        Hotline("Childline Kenya", "116", "Watoto walio hatarini / hatari ya FGM")
    )

    private val hotlinesMaa = listOf(
        Hotline("Polisi / Oldeke le Kenya", "999", "Oldeke le nchoo"),
        Hotline("Oldeke (neng'eni)", "112", "Kejap sidai enkigwanak line e neng'eni"),
        Hotline("Esimu ya GBV", "1195", "Minto olchani olkiret oltungani"),
        Hotline("Childline Kenya", "116", "Nchorro oltungani / olchani le FGM")
    )

    private val riskEn = listOf(
        RiskQuestion("q1", "Have you been threatened with violence recently?", 2),
        RiskQuestion("q2", "Are you being watched, controlled, or stopped from using your phone?", 2),
        RiskQuestion("q3", "Is there pressure for FGM, forced marriage, or leaving school?", 3),
        RiskQuestion("q4", "Do you have at least one person you can call safely?", -1),
        RiskQuestion("q5", "Do you feel unsafe in your home right now?", 3)
    )

    private val riskSw = listOf(
        RiskQuestion("q1", "Je, umetishiwa kwa vurugu hivi karibuni?", 2),
        RiskQuestion("q2", "Je, unaangaliwa, kudhibitiwa, au kuzuiwa kutumia simu yako?", 2),
        RiskQuestion("q3", "Je, kuna shinikizo la FGM, ndoa ya kulazimishwa, au kuacha shule?", 3),
        RiskQuestion("q4", "Je, una angalau mtu mmoja unayeweza kumpigia simu kwa usalama?", -1),
        RiskQuestion("q5", "Je, unahisi hatari nyumbani kwako sasa hivi?", 3)
    )

    private val riskMaa = listOf(
        RiskQuestion("q1", "Kejap aitisho olchani oltungani oleng'ata naa?", 2),
        RiskQuestion("q2", "Kejap aing'oru, aitum, melo aitum esimu enye?", 2),
        RiskQuestion("q3", "Kejap enkiteng neme FGM, enkaiya enkaina, melo aar enkolong?", 3),
        RiskQuestion("q4", "Kejap oltungani oleng'ata naa aisho sidai?", -1),
        RiskQuestion("q5", "Kejap aing'oru olchani enkaji enye naa?", 3)
    )
}
