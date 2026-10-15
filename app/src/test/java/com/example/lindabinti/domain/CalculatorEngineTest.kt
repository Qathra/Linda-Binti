package com.example.lindabinti.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorEngineTest {

    @Test
    fun additionWorks() {
        val e = CalculatorEngine()
        e.inputDigit('2')
        e.setOperation('+')
        e.inputDigit('3')
        assertEquals("5", e.equals())
    }

    @Test
    fun subtractionMultiplicationDivision() {
        val e = CalculatorEngine()
        e.inputDigit('9')
        e.setOperation('−')
        e.inputDigit('4')
        assertEquals("5", e.equals())

        e.clear()
        e.inputDigit('6')
        e.setOperation('×')
        e.inputDigit('7')
        assertEquals("42", e.equals())

        e.clear()
        e.inputDigit('8')
        e.setOperation('÷')
        e.inputDigit('2')
        assertEquals("4", e.equals())
    }

    @Test
    fun decimalsAndChaining() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.inputDecimal()
        e.inputDigit('5')
        e.setOperation('+')
        e.inputDigit('2')
        e.inputDecimal()
        e.inputDigit('5')
        assertEquals("4", e.equals())

        e.clear()
        e.inputDigit('2')
        e.setOperation('+')
        e.inputDigit('3')
        e.setOperation('×')
        e.inputDigit('4')
        assertEquals("20", e.equals())
    }

    @Test
    fun clearAndDelete() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.inputDigit('2')
        e.inputDigit('3')
        e.deleteLast()
        assertEquals("12", e.currentDisplay())
        e.deleteLast()
        e.deleteLast()
        assertEquals("0", e.currentDisplay())
        e.inputDigit('9')
        e.clear()
        assertEquals("0", e.currentDisplay())
        assertNull(e.digitsCandidate())
    }

    @Test
    fun digitsCandidateForSosOnlyWhenFreshlyTyped() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.inputDigit('1')
        e.inputDigit('0')
        e.inputDigit('2')
        assertEquals("1102", e.digitsCandidate())
    }

    @Test
    fun digitsCandidateNullDuringOperation() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.setOperation('+')
        e.inputDigit('2')
        assertNull(e.digitsCandidate())
    }

    @Test
    fun digitsCandidateNullAfterEqualsResult() {
        val e = CalculatorEngine()
        e.inputDigit('5')
        e.setOperation('+')
        e.inputDigit('5')
        e.equals()
        assertNull(e.digitsCandidate())

        // Result that happens to look like the SOS code must not unlock SOS.
        e.clear()
        e.inputDigit('1')
        e.inputDigit('1')
        e.inputDigit('0')
        e.inputDigit('0')
        e.setOperation('+')
        e.inputDigit('2')
        e.equals()
        assertEquals("1102", e.currentDisplay())
        assertNull(e.digitsCandidate())
    }

    @Test
    fun divideByZeroShowsError() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.setOperation('÷')
        e.inputDigit('0')
        assertEquals("Error", e.equals())
    }

    @Test
    fun errorClearsOnNextDigit() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.setOperation('÷')
        e.inputDigit('0')
        e.equals()
        e.inputDigit('7')
        assertEquals("7", e.currentDisplay())
        assertEquals("7", e.digitsCandidate())
    }

    @Test
    fun operatorCanBeReplacedBeforeOperand() {
        val e = CalculatorEngine()
        e.inputDigit('8')
        e.setOperation('+')
        e.setOperation('×')
        e.inputDigit('3')
        assertEquals("24", e.equals())
    }
}
