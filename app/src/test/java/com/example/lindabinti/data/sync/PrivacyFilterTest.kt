package com.example.lindabinti.data.sync

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrivacyFilterTest {

    @Test
    fun rejectsPhoneLikeCategories() {
        assertNull(PrivacyFilter.sanitizeCategoryOrBand("+254712345678"))
    }

    @Test
    fun acceptsRiskBands() {
        assertEquals("high", PrivacyFilter.sanitizeCategoryOrBand("High"))
        assertNotNull(PrivacyFilter.sanitizeCategoryOrBand("physical"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun assertNoPiiRejectsBodyKey() {
        PrivacyFilter.assertNoPii(mapOf("body" to "secret"))
    }
}