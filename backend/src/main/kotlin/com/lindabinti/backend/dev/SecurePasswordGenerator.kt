package com.lindabinti.backend.dev

import java.security.SecureRandom

object SecurePasswordGenerator {

    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val DIGITS = "23456789"
    private const val SPECIAL = "!@#$%&*-_=+"

    fun generate(length: Int = 16): String {
        require(length >= 12) { "Generated passwords must be at least 12 characters" }
        val random = SecureRandom()
        val required = charArrayOf(
            UPPER[random.nextInt(UPPER.length)],
            LOWER[random.nextInt(LOWER.length)],
            DIGITS[random.nextInt(DIGITS.length)],
            SPECIAL[random.nextInt(SPECIAL.length)]
        )
        val pool = UPPER + LOWER + DIGITS + SPECIAL
        val rest = CharArray(length - required.size) {
            pool[random.nextInt(pool.length)]
        }
        val all = CharArray(length)
        required.copyInto(all, 0)
        rest.copyInto(all, required.size)
        return all.shuffle(random).concatToString()
    }

    private fun CharArray.shuffle(random: SecureRandom): CharArray {
        for (i in size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = this[i]
            this[i] = this[j]
            this[j] = tmp
        }
        return this
    }
}
