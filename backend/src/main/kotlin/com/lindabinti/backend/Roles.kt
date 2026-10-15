package com.lindabinti.backend

enum class Role {
    USER,
    NGO_STAFF,
    NGO_ADMIN;

    companion object {
        fun parse(value: String): Role =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown role: $value")
    }
}