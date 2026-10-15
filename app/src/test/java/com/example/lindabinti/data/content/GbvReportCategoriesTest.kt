package com.example.lindabinti.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GbvReportCategoriesTest {

    @Test
    fun includesGbvFgmAndRelatedProtectionIds() {
        assertTrue(GbvReportCategories.ids.contains("physical"))
        assertTrue(GbvReportCategories.ids.contains("sexual"))
        assertTrue(GbvReportCategories.ids.contains("emotional"))
        assertTrue(GbvReportCategories.ids.contains("domestic"))
        assertTrue(GbvReportCategories.ids.contains("harassment"))
        assertTrue(GbvReportCategories.ids.contains("threats"))
        assertTrue(GbvReportCategories.ids.contains("fgm"))
        assertTrue(GbvReportCategories.ids.contains("fgm_incident"))
        assertTrue(GbvReportCategories.ids.contains("fgm_pressure"))
        assertTrue(GbvReportCategories.ids.contains("sos"))
        assertTrue(GbvReportCategories.ids.contains("forced_marriage"))
        assertTrue(GbvReportCategories.ids.contains("other"))
        assertEquals(12, GbvReportCategories.ids.size)
    }

    @Test
    fun groupsMatchFypScope() {
        val byGroup = GbvReportCategories.grouped.toMap()
        assertEquals(
            listOf("physical", "sexual", "emotional", "domestic", "harassment", "threats"),
            byGroup.getValue(GbvReportGroup.GBV).map { it.id }
        )
        assertEquals(
            listOf("fgm", "fgm_incident", "fgm_pressure"),
            byGroup.getValue(GbvReportGroup.FGM).map { it.id }
        )
        assertEquals(
            listOf("forced_marriage"),
            byGroup.getValue(GbvReportGroup.RELATED_PROTECTION).map { it.id }
        )
        assertEquals(
            listOf("other"),
            byGroup.getValue(GbvReportGroup.OTHER).map { it.id }
        )
    }

    @Test
    fun rejectsGeneralCrimeIds() {
        assertFalse(GbvReportCategories.isGbvFgmScope("theft"))
        assertFalse(GbvReportCategories.isGbvFgmScope("traffic"))
        assertFalse(GbvReportCategories.isGbvFgmScope("gbv"))
        assertTrue(GbvReportCategories.isGbvFgmScope("other"))
        assertTrue(GbvReportCategories.isGbvFgmScope("fgm_pressure"))
        assertEquals("other", GbvReportCategories.normalize("theft"))
        assertEquals("fgm_pressure", GbvReportCategories.normalize("FGM_PRESSURE"))
    }
}
