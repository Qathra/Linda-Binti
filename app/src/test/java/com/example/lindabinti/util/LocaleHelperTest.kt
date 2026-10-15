package com.example.lindabinti.util

import com.example.lindabinti.data.content.SupportedLanguages
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleHelperTest {

    @Test
    fun resourceTagsMatchAndroidQualifiers() {
        assertEquals("en", LocaleHelper.androidResourceTag("en"))
        assertEquals("sw", LocaleHelper.androidResourceTag("sw"))
        assertEquals("mas", LocaleHelper.androidResourceTag("maa"))
        assertEquals("so", LocaleHelper.androidResourceTag("so"))
    }

    @Test
    fun maaUsesMasResourceQualifierAndCatalog() {
        assertEquals("mas", LocaleHelper.androidResourceTag("maa"))
        assertEquals(SupportedLanguages.MAA, SupportedLanguages.catalogCode("maa"))
    }
}
