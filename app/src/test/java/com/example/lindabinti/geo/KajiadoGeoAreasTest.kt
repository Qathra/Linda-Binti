package com.example.lindabinti.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KajiadoGeoAreasTest {
    @Test
    fun mapsKnownCoordinatesToApprovedSubCounty() {
        assertEquals("kajiado_north", KajiadoGeoAreas.fromCoordinates(-1.39, 36.76)?.code)
        assertEquals("kajiado_east", KajiadoGeoAreas.fromCoordinates(-1.64, 37.13)?.code)
        assertEquals("kajiado_central", KajiadoGeoAreas.fromCoordinates(-1.85, 36.78)?.code)
        assertEquals("kajiado_west", KajiadoGeoAreas.fromCoordinates(-1.90, 36.29)?.code)
        assertEquals("kajiado_south", KajiadoGeoAreas.fromCoordinates(-2.90, 37.51)?.code)
    }

    @Test
    fun outsideCountyBoundsReturnsNull() {
        assertNull(KajiadoGeoAreas.fromCoordinates(-0.95, 36.82))
        assertNull(KajiadoGeoAreas.fromCoordinates(-4.2, 37.2))
    }
}
