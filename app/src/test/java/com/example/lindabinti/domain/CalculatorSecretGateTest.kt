package com.example.lindabinti.domain

import com.example.lindabinti.data.repository.AccessPinResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorSecretGateTest {

    @Test
    fun defaultSos1102TriggersSosBeforeAccessPin() {
        val action = CalculatorSecretGate.resolve(
            candidate = "1102",
            setupComplete = true,
            verifySosCode = { it == "1102" },
            attemptAccessPin = { AccessPinResult.Success }
        )
        assertEquals(SecretAction.TriggerSos, action)
    }

    @Test
    fun accessPinOpensWithoutSosWhenCodesDiffer() {
        val action = CalculatorSecretGate.resolve(
            candidate = "1111",
            setupComplete = true,
            verifySosCode = { it == "1102" },
            attemptAccessPin = { if (it == "1111") AccessPinResult.Success else AccessPinResult.Wrong }
        )
        assertEquals(SecretAction.OpenAccess, action)
    }

    @Test
    fun unknownDigitsStayACalculator() {
        val action = CalculatorSecretGate.resolve(
            candidate = "1102",
            setupComplete = true,
            verifySosCode = { false },
            attemptAccessPin = { AccessPinResult.Wrong }
        )
        assertEquals(SecretAction.CalculatorEquals, action)
    }

    @Test
    fun sosDoesNothingBeforeSetup() {
        val action = CalculatorSecretGate.resolve(
            candidate = "1102",
            setupComplete = false,
            verifySosCode = { true },
            attemptAccessPin = { AccessPinResult.Success }
        )
        assertEquals(SecretAction.CalculatorEquals, action)
    }

    @Test
    fun sosVerifyExceptionDoesNotCrash() {
        val action = CalculatorSecretGate.resolve(
            candidate = "1102",
            setupComplete = true,
            verifySosCode = { error("pbkdf2") },
            attemptAccessPin = { AccessPinResult.Wrong }
        )
        assertEquals(SecretAction.CalculatorEquals, action)
    }

    @Test
    fun typed1102IsADigitsCandidate() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.inputDigit('1')
        e.inputDigit('0')
        e.inputDigit('2')
        assertEquals("1102", e.digitsCandidate())
        val action = CalculatorSecretGate.resolve(
            candidate = e.digitsCandidate(),
            setupComplete = true,
            verifySosCode = { it == "1102" },
            attemptAccessPin = { AccessPinResult.Wrong }
        )
        assertTrue(action is SecretAction.TriggerSos)
    }
}
