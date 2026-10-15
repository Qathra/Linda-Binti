package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedLanguagesTest {

    @Test
    fun normalizesAliases() {
        assertEquals(SupportedLanguages.SW, SupportedLanguages.normalize("sw-KE"))
        assertEquals(SupportedLanguages.MAA, SupportedLanguages.normalize("maasai"))
        assertEquals(SupportedLanguages.SO, SupportedLanguages.normalize("somali"))
        assertEquals(SupportedLanguages.EN, SupportedLanguages.normalize("fr"))
    }

    @Test
    fun futureLanguagesFallBackToEnglishCatalog() {
        assertEquals(SupportedLanguages.EN, SupportedLanguages.catalogCode("so"))
        assertEquals(SupportedLanguages.SW, SupportedLanguages.catalogCode("sw"))
        assertEquals(SupportedLanguages.MAA, SupportedLanguages.catalogCode("maa"))
        assertFalse(SupportedLanguages.isFutureLanguage("maa"))
        assertTrue(SupportedLanguages.isFutureLanguage("so"))
        assertFalse(SupportedLanguages.isFutureLanguage("en"))
    }

    @Test
    fun androidLocaleTagsAreStableForResourcePacks() {
        assertEquals("en", SupportedLanguages.androidLocaleTag("en"))
        assertEquals("sw", SupportedLanguages.androidLocaleTag("sw-KE"))
        assertEquals("mas", SupportedLanguages.androidLocaleTag("maa"))
        assertEquals("mas", SupportedLanguages.androidLocaleTag("maasai"))
        assertEquals("so", SupportedLanguages.androidLocaleTag("somali"))
    }
}
