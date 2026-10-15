package com.lindabinti.backend.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaHotspotAssignerTest {

    private fun area(code: String, label: String, total: Int) = GeoAreaSummary(
        areaCode = code,
        areaLabel = label,
        areaType = ApprovedArea.TYPE_SUB_COUNTY,
        gbvCount = total,
        fgmCount = 0,
        totalCount = total,
        sosCount = 0
    )

    @Test
    fun busiestSubCountiesMarkedHighByCaseVolume() {
        val assigned = AreaHotspotAssigner.assign(
            listOf(
                area("kajiado_east", "Kajiado East", 8),
                area("kajiado_north", "Kajiado North", 5),
                area("kajiado_west", "Kajiado West", 1),
                area("kajiado_south", "Kajiado South", 0)
            )
        )
        val byCode = assigned.associateBy { it.areaCode }
        assertEquals(AreaHotspotAssigner.HIGH, byCode["kajiado_east"]?.hotspotLevel)
        assertEquals(AreaHotspotAssigner.HIGH, byCode["kajiado_north"]?.hotspotLevel)
        assertEquals(AreaHotspotAssigner.ELEVATED, byCode["kajiado_west"]?.hotspotLevel)
        assertEquals(AreaHotspotAssigner.LOWER, byCode["kajiado_south"]?.hotspotLevel)
        assertTrue(byCode["kajiado_east"]?.hotspotReason?.contains("8") == true)
    }

    @Test
    fun singleCaseDoesNotCreateHighHotspotUntilVolumeBuilds() {
        val assigned = AreaHotspotAssigner.assign(
            listOf(area("kajiado_central", "Kajiado Central", 1))
        )
        assertEquals(AreaHotspotAssigner.ELEVATED, assigned.first().hotspotLevel)
    }

    @Test
    fun twoCasesInOneAreaAreHighWhenThatIsThePeak() {
        val assigned = AreaHotspotAssigner.assign(
            listOf(
                area("kajiado_east", "Kajiado East", 2),
                area("kajiado_north", "Kajiado North", 0)
            )
        )
        assertEquals(AreaHotspotAssigner.HIGH, assigned.first { it.areaCode == "kajiado_east" }.hotspotLevel)
    }
}
