package com.example.lindabinti.data.content

import com.example.lindabinti.data.remote.SupportServiceDto
import com.example.lindabinti.data.remote.SupportServicesCatalogDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportServicesCatalogTest {

    @Test
    fun bundledPhonesAreOnlyVerifiedNationalShortCodes() {
        val services = SupportServicesCatalog.services("en")
        val phones = services.flatMap { it.callNumbers() }.toSet()
        assertEquals(SupportServicesCatalog.REQUIRED_PHONES, phones)
        services.forEach { service ->
            if (service.phone != null) {
                assertTrue(service.id, service.phoneVerified)
            } else {
                assertFalse(service.id, service.phoneVerified)
                assertTrue(service.callNumbers().isEmpty())
            }
        }
    }

    @Test
    fun kajiadoFacilitiesHaveNoInventedSwitchboard() {
        val hospital = SupportServicesCatalog.services("en").first { it.id == "kajiado_referral_hospital" }
        val desk = SupportServicesCatalog.services("en").first { it.id == "kajiado_gender_desk" }
        assertNull(hospital.phone)
        assertNull(desk.phone)
        assertTrue(hospital.area == SupportServiceArea.KAJIADO)
        assertTrue(desk.area == SupportServiceArea.KAJIADO)
        assertTrue(hospital.helpsWith.contains("GBV") || hospital.helpsWith.contains("FGM"))
    }

    @Test
    fun filtersCoverRequestedGroups() {
        val all = SupportServicesCatalog.services("en")
        SupportServicesCatalog.directoryKinds.forEach { kind ->
            assertTrue(kind.name, all.any { it.matches(kind) })
        }
        assertTrue(all.any { it.matches(SupportServiceKind.FGM) && it.phone == "116" })
        assertTrue(all.any { it.matches(SupportServiceKind.COUNSELLING) && it.phone == "1195" })
    }

    @Test
    fun kiswahiliKeepsStableIds() {
        assertEquals(
            SupportServicesCatalog.services("en").map { it.id },
            SupportServicesCatalog.services("sw").map { it.id }
        )
        assertTrue(SupportServicesCatalog.services("sw").first { it.id == "gbv_helpline" }.name.contains("GBV"))
    }

    @Test
    fun mergerDropsUnverifiedRemotePhonesAndKeepsRequiredHelplines() {
        val remote = SupportServicesCatalogDto(
            version = 2,
            source = "test",
            services = listOf(
                SupportServiceDto(
                    id = "invented_ngo",
                    kind = "gbv",
                    name = com.example.lindabinti.data.remote.LocalizedTextDto("Fake NGO", "Fake NGO"),
                    helpsWith = com.example.lindabinti.data.remote.LocalizedTextDto("Help", "Msaada"),
                    phone = "0700000000",
                    phoneVerified = false,
                    location = com.example.lindabinti.data.remote.LocalizedTextDto("Somewhere", "Mahali"),
                    referralGuidance = com.example.lindabinti.data.remote.LocalizedTextDto("Ask locally", "Uliza")
                ),
                SupportServiceDto(
                    id = "police_emergency",
                    kind = "other",
                    phone = null,
                    phoneVerified = false
                )
            )
        )
        val merged = SupportServicesMerger.merge(SupportServicesCatalog.records, remote)
        val resolved = SupportServicesCatalog.services("en", merged)
        val invented = resolved.first { it.id == "invented_ngo" }
        assertNull(invented.phone)
        val phones = resolved.flatMap { it.callNumbers() }.toSet()
        assertTrue(phones.containsAll(SupportServicesCatalog.REQUIRED_PHONES))
    }

    @Test
    fun staysOnGbvAndFgm() {
        val blob = SupportServicesCatalog.services("en").joinToString { it.name + it.helpsWith }
        assertTrue(blob.contains("GBV"))
        assertTrue(blob.contains("FGM"))
        assertFalse(blob.contains("malaria", ignoreCase = true))
        assertFalse(blob.contains("nutrition", ignoreCase = true))
    }
}
