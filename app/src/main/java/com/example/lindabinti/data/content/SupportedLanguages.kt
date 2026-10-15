package com.example.lindabinti.data.content

/**
 * Language codes the product is designed for.
 * English, Kiswahili, and Ol Maa/Maasai ship with full UI and safety catalogs.
 * Somali is reserved for a future translation pack.
 */
object SupportedLanguages {
    const val EN = "en"
    const val SW = "sw"
    const val MAA = "maa"
    const val SO = "so"

    fun normalize(code: String): String =
        when (code.trim().lowercase().replace('_', '-')) {
            "sw", "sw-ke" -> SW
            "maa", "mas", "maasai" -> MAA
            "so", "som", "somali", "so-so", "so-ke" -> SO
            else -> EN
        }

    /** UI/catalog content currently shipped. Somali falls back to English. */
    fun catalogCode(code: String): String =
        when (normalize(code)) {
            SW, MAA -> normalize(code)
            else -> EN
        }

    /**
     * Android resource qualifier. Kiswahili is `sw`, Somali is `so`,
     * Maa/Maasai uses ISO 639-2/3 `mas`. Missing keys fall back to English.
     */
    fun androidLocaleTag(code: String): String =
        when (normalize(code)) {
            SW -> "sw"
            SO -> "so"
            MAA -> "mas"
            else -> EN
        }

    fun isFutureLanguage(code: String): Boolean =
        normalize(code) == SO
}
