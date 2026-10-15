package com.lindabinti.backend.services

data class ApprovedArea(
    val code: String,
    val label: String,
    val type: String = TYPE_SUB_COUNTY
) {
    companion object {
        const val TYPE_SUB_COUNTY = "SUB_COUNTY"
    }
}

object KajiadoGeoAreas {
    private val areas = listOf(
        ApprovedArea("kajiado_north", "Kajiado North"),
        ApprovedArea("kajiado_east", "Kajiado East"),
        ApprovedArea("kajiado_central", "Kajiado Central"),
        ApprovedArea("kajiado_west", "Kajiado West"),
        ApprovedArea("kajiado_south", "Kajiado South")
    )

    fun all(): List<ApprovedArea> = areas

    /** USSD menu digits after GBV/FGM: 1 Central … 5 South. */
    fun fromMenuDigit(digit: String): ApprovedArea? = when (digit.trim()) {
        "1" -> areas.first { it.code == "kajiado_central" }
        "2" -> areas.first { it.code == "kajiado_east" }
        "3" -> areas.first { it.code == "kajiado_north" }
        "4" -> areas.first { it.code == "kajiado_west" }
        "5" -> areas.first { it.code == "kajiado_south" }
        else -> null
    }

    fun parseFreeText(raw: String): ApprovedArea? {
        val t = raw.trim().lowercase().replace('_', ' ').replace('-', ' ')
        if (t.isBlank()) return null
        areas.firstOrNull { it.label.equals(raw.trim(), ignoreCase = true) }?.let { return it }
        areas.firstOrNull { it.code == t.replace(" ", "_") }?.let { return it }
        return when {
            "central" in t -> areas.first { it.code == "kajiado_central" }
            "east" in t -> areas.first { it.code == "kajiado_east" }
            "north" in t -> areas.first { it.code == "kajiado_north" }
            "west" in t -> areas.first { it.code == "kajiado_west" }
            "south" in t -> areas.first { it.code == "kajiado_south" }
            else -> null
        }
    }

    fun canonicalOrNull(code: String?, label: String?, type: String?): ApprovedArea? {
        val normalizedCode = code?.trim()?.lowercase().orEmpty()
        val normalizedLabel = label?.trim().orEmpty()
        if (normalizedCode.isBlank() && normalizedLabel.isBlank()) return null
        val area = areas.firstOrNull {
            it.code == normalizedCode || it.label.equals(normalizedLabel, ignoreCase = true)
        } ?: throw IllegalArgumentException("invalid_kajiado_area")
        require(type.isNullOrBlank() || type.equals(ApprovedArea.TYPE_SUB_COUNTY, ignoreCase = true)) {
            "invalid_kajiado_area_type"
        }
        return area
    }
}
