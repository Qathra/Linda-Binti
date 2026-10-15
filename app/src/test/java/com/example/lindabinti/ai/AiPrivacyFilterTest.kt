package com.example.lindabinti.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPrivacyFilterTest {

    @Test
    fun redactsEmailPhoneAndPinLikeSecrets() {
        val raw = "Call me at user@example.com or +254712345678. SOS code: 1102"
        val redacted = PrivacyFilter.redactForCloudAnalysis(raw)
        assertFalse(redacted.contains("user@example.com"))
        assertFalse(redacted.contains("712345678"))
        assertFalse(redacted.contains("1102"))
        assertTrue(redacted.contains("[redacted]"))
    }

    @Test
    fun prepareOnlineInputStripsPiiBeforeCloudCall() {
        val prepared = PrivacyFilter.prepareOnlineInput(
            AiAnalysisInput(
                categoryId = "physical",
                categoryLabel = "Physical",
                description = "My name is Jane Doe phone 0712345678",
                includeDescription = true
            )
        )
        assertFalse(prepared.description.contains("Jane Doe"))
        assertFalse(prepared.description.contains("0712345678"))
    }

    @Test
    fun excludesBodyWhenIncludeDescriptionFalse() {
        val prepared = PrivacyFilter.prepareOnlineInput(
            AiAnalysisInput(
                categoryId = "fgm_pressure",
                categoryLabel = "FGM pressure",
                description = "secret details",
                includeDescription = false
            )
        )
        assertEquals("", prepared.description)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsForbiddenCloudFields() {
        PrivacyFilter.assertSafeForCloud(mapOf("accessToken" to "abc"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPhoneLikeValuesInCloudPayload() {
        PrivacyFilter.assertSafeForCloud(mapOf("description" to "+254712345678"))
    }
}
