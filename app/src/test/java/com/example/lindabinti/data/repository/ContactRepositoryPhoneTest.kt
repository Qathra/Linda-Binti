package com.example.lindabinti.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactRepositoryPhoneTest {

    @Test
    fun kenyaLocalNormalized() {
        assertEquals("+254712345678", ContactRepository.normalizePhone("0712345678"))
    }

    @Test
    fun e164Preserved() {
        assertEquals("+254712345678", ContactRepository.normalizePhone("+254712345678"))
    }

    @Test
    fun blankAndShortRejected() {
        assertNull(ContactRepository.normalizePhone(""))
        assertNull(ContactRepository.normalizePhone("123"))
        assertNull(ContactRepository.normalizePhone("abc"))
    }

    @Test
    fun kenyaSmsRadioPrefersE164ForImsThenLocalGsm() {
        val candidates = ContactRepository.smsDestinationCandidates("0712345678")
        assertEquals(listOf("+254712345678", "0712345678", "254712345678"), candidates)
    }

    @Test
    fun e164SmsCandidatesIncludeLocalZeroForm() {
        val candidates = ContactRepository.smsDestinationCandidates("+254712345678")
        assertEquals(listOf("+254712345678", "0712345678", "254712345678"), candidates)
    }

    @Test
    fun saveErrorCodesStayStableForUiMapping() {
        assertEquals("invalid_phone", ContactSaveException.INVALID_PHONE)
        assertEquals("max", ContactSaveException.MAX)
        assertEquals("duplicate", ContactSaveException.DUPLICATE)
    }
}
