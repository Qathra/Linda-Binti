package com.example.lindabinti.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lindabinti.data.content.SupportedLanguages
import java.util.Locale

object LocaleHelper {

    fun normalizeTag(languageTag: String): String =
        SupportedLanguages.normalize(languageTag)

    /**
     * Android string resources: English (`values`) and Kiswahili (`values-sw`).
     * Maa uses `values-mas` and Somali uses `values-so` as placeholder packs
     * until verified translations are added. Missing keys fall back to English.
     * Education/catalog copy uses [com.example.lindabinti.data.content.Copy.t].
     */
    fun androidResourceTag(languageTag: String): String =
        SupportedLanguages.androidLocaleTag(languageTag)

    fun wrap(context: Context, languageTag: String): Context {
        val tag = androidResourceTag(languageTag)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLanguage(languageTag: String) {
        val tag = androidResourceTag(languageTag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
