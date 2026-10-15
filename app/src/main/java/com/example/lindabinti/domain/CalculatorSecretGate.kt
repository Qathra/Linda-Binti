package com.example.lindabinti.domain

import com.example.lindabinti.data.repository.AccessPinResult

/**
 * Dual-code Equals gate. SOS is always checked first and never locked out.
 * Access Code opens the protected space without SMS.
 */
sealed class SecretAction {
    data object TriggerSos : SecretAction()
    data object OpenAccess : SecretAction()
    data class Locked(val remainingSeconds: Long) : SecretAction()
    data object CalculatorEquals : SecretAction()
}

object CalculatorSecretGate {

    fun resolve(
        candidate: String?,
        setupComplete: Boolean,
        verifySosCode: (String) -> Boolean,
        attemptAccessPin: (String) -> AccessPinResult
    ): SecretAction {
        if (candidate.isNullOrBlank() || !setupComplete) return SecretAction.CalculatorEquals
        val sosMatch = runCatching { verifySosCode(candidate) }.getOrDefault(false)
        if (sosMatch) return SecretAction.TriggerSos
        if (candidate.length !in 4..6) return SecretAction.CalculatorEquals
        return when (val result = runCatching { attemptAccessPin(candidate) }.getOrDefault(AccessPinResult.Wrong)) {
            AccessPinResult.Success -> SecretAction.OpenAccess
            is AccessPinResult.Locked -> SecretAction.Locked(result.remainingSeconds)
            AccessPinResult.Wrong -> SecretAction.CalculatorEquals
        }
    }
}
