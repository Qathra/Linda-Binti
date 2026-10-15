package com.example.lindabinti.data.prefs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hashAndVerify_pbkdf2() {
        val hash = PinHasher.hash("1102")
        assertTrue(PinHasher.isPbkdf2Format(hash))
        assertTrue(PinHasher.verify("1102", hash))
        assertFalse(PinHasher.verify("1103", hash))
        assertFalse(PinHasher.verify("1102", null))
    }

    @Test
    fun verify_acceptsLegacySha256Format() {
        // salt:sha256digest — salt = 16 zero bytes encoded
        val salt = ByteArray(16) { 0 }
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest("1111".toByteArray(Charsets.UTF_8))
        val saltB64 = java.util.Base64.getEncoder().encodeToString(salt)
        val hashB64 = java.util.Base64.getEncoder().encodeToString(digest)
        val legacy = "$saltB64:$hashB64"
        assertFalse(PinHasher.isPbkdf2Format(legacy))
        assertTrue(PinHasher.verify("1111", legacy))
        assertFalse(PinHasher.verify("2222", legacy))
    }

    @Test
    fun verify_doesNotThrowOnGarbage() {
        assertFalse(PinHasher.verify("1102", "not-a-hash"))
        assertFalse(PinHasher.verify("1102", "pbkdf2:bad:xx:yy"))
        assertFalse(PinHasher.verify("1102", "pbkdf2:120000:%%%%:%%%%"))
    }

    @Test
    fun differentPinsProduceDifferentHashes() {
        val a = PinHasher.hash("1102")
        val b = PinHasher.hash("1111")
        assertTrue(a != b)
    }
}
