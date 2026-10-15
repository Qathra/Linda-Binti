package com.example.lindabinti.geo

data class ReportArea(
    val code: String,
    val label: String,
    val type: String = TYPE_SUB_COUNTY
) {
    companion object {
        const val TYPE_SUB_COUNTY = "SUB_COUNTY"
    }
}

/**
 * Coarse Kajiado sub-county mapping only.
 * Exact coordinates are never stored or exposed from this path.
 */
object KajiadoGeoAreas {
    private data class AreaCenter(
        val area: ReportArea,
        val latitude: Double,
        val longitude: Double
    )

    private val countyLatRange = -3.40..-1.00
    private val countyLngRange = 36.00..38.10

    val approvedAreas: List<ReportArea> = listOf(
        ReportArea("kajiado_north", "Kajiado North"),
        ReportArea("kajiado_east", "Kajiado East"),
        ReportArea("kajiado_central", "Kajiado Central"),
        ReportArea("kajiado_west", "Kajiado West"),
        ReportArea("kajiado_south", "Kajiado South")
    )

    /** Demo / SOS fallback so the NGO map still flags when GPS is still warming. */
    val central: ReportArea = approvedAreas[2]

    private val centers = listOf(
        AreaCenter(approvedAreas[0], latitude = -1.391, longitude = 36.763),
        AreaCenter(approvedAreas[1], latitude = -1.641, longitude = 37.129),
        AreaCenter(approvedAreas[2], latitude = -1.852, longitude = 36.776),
        AreaCenter(approvedAreas[3], latitude = -1.902, longitude = 36.290),
        AreaCenter(approvedAreas[4], latitude = -2.903, longitude = 37.509)
    )

    fun fromCoordinates(latitude: Double, longitude: Double): ReportArea? {
        if (latitude !in countyLatRange || longitude !in countyLngRange) return null
        return centers.minByOrNull { squaredDistance(latitude, longitude, it.latitude, it.longitude) }?.area
    }

    fun byCode(code: String?): ReportArea? =
        approvedAreas.firstOrNull { it.code == code?.trim()?.lowercase() }

    private fun squaredDistance(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val dLat = aLat - bLat
        val dLng = aLng - bLng
        return dLat * dLat + dLng * dLng
    }
}
