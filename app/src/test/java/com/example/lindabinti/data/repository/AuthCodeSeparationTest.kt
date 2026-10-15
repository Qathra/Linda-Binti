package com.example.lindabinti.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCodeSeparationTest {

    @Test
    fun accessAndSosMustNeverMatch() {
        assertTrue(AuthRepository.codesAreIdentical("1102", "1102"))
        assertFalse(AuthRepository.codesAreIdentical("1111", "1102"))
        assertFalse(AuthRepository.codesAreIdentical("", "1102"))
    }

    @Test
    fun defaultSosMustNotBeUsedAsAccessExample() {
        // Product rule: 1102 is SOS; Access must be a different code (e.g. 1111).
        assertTrue(AuthRepository.codesAreIdentical("1102", "1102"))
        assertFalse(AuthRepository.codesAreIdentical("1111", "1102"))
    }

    @Test
    fun accessCodeFormat() {
        assertTrue(AuthRepository.isValidAccessCode("1111"))
        assertTrue(AuthRepository.isValidAccessCode("123456"))
        assertFalse(AuthRepository.isValidAccessCode("12"))
        assertFalse(AuthRepository.isValidAccessCode("abcdef"))
    }

    @Test
    fun sosCodeFormat() {
        assertTrue(AuthRepository.isValidSosCode("1102"))
        assertTrue(AuthRepository.isValidSosCode("12345678"))
        assertFalse(AuthRepository.isValidSosCode("123"))
        assertFalse(AuthRepository.isValidSosCode("12ab"))
    }
}
