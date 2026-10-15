package com.lindabinti.backend.services

/**
 * Marks Kajiado sub-counties as HIGH / ELEVATED / LOWER from **case volume**
 * (how many reports+SOS landed in that area), not from a single case's AI score alone.
 */
object AreaHotspotAssigner {

    const val HIGH = "HIGH"
    const val ELEVATED = "ELEVATED"
    const val LOWER = "LOWER"

    fun assign(areas: List<GeoAreaSummary>): List<GeoAreaSummary> {
        val active = areas.filter { !it.suppressed && it.areaCode != "suppressed" }
        if (active.isEmpty()) return areas

        val max = active.maxOf { it.totalCount }.coerceAtLeast(0)
        // High-risk areas = those with the most cases (at least half of the busiest, min 2).
        val highFloor = if (max <= 0) Int.MAX_VALUE else maxOf(2, (max + 1) / 2)
        val elevatedFloor = 1

        return areas.map { area ->
            if (area.suppressed || area.areaCode == "suppressed") {
                area.copy(hotspotLevel = LOWER, hotspotReason = "Suppressed for privacy")
            } else {
                val total = area.totalCount
                val (level, reason) = when {
                    total >= highFloor -> HIGH to
                        "$total case(s) in this sub-county — among the highest volumes in Kajiado this period"
                    total >= elevatedFloor -> ELEVATED to
                        "$total case(s) reported in this sub-county"
                    else -> LOWER to "No reported cases in this sub-county this period"
                }
                area.copy(hotspotLevel = level, hotspotReason = reason)
            }
        }.sortedWith(
            compareByDescending<GeoAreaSummary> { hotspotRank(it.hotspotLevel) }
                .thenByDescending { it.totalCount }
        )
    }

    fun hotspotRank(level: String): Int = when (level.uppercase()) {
        HIGH -> 2
        ELEVATED -> 1
        else -> 0
    }
}
