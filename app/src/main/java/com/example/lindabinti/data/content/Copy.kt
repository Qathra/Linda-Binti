package com.example.lindabinti.data.content

/**
 * Translatable education copy.
 *
 * English, Kiswahili, and Ol Maa/Maasai ship now. Somali can be filled later.
 * without changing screens — the UI only calls [t].
 */
data class Copy(
    val en: String,
    val sw: String,
    val maa: String? = null,
    val so: String? = null
) {
    fun t(language: String): String {
        val text = when (SupportedLanguages.normalize(language)) {
            SupportedLanguages.SW -> sw
            SupportedLanguages.MAA -> maa
            SupportedLanguages.SO -> so
            else -> en
        }
        return text?.takeIf { it.isNotBlank() } ?: when (SupportedLanguages.normalize(language)) {
            SupportedLanguages.MAA, SupportedLanguages.SO -> en
            else -> en
        }
    }
}

fun copy(en: String, sw: String, maa: String? = null): Copy = Copy(en = en, sw = sw, maa = maa)
