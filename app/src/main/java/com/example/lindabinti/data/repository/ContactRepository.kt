package com.example.lindabinti.data.repository

import com.example.lindabinti.data.local.TrustedContact
import com.example.lindabinti.data.local.TrustedContactDao
import kotlinx.coroutines.flow.Flow

class ContactSaveException(val code: String) : IllegalArgumentException(code) {
    companion object {
        const val INVALID_PHONE = "invalid_phone"
        const val MAX = "max"
        const val DUPLICATE = "duplicate"
    }
}

class ContactRepository(private val dao: TrustedContactDao) {

    fun observeContacts(): Flow<List<TrustedContact>> = dao.observeAll()

    suspend fun getContacts(): List<TrustedContact> = dao.getAll()

    suspend fun count(): Int = dao.count()

    suspend fun addContact(name: String, phone: String, relationship: String = ""): Result<Long> {
        val normalized = normalizePhone(phone) ?: return Result.failure(
            ContactSaveException(ContactSaveException.INVALID_PHONE)
        )
        val current = dao.count()
        if (current >= MAX_CONTACTS) {
            return Result.failure(ContactSaveException(ContactSaveException.MAX))
        }
        val existing = dao.getAll()
        if (existing.any { it.phoneE164 == normalized }) {
            return Result.failure(ContactSaveException(ContactSaveException.DUPLICATE))
        }
        val id = dao.insert(
            TrustedContact(
                name = name.trim().ifBlank { normalized },
                phoneE164 = normalized,
                relationship = relationship.trim(),
                sortOrder = current
            )
        )
        return Result.success(id)
    }

    suspend fun deleteContact(id: Long) {
        dao.deleteById(id)
    }

    companion object {
        const val MAX_CONTACTS = 5
        const val MIN_CONTACTS = 1

        fun normalizePhone(raw: String): String? {
            val cleaned = raw.filter { it.isDigit() || it == '+' }
            if (cleaned.isBlank()) return null
            val digits = cleaned.filter { it.isDigit() }
            return when {
                cleaned.startsWith("+") && digits.length in 9..15 -> cleaned
                digits.startsWith("0") && digits.length >= 9 -> {
                    // Kenya local → E.164 style +254…
                    "+254" + digits.drop(1)
                }
                digits.startsWith("254") && digits.length >= 12 -> "+$digits"
                digits.length in 9..15 -> "+$digits"
                else -> null
            }
        }

        /**
         * Addresses to try on the SMS radio.
         * This Samsung/Safaricom path sends SMS-over-IMS, which expects E.164
         * (`+254…`). Local `07…` is the retry form for CS GSM.
         */
        fun smsDestinationCandidates(raw: String): List<String> {
            val normalized = normalizePhone(raw) ?: return emptyList()
            val digits = normalized.filter { it.isDigit() }
            val local = if (digits.startsWith("254") && digits.length >= 12) {
                "0" + digits.drop(3)
            } else {
                null
            }
            val out = LinkedHashSet<String>()
            out += normalized
            local?.let { out += it }
            if (digits.isNotEmpty()) out += digits
            return out.toList()
        }
    }
}
