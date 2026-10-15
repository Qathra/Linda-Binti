package com.example.lindabinti.domain

/**
 * Real calculator for +, −, ×, ÷, decimals, clear, and backspace.
 * PIN/SOS candidates are only exposed for a freshly typed integer entry
 * with no pending operator — never for computed results.
 *
 * Dual-code contract (do not weaken):
 * - digitsCandidate() only when freshly typed bare integer
 * - CalculatorScreen checks SOS first, then Access Code, on Equals
 */
class CalculatorEngine {

    private var display: String = "0"
    private var accumulator: Double? = null
    private var pendingOp: Char? = null
    private var overwrite: Boolean = true
    /** True only while the user is typing a fresh operand/entry. */
    private var freshEntry: Boolean = false

    fun currentDisplay(): String = display

    fun inputDigit(d: Char) {
        require(d in '0'..'9')
        if (display == "Error") {
            clear()
        }
        if (overwrite) {
            display = d.toString()
            overwrite = false
            freshEntry = true
            return
        }
        if (display == "0") {
            display = d.toString()
            freshEntry = true
            return
        }
        if (display == "-0") {
            display = "-$d"
            freshEntry = true
            return
        }
        if (display.length < 12) {
            display += d
            freshEntry = true
        }
    }

    fun inputDecimal() {
        if (display == "Error") {
            clear()
        }
        if (overwrite) {
            display = "0."
            overwrite = false
            freshEntry = true
            return
        }
        if (!display.contains('.')) {
            display += "."
            freshEntry = true
        }
    }

    /** Clears everything (AC). */
    fun clear() {
        display = "0"
        accumulator = null
        pendingOp = null
        overwrite = true
        freshEntry = false
    }

    /** Deletes the last character of the current entry. */
    fun deleteLast() {
        if (display == "Error") {
            clear()
            return
        }
        if (overwrite || !freshEntry) {
            display = "0"
            overwrite = true
            freshEntry = false
            return
        }
        val next = display.dropLast(1)
        display = when {
            next.isEmpty() || next == "-" || next == "-0" -> "0"
            else -> next
        }
        if (display == "0") {
            overwrite = true
            freshEntry = false
        }
    }

    fun toggleSign() {
        if (display == "Error" || display == "0" || display == "0.") return
        display = if (display.startsWith("-")) display.drop(1) else "-$display"
    }

    fun percent() {
        if (display == "Error") return
        val v = display.toDoubleOrNull() ?: return
        val base = accumulator
        val result = if (base != null && pendingOp != null) {
            // Common phone behavior: x% of accumulator while an operator is pending.
            base * (v / 100.0)
        } else {
            v / 100.0
        }
        display = format(result)
        overwrite = true
        freshEntry = false
    }

    fun setOperation(op: Char) {
        if (display == "Error") {
            clear()
            return
        }
        val value = display.toDoubleOrNull() ?: return
        // Replace pending operator when user taps another op before a new operand.
        if (accumulator != null && pendingOp != null && overwrite) {
            pendingOp = op
            return
        }
        if (accumulator != null && pendingOp != null && !overwrite) {
            val result = apply(accumulator!!, value, pendingOp!!)
            display = format(result)
            accumulator = if (display == "Error") null else result
            if (display == "Error") {
                pendingOp = null
                overwrite = true
                freshEntry = false
                return
            }
        } else {
            accumulator = value
        }
        pendingOp = op
        overwrite = true
        freshEntry = false
    }

    /** Evaluate pending operation; returns display string. */
    fun equals(): String {
        if (display == "Error") return display
        val value = display.toDoubleOrNull() ?: return display
        val acc = accumulator
        val op = pendingOp
        if (acc != null && op != null) {
            val result = apply(acc, value, op)
            display = format(result)
            accumulator = null
            pendingOp = null
        }
        overwrite = true
        freshEntry = false
        return display
    }

    /**
     * Digits for Access Code / SOS only when the user typed a bare integer
     * and has not used an operator. Computed results never qualify.
     */
    fun digitsCandidate(): String? {
        if (!freshEntry) return null
        if (pendingOp != null) return null
        if (overwrite) return null
        if (display == "Error") return null
        if (display.contains('.') || display.startsWith("-")) return null
        val digits = display.filter { it.isDigit() }
        return digits.ifBlank { null }
    }

    private fun apply(a: Double, b: Double, op: Char): Double {
        return when (op) {
            '+' -> a + b
            '−', '-' -> a - b
            '×', '*' -> a * b
            '÷', '/' -> if (b == 0.0) Double.NaN else a / b
            else -> b
        }
    }

    private fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val asLong = value.toLong()
        return if (value == asLong.toDouble() && kotlin.math.abs(value) < 1e12) {
            asLong.toString()
        } else {
            val s = String.format(java.util.Locale.US, "%.8f", value)
                .trimEnd('0')
                .trimEnd('.')
            if (s.length > 12) {
                String.format(java.util.Locale.US, "%.6g", value)
            } else {
                s
            }
        }
    }
}
