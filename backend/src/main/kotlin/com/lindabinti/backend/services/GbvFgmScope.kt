package com.lindabinti.backend.services

/**
 * Canonical GBV/FGM report scope accepted by the backend.
 * Keeps report ingestion aligned with the Android category set.
 */
object GbvFgmScope {
    private val gbvIds = setOf(
        "physical",
        "sexual",
        "emotional",
        "domestic",
        "harassment",
        "threats",
        "forced_marriage",
        "other",
        // Device SOS alert — critical NGO flag (not a selectable report category).
        "sos"
    )

    private val fgmIds = setOf(
        "fgm",
        "fgm_incident",
        "fgm_pressure"
    )

    val allowedCategoryIds: Set<String> = gbvIds + fgmIds

    fun normalize(raw: String): String = raw.trim().lowercase()

    fun requireValidCategory(raw: String): String {
        val normalized = normalize(raw)
        require(normalized in allowedCategoryIds) { "invalid_gbv_fgm_category" }
        return normalized
    }

    fun isFgm(categoryId: String): Boolean = normalize(categoryId) in fgmIds

    fun isSos(categoryId: String): Boolean = normalize(categoryId) == "sos"

    fun relevanceFor(categoryId: String): String = when (normalize(categoryId)) {
        "sos" -> "GBV"
        "forced_marriage" -> "BOTH"
        in fgmIds -> "FGM"
        else -> "GBV"
    }
}
