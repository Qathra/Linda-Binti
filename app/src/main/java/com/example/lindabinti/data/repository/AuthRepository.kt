package com.example.lindabinti.data.repository

import com.example.lindabinti.data.prefs.PinHasher
import com.example.lindabinti.data.prefs.SecurePrefs
import com.example.lindabinti.data.content.SupportedLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AccessPinResult {
    data object Success : AccessPinResult()
    data object Wrong : AccessPinResult()
    data class Locked(val remainingSeconds: Long) : AccessPinResult()
}

/**
 * Two separate secrets:
 * - Access Code → opens protected Home, never sends SMS.
 * - SOS code → triggers SOS SMS first, then opens Home.
 * They must never be identical.
 */
class AuthRepository(private val prefs: SecurePrefs) {

    private val _language = MutableStateFlow(SupportedLanguages.normalize(prefs.language))
    val language: StateFlow<String> = _language.asStateFlow()

    fun isSetupComplete(): Boolean = prefs.setupComplete

    fun getUserDisplayName(): String = prefs.userDisplayName

    fun getLanguage(): String = _language.value

    fun setLanguage(code: String) {
        val normalized = SupportedLanguages.normalize(code)
        prefs.language = normalized
        _language.value = normalized
    }

    fun isShareAnonymousStats(): Boolean = prefs.shareAnonymousStats

    fun setShareAnonymousStats(enabled: Boolean) {
        prefs.shareAnonymousStats = enabled
    }

    /**
     * Saves Access Code only if it does not match the stored SOS code.
     * @return false when the value conflicts with the SOS code or format is invalid.
     */
    fun saveAccessPin(pin: String): Boolean {
        if (!isValidAccessCode(pin)) return false
        if (verifySosCode(pin)) return false
        prefs.accessPinHash = PinHasher.hash(pin)
        prefs.accessPinFailCount = 0
        prefs.accessPinLockUntilMillis = 0L
        return true
    }

    /**
     * Saves SOS code only if it does not match the stored Access Code.
     * @return false when the value conflicts with the Access Code or format is invalid.
     */
    fun saveSosCode(code: String): Boolean {
        if (!isValidSosCode(code)) return false
        if (verifyAccessPin(code)) return false
        prefs.sosCodeHash = PinHasher.hash(code)
        return true
    }

    fun verifyAccessPin(pin: String): Boolean =
        PinHasher.verify(pin, prefs.accessPinHash)

    fun verifySosCode(code: String): Boolean {
        return runCatching {
            val stored = prefs.sosCodeHash
            if (stored.isNullOrBlank()) return@runCatching false
            val ok = PinHasher.verify(code, stored)
            if (ok && !PinHasher.isPbkdf2Format(stored)) {
                prefs.sosCodeHash = PinHasher.hash(code)
            }
            ok
        }.getOrDefault(false)
    }

    /**
     * Access Code with brute-force protection.
     * SOS verification is separate and is never locked by this.
     */
    fun attemptAccessPin(pin: String): AccessPinResult {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.accessPinLockUntilMillis
        if (now < lockUntil) {
            val remaining = ((lockUntil - now + 999) / 1000).coerceAtLeast(1)
            return AccessPinResult.Locked(remaining)
        }
        if (PinHasher.verify(pin, prefs.accessPinHash)) {
            prefs.accessPinFailCount = 0
            prefs.accessPinLockUntilMillis = 0L
            // Re-hash legacy SHA-256 stores onto PBKDF2 when the user succeeds.
            if (!PinHasher.isPbkdf2Format(prefs.accessPinHash)) {
                prefs.accessPinHash = PinHasher.hash(pin)
            }
            return AccessPinResult.Success
        }
        val fails = prefs.accessPinFailCount + 1
        prefs.accessPinFailCount = fails
        if (fails >= SecurePrefs.MAX_PIN_FAILURES) {
            prefs.accessPinFailCount = 0
            prefs.accessPinLockUntilMillis = now + SecurePrefs.PIN_LOCK_DURATION_MS
            return AccessPinResult.Locked(SecurePrefs.PIN_LOCK_DURATION_MS / 1000)
        }
        return AccessPinResult.Wrong
    }

    /**
     * Completes first-time setup. Rejects identical Access Code and SOS code.
     */
    fun completeSetup(
        userName: String,
        accessPin: String,
        sosCode: String,
        language: String
    ) {
        require(userName.isNotBlank()) { "Name is required" }
        require(accessPin != sosCode) {
            "Access Code and SOS code must never be identical"
        }
        require(isValidAccessCode(accessPin)) { "Access Code must be 4–6 digits" }
        require(isValidSosCode(sosCode)) { "SOS code must be at least 4 digits" }

        prefs.userDisplayName = userName.trim()
        prefs.accessPinHash = PinHasher.hash(accessPin)
        prefs.sosCodeHash = PinHasher.hash(sosCode)
        prefs.accessPinFailCount = 0
        prefs.accessPinLockUntilMillis = 0L
        val normalized = SupportedLanguages.normalize(language)
        prefs.language = normalized
        _language.value = normalized
        prefs.setupComplete = true
    }

    /** Wipes PINs/setup flags. Caller must also clear Room tables. */
    fun wipeSecurePrefs() {
        prefs.clearAll()
        _language.value = SupportedLanguages.EN
    }

    companion object {
        fun codesAreIdentical(accessPin: String, sosCode: String): Boolean =
            accessPin.isNotEmpty() && accessPin == sosCode

        fun isValidAccessCode(pin: String): Boolean =
            pin.length in 4..6 && pin.all { it.isDigit() }

        fun isValidSosCode(code: String): Boolean =
            code.length in 4..8 && code.all { it.isDigit() }
    }
}
